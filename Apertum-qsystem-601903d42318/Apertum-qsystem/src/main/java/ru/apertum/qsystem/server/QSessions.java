/*
 * Copyright (C) 2014 Evgeniy Egorov
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
package ru.apertum.qsystem.server;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import ru.apertum.qsystem.common.QConfig;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.server.model.QUserList;

/**
 * Тут сессии пользователей. Истекают черех N минут = константа в сессии.
 *
 * @author Evgeniy Egorov
 */
public class QSessions {

    private QSessions() {
    }

    /**
     * Синглтон.
     */
    public static QSessions getInstance() {
        return SessionsHolder.INSTANCE;
    }

    private static class SessionsHolder {

        private static final QSessions INSTANCE = new QSessions();
    }

    private final LinkedList<QSession> sessions = new LinkedList<>();

    public List<QSession> getSessions() {
        return sessions;
    }

    private int anInt = 0;

    /**
     * Обновить состояние сессии.
     *
     * @param userId   для пользователя.
     * @param ipAdress с адреса.
     * @param ip       с адреса.
     */
    public synchronized void update(Long userId, String ipAdress, byte[] ip) {
        if (++anInt > 1000) {
            final LinkedList<QSession> forDel = new LinkedList();
            sessions.stream().filter(session -> !session.isValid()).forEach(forDel::add);
            sessions.removeAll(forDel);
            anInt = 0;
        }
        for (QSession session : sessions) {
            if ((ipAdress.equals(session.getIpAdress()) || Arrays.equals(ip, session.getIp()))
                && (userId != null && userId.equals(session.getUser().getId()))) {
                session.update();
                return;
            }
        }
    }

    /**
     * Проверить сессию.
     *
     * @param userId   для пользователя.
     * @param ipAdress с адреса.
     * @param ip       с адреса.
     * @return есть или нет.
     */
    public synchronized boolean check(Long userId, String ipAdress, byte[] ip) {
        for (QSession session : sessions) {
            if ((session.isValid())
                && ((!QConfig.cfg().isTerminal() && (ipAdress.equals(session.getIpAdress()) || Arrays.equals(ip, session.getIp())))
                || (userId != null && userId.equals(session.getUser().getId())))) {

                if ((QConfig.cfg().isTerminal() || (ipAdress.equals(session.getIpAdress()) || Arrays.equals(ip, session.getIp())))
                    && (userId != null && userId.equals(session.getUser().getId()))) {
                    continue;
                }

                QLog.l().logger().warn("Bad QSession for userID=" + userId + " adress=" + ipAdress + " ip=" + Arrays.toString(ip));
                return false;
            }
        }
        for (QSession session : sessions) {
            if ((ipAdress.equals(session.getIpAdress()) || Arrays.equals(ip, session.getIp()))
                && (userId != null && userId.equals(session.getUser().getId()))) {
                session.update();
                return true;
            }
        }
        return sessions.add(new QSession(QUserList.getInstance().getById(userId), ipAdress, ip));
    }

    /**
     * Удалить из сессий.
     *
     * @param userId по пользователю.
     */
    public synchronized void remove(Long userId) {
        for (QSession session : sessions) {
            if (userId.equals(session.getUser().getId())) {
                sessions.remove(session);
                return;
            }
        }
    }

}
