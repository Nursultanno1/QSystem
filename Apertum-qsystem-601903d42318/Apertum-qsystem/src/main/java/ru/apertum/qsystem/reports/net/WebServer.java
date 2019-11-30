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
package ru.apertum.qsystem.reports.net;

import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.exceptions.ReportException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Отчетный сервер, выступающий в роли вэбсервера, обрабатывающего запросы на выдачу отчетов.
 * Класс потоков, обрабатывающих запросы HTTP, для выдачи отчетов
 *
 * @author Evgeniy Egorov
 */
public class WebServer {

    public static WebServer getInstance() {
        return WebServerHolder.INSTANCE;
    }

    private static class WebServerHolder {

        private static final WebServer INSTANCE = new WebServer();
    }

    private WebServer() {
    }

    /**
     * состояние вэбсервера.
     */
    private volatile boolean isActive = false;
    /**
     * Поток вебсервера.
     */
    private Thread webTread = null;

    /**
     * запуск вэбсервера.
     *
     * @param port На каком порту
     */
    public synchronized void startWebServer(int port) {
        if (!isActive) {
            isActive = true;
        } else {
            return;
        }

        // привинтить сокет на локалхост, порт port
        QLog.l().logger().info("Отчетный сервер захватывает порт \"" + port + "\".");

        // поток вэбсервера, весит параллельно и обслуживает запросы
        webTread = new Thread() {

            @Override
            public void run() {
                try (final ServerSocket reportSocket = new ServerSocket(port, 0)) {
                    reportSocket.setSoTimeout(30000);
                    QLog.l().logRep().info("Report server for QSystem started.");

                    while (isActive && !webTread.isInterrupted()) {
                        // ждём нового подключения, после чего запускаем обработку клиента
                        // в новый вычислительный поток и увеличиваем счётчик на единичку
                        go(reportSocket);
                    }
                } catch (java.net.SocketTimeoutException e) {
                    // press hard
                } catch (Exception e) {
                    throw new ReportException("Ошибка при создании серверного сокета для вэбсервера: " + e);
                }
                QLog.l().logRep().info("Отчетный вэбсервер системы 'Очередь' остановлен.");

            }
        };
        // и запускаем новый вычислительный поток (см. ф-ю run())
        webTread.setDaemon(true);
        webTread.setPriority(Thread.NORM_PRIORITY);
        webTread.start();

    }

    private final void go(ServerSocket reportSocket) {
        try {
            final Socket socket = reportSocket.accept();
            final RunnableSocket rs = new RunnableSocket();
            rs.setSocket(socket);
            final Thread thread = new Thread(rs);
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.start();
        } catch (java.net.SocketTimeoutException ex) {
            // Accept timed out
        } catch (IOException ex) {
            throw new ReportException("Ошибка при работе сокета для вэбсервера: " + ex);
        }
    }

    /**
     * Останов вэбсервера.
     */
    public synchronized void stopWebServer() {
        if (isActive) {
            isActive = false;
        } else {
            return;
        }
        if (webTread != null) {
            webTread.interrupt();
        }
        try {
            do {
                wait(600);
            } while (false);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
