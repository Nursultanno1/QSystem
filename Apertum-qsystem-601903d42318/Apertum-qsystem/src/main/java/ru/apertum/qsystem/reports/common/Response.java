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
package ru.apertum.qsystem.reports.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс данных для отправки пользоваьелю в браузер.
 * Нужет для того что-бы вместе с данными можно было добавить некоторые атрибуты.
 *
 * @author Evgeniy Egorov
 */
public class Response {

    public Response() {
    }

    /**
     * type of content will be "text/html".
     *
     * @param data байтики для отправки.
     */
    public Response(byte[] data) {
        this.data = data;
    }

    public Response(byte[] data, HashMap<String, String> cookies) {
        this.data = data;
        this.cookies = cookies;
    }

    public Response(byte[] data, String contentType) {
        this.data = data;
        this.contentType = contentType;
    }

    /**
     * Отсылаемые данные. Это может быть текст html или pdf или что-то другое
     */
    private byte[] data;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * тип данных для последующей встаки в http-заголовок.
     */
    private String contentType = "text/html";

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String type) {
        this.contentType = type;
    }

    private HashMap<String, String> cookies = new HashMap<>();

    public HashMap<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(HashMap<String, String> cookies) {
        this.cookies = cookies;
    }

    /**
     * Куки в строку.
     *
     * @return строка с куками.
     */
    public String getCookiesStr() {
        final StringBuilder builder = new StringBuilder();
        String delim = "";
        for (Map.Entry cookie : cookies.entrySet()) {
            builder.append(delim).append(cookie.getKey()).append("=").append(cookie.getValue());
            delim = ";";
        }
        return builder.toString();
    }
}
