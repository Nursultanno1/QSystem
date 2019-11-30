/*
 * Copyright (C) 2016 Evgeniy Egorov (c) Apertum Projects
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ru.apertum.qsystem.client.model;

import javax.swing.table.AbstractTableModel;
import ru.apertum.qsystem.client.forms.FAdmin;
import ru.apertum.qsystem.common.QConfig;
import ru.apertum.qsystem.server.ServerProps;
import ru.apertum.qsystem.server.model.QProperty;

/**
 * Модель таблицы серверных настроек.
 *
 * @author Evgeniy Egorov
 */
public class PropsTableModel extends AbstractTableModel {

    private final transient ServerProps.Section section;

    public PropsTableModel(ServerProps.Section item) {
        section = item;
    }

    @Override
    public int getRowCount() {
        int i = 0;
        i = section.getProperties().keySet().stream()
            .filter(key -> (QConfig.cfg().showHidenProps() || !section.getProperties().get(key).getHide()))
            .map(item -> 1).reduce(i, Integer::sum);
        return i;
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return FAdmin.txt("key");
            case 1:
                return FAdmin.txt("value");
            case 2:
                return FAdmin.txt("comment");
            default:
                throw new AssertionError();
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 1 || columnIndex == 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final String key = getKey(rowIndex);
        switch (columnIndex) {
            case 0:
                return key;
            case 1:
                return section.getProperty(key).getValue();
            case 2:
                return section.getProperty(key).getComment();
            default:
                throw new AssertionError();
        }
    }

    /**
     * Заполнить ячейку в таблице.
     */
    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        final String key = getKey(rowIndex);
        switch (columnIndex) {
            case 1:
                section.getProperty(key).setValue((String) value);
                break;
            case 2:
                section.getProperty(key).setComment((String) value);
                break;
            default:
                throw new AssertionError();
        }
    }

    /**
     * Порядок элементов для показа не совпадает. Требуется исключить скрытые параметры.
     *
     * @param rowIndex параметр по этому интексу среди не скрытых
     * @return что-то
     */
    private String getKey(int rowIndex) {
        int i = 0;
        for (String key : section.getProperties().keySet()) {
            if (QConfig.cfg().showHidenProps() || !section.getProperties().get(key).getHide()) {
                if (i == rowIndex) {
                    return key;
                }
                i++;
            }
        }
        return null;
    }

    public QProperty getPropertyByKey(String key) {
        return section.getProperty(key);
    }

    /**
     * Удлалим настройку.
     *
     * @param p её удалим.
     */
    public void removeByKey(QProperty p) {
        if (p.getId() == null) {
            section.removeProperty(p);
        } else {
            ServerProps.getInstance().removeProperty(p);
        }

    }
}
