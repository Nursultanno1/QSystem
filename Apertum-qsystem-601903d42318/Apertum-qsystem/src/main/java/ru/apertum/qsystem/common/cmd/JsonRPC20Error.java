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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Ответ с ошибкой.
 *
 * @author Evgeniy Egorov
 */
public class JsonRPC20Error extends AJsonRPC20 {

    public enum ErrorMsg {
        UNKNOWN_ERROR(-1, "Unknown error."),
        RESPONCE_NOT_SAVE(2, "Не сохранили отзыв в базе."),
        POSTPONED_NOT_FOUND(3, "Отложенный пользователь не найден по его ID."),
        ADVANCED_NOT_FOUND(4, "Не верный номер предварительной записи."),
        REQUIRED_CUSTOMER_NOT_FOUND(5, "Customer not found but required.");

        final String message;
        final Integer code;

        ErrorMsg(Integer code, String message) {
            this.message = message;
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public Integer getCode() {
            return code;
        }
    }

    public static class ErrorRPC {

        public ErrorRPC() {
        }

        /**
         * Создать ответ с ошибкой.
         *
         * @param error тип ошибки.
         * @param data  сопутствующая информация.
         */
        public ErrorRPC(ErrorMsg error, Object data) {
            this.code = error == null ? ErrorMsg.UNKNOWN_ERROR.getCode() : error.getCode();
            this.message = error == null ? ErrorMsg.UNKNOWN_ERROR.getMessage() : error.getMessage();
            this.data = data;
        }

        public Integer getCode() {
            return code;
        }

        public Object getData() {
            return data;
        }

        public String getMessage() {
            return message;
        }

        @Expose
        @SerializedName("code")
        private Integer code;
        @Expose
        @SerializedName("message")
        private String message;
        @Expose
        @SerializedName("data")
        private Object data;
    }

    public JsonRPC20Error() {
    }

    public JsonRPC20Error(ErrorMsg err) {
        error = new ErrorRPC(err, null);
    }

    public JsonRPC20Error(ErrorMsg err, Object data) {
        error = new ErrorRPC(err, data);
    }

    public JsonRPC20Error(Integer code, Object data) {
        error = new ErrorRPC(null, data);
        error.code = code;
    }

    public JsonRPC20Error(ErrorMsg err, String message, Object data) {
        error = new ErrorRPC(err, data);
        error.message = message;
    }

    @Expose
    @SerializedName("error")
    private ErrorRPC error;

    public void setError(ErrorRPC error) {
        this.error = error;
    }

    public ErrorRPC getError() {
        return error;
    }
}
