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
package ru.apertum.qsystem.server.model.schedule;

import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import ru.apertum.qsystem.hibernate.Dao;
import ru.apertum.qsystem.server.model.ATListModel;

import javax.swing.ComboBoxModel;
import java.util.LinkedList;

/**
 * Можель для админки для списка расписания.
 *
 * @author Evgeniy Egorov
 */
public class QScheduleList extends ATListModel<QSchedule> implements ComboBoxModel {

    private QScheduleList() {
        super();
    }

    public static QScheduleList getInstance() {
        return QScheduleListHolder.INSTANCE;
    }

    private static class QScheduleListHolder {

        private static final QScheduleList INSTANCE = new QScheduleList();
    }

    @Override
    protected LinkedList<QSchedule> load() {
        return new LinkedList<>(Dao.get()
            .findByCriteria(DetachedCriteria.forClass(QSchedule.class)
                .setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY)));
    }

    private QSchedule selected;

    @Override
    public void setSelectedItem(Object anItem) {
        selected = (QSchedule) anItem;
    }

    @Override
    public Object getSelectedItem() {
        return selected;
    }
}
