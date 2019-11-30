/*
 * Copyright (C) 2010 {Apertum}Projects. web: www.apertum.ru email: info@apertum.ru
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
package ru.apertum.qsystem.server.http;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import ru.apertum.qsystem.common.GsonPool;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.cmd.JsonRPC20;
import ru.apertum.qsystem.server.controller.Executer;

import java.io.IOException;

/**
 * Недоделанный вэбсокет.
 *
 * @author Evgeniy Egorov
 */
@WebSocket
public class QWebSocketHandler {

    private Session session;

    /**
     * Когда подконектятся по вэбсокету.
     *
     * @param session Это сессия такого соединения. Внутри неё может прийти много сообщений.
     */
    @OnWebSocketConnect
    public void onConnect(Session session) {
        QLog.l().logger().trace("Connect: " + session.getRemoteAddress().getAddress());
        try {
            this.session = session;
            session.getRemote().sendString("Got your connect message");
        } catch (IOException ex) {
            QLog.l().logger().error("ERROR!!!", ex);
        }
    }

    /**
     * В вэбсокет пришло сообщение. Соединение не рвется, но сообщения приходят и ответ отправляется в это же соединение с постоянной сессией.
     *
     * @param message Это сообщение пришло.
     */
    @OnWebSocketMessage
    public void onMessage(String message) {
        QLog.l().logger().trace("Got message by WebSocket: \"{}\"", message);

        String answer;
        final Gson gson = GsonPool.getInstance().borrowGson();
        try {
            final JsonRPC20 rpc = gson.fromJson(message, JsonRPC20.class);
            // полученное задание передаем в пул
            final Object res = Executer.getInstance().doTask(rpc, session.getRemoteAddress().getHostName(), session.getRemoteAddress().getAddress().getAddress());
            answer = gson.toJson(res);
        } catch (Exception ex) {
            answer = "Произошла ошибка обработки задания. " + ex;
            QLog.l().logger().error("Произошла ошибка обработки задания. ", ex);
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        // выводим данные:
        QLog.l().logger().trace("Websocket response:\n" + (answer.length() > 200 ? (answer.substring(0, 200) + "...") : answer));
        try {
            session.getRemote().sendString(answer);
        } catch (Exception ex) {
            QLog.l().logger().error("ERROR!!!", ex);
        }
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        QLog.l().logger().trace("Close: statusCode={}, reason={}", statusCode, reason);
    }


}
