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

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import ru.apertum.qsystem.common.exceptions.ServerException;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import static ru.apertum.qsystem.common.QLog.log;

/**
 * Класс старта и останова сервера Jetty. При старте создается новый поток и в нем стартует Jetty.
 *
 * @author Evgeniy Egorov
 */
@SuppressWarnings("squid:S4797")
public class JettyRunner implements Runnable {

    /**
     * Страт Jetty.
     *
     * @param port порт на котором стартует сервер
     */
    public static synchronized void start(int port) {
        servetPort = port;
        if (jetthread != null && !jetthread.isInterrupted()) {
            try {
                if (jetty.isRunning()) {
                    jetty.stop();
                }
            } catch (Exception ex) {
                log().error("Ошибка остановки сервера Jetty.", ex);
            }
            jetthread.interrupt();
        }
        jetthread = new Thread(new JettyRunner());
        jetthread.setDaemon(true);
        jetthread.start();
    }

    /**
     * Остановить сервер Jetty.
     */
    public static void stop() {
        if (jetthread != null && !jetthread.isInterrupted()) {
            try {
                if (jetty.isRunning()) {
                    jetty.stop();
                }
            } catch (Exception ex) {
                throw new ServerException("Ошибка остановки сервера Jetty.", ex);
            }
            jetthread.interrupt();
        }
        log().info("Сервер Jetty успешно остановлен.");
    }

    private static Server jetty = null;
    private static int servetPort = 8081;
    private static Thread jetthread = null;

    @Override
    public void run() {
        log().info("Старт сервера Jetty на порту " + servetPort);
        jetty = new Server();

        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(8443);
        httpConfig.setOutputBufferSize(32768);
        httpConfig.setRequestHeaderSize(8192);
        httpConfig.setResponseHeaderSize(8192);
        httpConfig.setSendServerVersion(true);
        httpConfig.setSendDateHeader(false);
        ServerConnector httpConnector = new ServerConnector(jetty, new HttpConnectionFactory(httpConfig));
        httpConnector.setIdleTimeout(30000);
        httpConnector.setPort(servetPort);
        jetty.addConnector(httpConnector);

        final ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        resourceHandler.setResourceBase("www");

        final HandlerList handlers = new HandlerList();

        final WebSocketHandler webSocketHandler = new WebSocketHandler() {
             @Override
             public void configure(WebSocketServletFactory webSocketServletFactory) {
                 webSocketServletFactory.register(QWebSocketHandler.class);
             }
         };

        // Важный момент - поряд следования хандлеров
        // по этому порядку будет передоваться запрос, если он еще не обработан
        // т.е. с начала ищется файл, если не найден, то урл передается на исполнение команды,
        // в комаедах учтено что урл для вебсокета нужно пробросить дальше, его поймает хандлер вебсокетов
        //handlers.setHandlers(new Handler[]{resourceHandler, new CommandHandler(), qWebSocketHandler})
        handlers.setHandlers(new Handler[]{resourceHandler, new CommandHandler(), webSocketHandler});

        // Загрузка war из папки 
        String folder = "./www/war/";
        log().info("Загрузка war из папки " + folder);
        final File[] list = new File(folder).listFiles((File dir, String name) -> name.toLowerCase().endsWith(".war"));
        if (list != null && list.length != 0) {
            for (File file : list) {
                final String name = file.getName().split("\\.|-")[0].toLowerCase();
                log().debug("WAR " + name + ": " + file.getAbsolutePath());
                final WebAppContext webapp = new WebAppContext();
                webapp.setContextPath("/" + name);
                webapp.setWar(file.getAbsolutePath());
                handlers.addHandler(webapp);
            }
        }

        jetty.setHandler(handlers);

        try {
            jetty.start();
        } catch (Exception ex) {
            throw new ServerException("Ошибка запуска сервера Jetty. ", ex);
        }
        STARTED_FLAG.set(true);
        log().info("Join сервера Jetty на порту " + servetPort);
        try {
            jetty.join();
        } catch (InterruptedException ex) {
            log().warn("Jetty прекратил работу");
            Thread.currentThread().interrupt();
        }
        log().info("Сервер Jetty остановлен.");
    }

    public static final AtomicBoolean STARTED_FLAG = new AtomicBoolean(false);
}
