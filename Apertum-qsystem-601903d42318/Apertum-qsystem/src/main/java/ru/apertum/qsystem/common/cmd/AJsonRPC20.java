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
 *
 */
package ru.apertum.qsystem.common.cmd;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ru.apertum.qsystem.common.GsonPool;
import ru.apertum.qsystem.common.exceptions.QException;

import java.util.Date;

import static ru.apertum.qsystem.client.Locales.locMes;

/**
 * Class for input protocol. Using RPC2.0
 *
 * @author Evgeniy Egorov
 */
public abstract class AJsonRPC20 {

    public AJsonRPC20() {
    }

    public AJsonRPC20(String id) {
        this.id = id;
    }

    @Expose
    @SerializedName("jsonrpc")
    private String jsonrpc = "2.0";

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }


    @Expose
    @SerializedName("id")
    private String id = Long.toString(new Date().getTime());

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Expose
    @SerializedName("method")
    protected String method;

    public void setMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    /**
     * Преобразуем команду в строку.
     *
     * @param jsonRPC20 команда.
     * @return строка json.
     */
    public static String rpcToJson(AJsonRPC20 jsonRPC20) {
        final Gson gson = GsonPool.getInstance().borrowGson();
        try {
            return gson.toJson(jsonRPC20, jsonRPC20.getClass());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
    }

    /**
     * Демаршалинг строки с помощью Gson в обхект.
     *
     * @param text     Это валидный json в строке.
     * @param classOfT Это класс, в который превратился json.
     * @param <T>      Тип класса для демаршалинга
     * @return Конкретный объект демаршаленный из json.
     * @throws QException Есди получить объект из строки не удалось. Ошибка от gson.
     */
    public static <T> T demarshal(String text, Class<T> classOfT) throws QException {
        final T rpc;
        final Gson gson = GsonPool.getInstance().borrowGson();
        try {
            rpc = gson.fromJson(text, classOfT);
        } catch (JsonSyntaxException ex) {
            throw new QException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc;
    }

    @Override
    public String toString() {
        return rpcToJson(this);
    }
}
