/*
 *  Copyright (C) 2010 {Apertum}Projects. web: www.apertum.ru email: info@apertum.ru
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ru.apertum.qsystem.server.model.calendar;

import org.hibernate.Transaction;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.exceptions.ClientException;
import ru.apertum.qsystem.hibernate.Dao;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Модель для отображения сетки календаля.
 *
 * @author Evgeniy Egorov
 */
public class CalendarTableModel extends AbstractTableModel {

    private final List<FreeDay> days;
    private List<FreeDay> daysDel;

    /**
     * Модель для отображения сетки календаля.
     *
     * @param calc ID из БД.
     */
    public CalendarTableModel(QCalendar calc) {
        QLog.l().logger().debug("Создаем модель для календаря");
        this.calendar = calc;
        days = getFreeDays(calendar);
        daysDel = new ArrayList<>(days);
    }

    /**
     * В каком календаре сейчас работаем.
     */
    private final QCalendar calendar;

    /**
     * Выборка из БД требуемых данных.
     *
     * @param calc календарь
     * @return список выходных дней определенного календаря
     */
    @SuppressWarnings("squid:S1319")
    public static synchronized LinkedList<FreeDay> getFreeDays(final QCalendar calc) {
        final DetachedCriteria criteria = DetachedCriteria.forClass(FreeDay.class);
        criteria.add(Property.forName("calendarId").eq(calc.getId()));
        return new LinkedList<>(Dao.get().findByCriteria(criteria));
    }

    /**
     * Добавляем дату. Если Такая уже есть то инвертируем.
     *
     * @param date     день.
     * @param noInvert true - при обнаружении выходного оставлять его выходным
     * @return Добавлена как свободная или как рабочая/ true = свободная
     */
    public boolean addDay(Date date, boolean noInvert) {
        final FreeDay day = isFreeDate(date);
        if (day != null) {
            if (noInvert) {
                return true;
            } else {
                days.remove(day);
            }
            return false;
        } else {
            days.add(new FreeDay(date, calendar));
            return true;
        }
    }

    /**
     * Проверяем добавлена ли в выходные уже. Пробежим по всемвыходным и при совпадении даты вернем этот выходной день.
     *
     * @param day день для поиска по нему выходного.
     * @return выходной или нулл.
     */
    public FreeDay isFreeDate(Date day) {
        for (FreeDay freeDay : days) {
            if (freeDay.sameDay(day)) {
                return freeDay;
            }
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return 12;
    }

    @Override
    public int getColumnCount() {
        return 32;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return "X";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? super.getColumnClass(columnIndex) : FreeDay.class;
    }

    @Override
    public String getColumnName(int column) {
        return column == 0 ? "" : Integer.toString(column);
    }

    /**
     * Сбросить выделенные дни в календаре.
     *
     * @param year год.
     */
    public void dropCalendar(int year) {
        QLog.l().logger().debug("Сбросим календарь");
        final ArrayList<FreeDay> del = new ArrayList<>();
        final GregorianCalendar gc = new GregorianCalendar();
        for (FreeDay freeDay : days) {
            gc.setTime(freeDay.getDate());
            if (gc.get(GregorianCalendar.YEAR) == year) {
                del.add(freeDay);
            }
        }
        days.removeAll(del);
        fireTableDataChanged();
    }

    /**
     * Пометить все субботы выходными.
     *
     * @param year год.
     */
    public void checkSaturday(int year) {
        QLog.l().logger().debug("Пометив все субботы");
        final GregorianCalendar gc = new GregorianCalendar();
        gc.set(GregorianCalendar.YEAR, year);
        final int ye = year % 4 == 0 ? 366 : 365;
        for (int i = 1; i <= ye; i++) {
            gc.set(GregorianCalendar.DAY_OF_YEAR, i);
            if (gc.get(GregorianCalendar.DAY_OF_WEEK) == 7) {
                addDay(gc.getTime(), true);
            }
        }
        fireTableDataChanged();
    }

    /**
     * Пометить все воскресенья выходными.
     *
     * @param year год.
     */
    public void checkSunday(int year) {
        QLog.l().logger().debug("Пометим все воскресенья");
        final GregorianCalendar gc = new GregorianCalendar();
        gc.set(GregorianCalendar.YEAR, year);
        final int ye = year % 4 == 0 ? 366 : 365;
        for (int i = 1; i <= ye; i++) {
            gc.set(GregorianCalendar.DAY_OF_YEAR, i);
            if (gc.get(GregorianCalendar.DAY_OF_WEEK) == 1) {
                addDay(gc.getTime(), true);
            }
        }
        fireTableDataChanged();
    }

    /**
     * Сохранить календарь.
     */
    public void save() {
        QLog.l().logger().info("Сохраняем календарь \"" + calendar + "\" ID=" + calendar.getId());
        final Transaction transaction = Dao.get().getSession().beginTransaction();
        try {
            final LinkedList<FreeDay> dels = new LinkedList<>();
            for (FreeDay bad : daysDel) {
                boolean f = true;
                for (FreeDay good : days) {
                    if (good.equals(bad)) {
                        f = false;
                    }
                }
                if (f) {
                    dels.add(bad);
                }
            }
            Dao.get().saveOrUpdate(calendar);
            Dao.get().deleteAll(dels);
            Dao.get().saveOrUpdateAll(days);
        } catch (Exception ex) {
            transaction.rollback();
            throw new ClientException("Ошибка выполнения операции изменения данных в БД(JDBC).\nВозможно Вы добавили новый календарь, изменили его,"
                    + " пытаетесь сохранить содержимое календаря, но общую конфигурацию не сохранили.\nСохраните всю конфигурацию(Ctrl + S) "
                    + "и еще раз попытайтесь сохранить содержимое календаря.\n\n[" + ex.getLocalizedMessage() + "]\n(" + ex.toString() + ")");
        }
        transaction.commit();
        QLog.l().logger().debug("Сохранили новый календарь");
        //типо чтоб были актуальные внутренние данные
        daysDel = new ArrayList<>(days);
    }

    /**
     * Проверка на сохраненность календаря.
     */
    public boolean isSaved() {
        for (FreeDay day : days) {
            if (day.getId() == null) {
                return false;
            }
        }
        return daysDel.size() == days.size();
    }
}
