/*
 * Copyright (C) 2011 Evgeniy Egorov
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

import java.util.HashMap;
import javax.swing.table.AbstractTableModel;
import ru.apertum.qsystem.client.forms.FServicePriority;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.cmd.RpcGetSelfSituation.SelfService;
import ru.apertum.qsystem.common.cmd.RpcGetSelfSituation.SelfSituation;

/**
 * Таблица гибких приоритетов.
 *
 * @author Evgeniy Egorov
 */
public class FlexPriorityMableModel extends AbstractTableModel {

    private final transient SelfSituation plan;

    public FlexPriorityMableModel(SelfSituation plan) {
        this.plan = plan;
    }

    @Override
    public int getRowCount() {
        return plan.getSelfservices().size();
    }

    /**
     * Количество колонок в таблице.
     */
    @Override
    public int getColumnCount() {
        return 2;
    }

    private final HashMap<Integer, SelfService> services = new HashMap<>();

    public HashMap<Integer, SelfService> getServicesInRows() {
        return services;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        int n = 0;
        for (SelfService service : plan.getSelfservices()) {
            if (n++ == rowIndex) {
                switch (columnIndex) {
                    case 0:
                        services.put(rowIndex, service);
                        return service.getServiceName();
                    case 1:
                        return Uses.getCoeffWord().get(service.getPriority());
                    default:
                        throw new AssertionError();
                }
            }
        }
        throw new AssertionError();
    }

    /**
     * Заполнение ячейки.
     *
     * @param value заполняемое значение.
     */
    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        int res = 1;

        for (int i = 0; i < Uses.getCoeffWord().size(); i++) {
            if (((String) value).equals(Uses.getCoeffWord().get(i))) {
                res = i;
            }
        }
        services.get(rowIndex).setPriority(res);
        super.setValueAt(services.get(rowIndex), rowIndex, columnIndex);
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return FServicePriority.getLocaleMessage("service.service");
            case 1:
                return FServicePriority.getLocaleMessage("service.priority");
            default:
                throw new AssertionError();
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != 0 && services.get(rowIndex).isFlexy();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? String.class : SelfService.class;
    }
}
