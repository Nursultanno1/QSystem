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
package ru.apertum.qsystem.server.controller;

import ru.apertum.qsystem.common.CustomerState;
import ru.apertum.qsystem.common.model.ATalkingClock;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.server.model.QUser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Базовый класс для классов вывода. Сдесь реализован движок хранения и управления строками и прочий инфой для вывода инфы. При непосредственным выводом на
 * табло нужно вызвать этот метод markShowed(), чтоб промаркировать записи как начавшие висеть.
 *
 * @author Evgeniy Egorov
 */
public abstract class AIndicatorBoard implements IIndicatorBoard {

    /**
     * Количество строк на табло. Реализовать под конкретное табло.
     *
     * @return Количество строк на табло.
     */
    protected abstract Integer getLinesCount();

    /**
     * Задержка обновления главного табло в секундах.
     */
    private Integer pause = 0;

    public Integer getPause() {
        return pause;
    }

    public void setPause(Integer pause) {
        this.pause = pause;
    }

    //***********************************************************************
    //*************** Работа с хранением строк ******************************
    /**
     * Список отображаемых строк Название юзера, создавшего эту строку на табло(Это идентификатор строк, т.к. имя позьзователя уникально в системе) - строка
     */
    private final LinkedHashMap<String, Record> records = new LinkedHashMap<>();

    protected LinkedHashMap<String, Record> getRecords() {
        return records;
    }

    protected void clearRecords() {
        records.clear();
        oldRec = null;
        oldList = new LinkedList<>();
    }

    /**
     * Добавляет запись в хвост списка отображения Делает ее еще не отображенной. Мигание переехало в табло.
     *
     * @param record строка на табло.
     */
    protected void addItem(Record record) {
        records.remove(record.getUserName());
        record.isShowed = false;
        records.put(record.getUserName(), record);
    }

    /**
     * Убрать запись. Кастомер домой ушел.
     *
     * @param record запись.
     */
    protected void removeItem(Record record) {
        records.remove(record.userName);
    }

    protected LinkedList<Record> getShowRecords() {
        ArrayList<Record> arr = new ArrayList<>(records.values());
        // перевернуть массив, так как добавленные валятся в конец, а выводить их первыми
        for (int i = 0; i < arr.size() / 2; i++) {
            final Record record = arr.get(i);
            arr.set(i, arr.get(arr.size() - 1 - i));
            arr.set(arr.size() - 1 - i, record);
        }

        int pos = -1; // позиция последнего не отвесевшего.
        for (int i = 0; i < arr.size(); i++) {
            if (!arr.get(i).isShowed()) {
                pos = i;
            }
        }
        final int startPos = (getLinesCount() - 1 > pos) ? 0 : pos - getLinesCount() + 1; // позиция первой строки на табло.
        final LinkedList<Record> res = new LinkedList<>();
        for (int j = 0; j < arr.size(); j++) {
            if (j >= startPos && j < startPos + getLinesCount()) {
                res.add(arr.get(j));
            }
        }
        return res;
    }

    /**
     * Класс одной строки.
     */
    public class Record implements Comparable<Record> {

        private final String point;
        private String customerPrefix;
        private Integer customerNumber;

        public String getPoint() {
            return point;
        }

        public String getCustomerPrefix() {
            return customerPrefix;
        }

        public void setCustomerPrefix(String customerPrefix) {
            this.customerPrefix = customerPrefix;
        }

        public Integer getCustomerNumber() {
            return customerNumber;
        }

        public void setCustomerNumber(Integer customerNumber) {
            this.customerNumber = customerNumber;
        }

        /**
         * При RS это адрес устройства. При мониторе это норядковый номер вывода
         */
        public final Integer adressRS;
        public final String extData;

        public Integer getAdressRS() {
            return adressRS;
        }

        public String getExtData() {
            return extData;
        }

        @Override
        public String toString() {
            return customerNumber + "-" + point;
        }

        /**
         * Название юзера, создавшего эту строку на табло. Это идентификатор строк, т.к. имя позьзователя уникально в системе.
         */
        private final String userName;

        public String getUserName() {
            return userName;
        }

        /**
         * Отвесела на табло или нет.
         */
        private boolean isShowed = false;

        /**
         * Уже показалась сколько надо.
         */
        public boolean isShowed() {
            return isShowed;
        }

        /**
         * значения состояния "очередника".
         */
        private CustomerState state = CustomerState.STATE_INVITED;

        public CustomerState getState() {
            return state;
        }

        public void setState(CustomerState state) {
            this.state = state;
        }

        /**
         * При создании строка попадает в список отображения с признаком того что еще не отвесела. Таймер висения включеется когда строка попадает на табло.
         *
         * @param userName       пользователь.
         * @param point          номер кабинета куда вызвали кастомера.
         * @param customerPrefix префикс услуги.
         * @param customerNumber номер кастомера о ком запись.
         * @param extData        третья колонка
         * @param adressRS       адрес клиентского табло.
         * @param interval       обязательное время висения строки на табло в секундах
         */
        public Record(String userName, String point, String customerPrefix, Integer customerNumber, String extData, Integer adressRS, Integer interval) {
            this.extData = extData;
            this.adressRS = adressRS;
            this.customerPrefix = customerPrefix;
            this.customerNumber = customerNumber;
            this.userName = userName;
            this.point = point;
            final Record re = this;
            records.put(userName, re);
            showTimer = new ATalkingClock(interval * 1000, 1) {

                @Override
                public void run() {
                    isShowed = true;
                    show(null);
                }
            };
        }

        /**
         * Строка на табло.
         */
        public Record(CustomerState state, String point, String customerPrefix, Integer customerNumber, String extData, Integer adressRS) {
            this.extData = extData;
            this.customerPrefix = customerPrefix;
            this.customerNumber = customerNumber;
            this.point = point;
            this.state = state;
            this.adressRS = adressRS;
            this.userName = "noName";
            showTimer = null;
        }

        /**
         * Таймер время висения на табло.
         */
        private final ATalkingClock showTimer;

        /**
         * Запись попала на табло.
         */
        public void startVisible() {
            if (!showTimer.isActive()) {
                showTimer.start();
            }
        }

        @Override
        public int compareTo(Record o) {
            return (o != null && adressRS.equals(o.adressRS) && customerNumber.equals(o.customerNumber) && point.equals(o.point) && state == o.state) ? 0 : -1;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Record) {
                return this.compareTo((Record) obj) == 0;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return (adressRS.toString() + customerNumber.toString() + point + state.name()).hashCode();
        }
    }
    //**************************************************************************
    //************************** Методы взаимодействия *************************

    @Override
    public synchronized void inviteCustomer(QUser user, QCustomer customer) {
        Record rec = records.get(user.getName());
        if (rec == null) {
            rec = new Record(user.getName(), user.getPoint(), customer.getPrefix(), customer.getNumber(),
                user.getPointExt().replaceAll("(#client)", customer.getFullNumber()).replaceAll("(#point)", user.getPoint())
                    .replaceAll("(#user)", user.getTabloText()).replaceAll("(#service)", customer.getService().getTabloText())
                    .replaceAll("(#inputed)", !customer.getService().getInputedAsExt() || customer.getInputData() == null ? "" : customer.getInputData()),
                user.getAdressRS(), getPause());
        } else {
            // параллельный вызов надо учесть
            // т.е. сразу после вызова и начала работы с одним кастомером, оператор может вызвать еще одного
            // получается что уже вызов висит и ему нужно изменить номер вызванного и его статус, а кабинет тот же.
            if (!rec.customerPrefix.equalsIgnoreCase(customer.getPrefix()) || !rec.customerNumber.equals(customer.getNumber())) {
                rec.customerPrefix = customer.getPrefix();
                rec.customerNumber = customer.getNumber();
                rec.state = customer.getState();
            }
            addItem(rec);
        }
        show(rec);
    }

    /**
     * На табло оператора долженн перестать мигать номер вызываемого клиента.
     *
     * @param user пользователь, который начал работать с клиентом.
     */
    @Override
    @SuppressWarnings("empty-statement")
    public synchronized void workCustomer(QUser user) {
        Record rec = records.get(user.getName());
        //запись может быть не найдена после рестарта сервера, список номеров на табло не бакапится
        if (rec == null) {
            rec = new Record(user.getName(), user.getPoint(), user.getCustomer().getPrefix(), user.getCustomer().getNumber(),
                user.getPointExt().replaceAll("(#client)", user.getCustomer().getFullNumber()).replaceAll("(#point)", user.getPoint())
                    .replaceAll("(#user)", user.getTabloText()).replaceAll("(#service)", user.getCustomer().getService().getTabloText())
                    .replaceAll("(#inputed)", !user.getCustomer().getService().getInputedAsExt() || user.getCustomer().getInputData() == null
                        ? ""
                        : user.getCustomer().getInputData()),
                user.getAdressRS(), getPause());
        }
        rec.setState(CustomerState.STATE_WORK);
        show(rec);
    }

    /**
     * На табло по определенному адресу должно отчистиццо табло.
     *
     * @param user пользователь, который удалил клиента.
     */
    @Override
    public synchronized void killCustomer(QUser user) {
        final Record rec = records.get(user.getName());
        //запись может быть не найдена после рестарта сервера, список номеров на табло не бакапится
        if (rec != null) {
            rec.setState(CustomerState.STATE_DEAD);
            removeItem(rec);
            show(rec);
        }
    }

    /**
     * Выключить информационное табло.
     */
    @Override
    public synchronized void close() {
        showOnBoard(new LinkedList<>());
    }

    //**************************************************************************
    //************************** Другие методы *********************************
    // чтоб отсеч дублирование
    private Record oldRec = null;
    private LinkedList<Record> oldList = new LinkedList<>();

    private boolean compareList(LinkedList<Record> newList) {
        if (oldList.size() != newList.size()) {
            return false;
        }
        final int size = oldList.size();
        final Record[] ol = oldList.toArray(new Record[size]);
        final Record[] nl = newList.toArray(new Record[size]);
        for (int i = 0; i < size; i++) {
            if (ol[i].compareTo(nl[i]) != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Тут вся иллюминация.
     *
     * @param record строка на табло.
     */
    protected void show(Record record) {

        LinkedList<Record> newList = getShowRecords();

        if (!compareList(newList)) {
            oldList = new LinkedList<>();
            newList.stream().forEach(rec -> oldList.add(new Record(rec.state, rec.point, rec.customerPrefix, rec.customerNumber, rec.extData, rec.adressRS)));
            showOnBoard(newList);
        }
        if (record != null && record.compareTo(oldRec) != 0) {
            oldRec = new Record(record.state, record.point, record.customerPrefix, record.customerNumber, record.extData, record.adressRS);
            showToUser(record);
        }
    }

    /**
     * При непосредственным выводом на табло нужно вызвать этот метод, чтоб промаркировать записи как начавшие висеть.
     *
     * @param list список выводимых звписей.
     */
    protected void markShowed(Collection<Record> list) {
        // Записи попадают на табло
        if (list != null) {
            list.stream().filter(rec -> (!rec.isShowed())).forEach(Record::startVisible);
        }
    }

    /**
     * Высветить записи на общем табло.
     *
     * @param records Высвечиваемые записи.
     */
    protected abstract void showOnBoard(LinkedList<Record> records);

    /**
     * Высветить запись на табло оператора.
     *
     * @param record Высвечиваемая запись.
     */
    protected abstract void showToUser(Record record);
}
