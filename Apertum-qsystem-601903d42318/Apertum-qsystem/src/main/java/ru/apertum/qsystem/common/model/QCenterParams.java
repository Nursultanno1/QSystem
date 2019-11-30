/*
 * Copyright (C) 2012 {Apertum}Projects. web: www.apertum.ru email: info@apertum.ru
 * Copyright (C) 2012 Evgeniy Egorov
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
package ru.apertum.qsystem.common.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.LinkedList;

/**
 * Параметры центра табло с отображением вызовов.
 *
 * @author Evgeniy Egorov
 */
@SuppressWarnings("squid:S1319")
public class QCenterParams {

    @Expose
    @SerializedName("props")
    private LinkedList<QParam> params = new LinkedList<>();

    public LinkedList<QParam> getParams() {
        return params;
    }

    public void setParams(LinkedList<QParam> params) {
        this.params = params;
    }
}
