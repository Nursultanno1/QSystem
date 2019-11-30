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
package ru.apertum.qsystem.common;

import java.awt.Font;
import java.nio.charset.Charset;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.lookup.MainMapLookup;
import ru.apertum.qsystem.About;
import ru.apertum.qsystem.server.ServerProps;

/**
 * Собственно, логер лог4Ж Это синглтон. Тут в место getInstance() для короткого написания используется l()
 *
 * @author Evgeniy Egorov
 */
public class QLog {

    private Logger logger;

    /**
     * Пользуемся этой константой для работы с логом для отчетов.
     */
    private Logger logRep;
    private Logger logProp;
    private Logger logQUser;

    public Logger logger() {
        return logger;
    }

    public static Logger log() {
        return l().logger();
    }

    public Logger logRep() {
        return logRep;
    }

    public static Logger lgRep() {
        return l().logRep();
    }

    private Logger lgProp() {
        return logProp;
    }

    public static Logger logProp() {
        return l().lgProp();
    }

    public Logger logQUser() {
        return logQUser;
    }

    public static Logger lgUser() {
        return l().logQUser();
    }

    private QLog() {
        Charset charset = Charset.forName("UTF-8");
        if (!QConfig.cfg().isIDE() && SystemUtils.IS_OS_WINDOWS) { // Операционка и бинс
            charset = Charset.forName("cp866");
        }
        final String[] args = {loggerType.name(), charset.name()};
        MainMapLookup.setMainArguments(args);
        Configurator.initialize(loggerType.name(), QConfig.cfg().getLog4j2Cfg() != null ? QConfig.cfg().getLog4j2Cfg() : "log4j2.xml");
        logger = LogManager.getLogger(loggerType.name());
        logRep = LogManager.getLogger("reports");
        logProp = LogManager.getLogger("adminProperties");
        logQUser = LogManager.getLogger(QModule.quser);
        pause();
    }

    private void pause() {
        // ключ, отвечающий за паузу на старте.
        if (QConfig.cfg().getDelay() > 0) {
            try {
                Thread.sleep(QConfig.cfg().getDelay() * 1000L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static QLog l() {
        return LogerHolder.INSTANCE;
    }

    private static QModule loggerType = QModule.server; // 0-сервер,1-клиент,2-приемная,3-админка,4-киоск

    /**
     * Инициалицация. Параметры из консоли и логгер.
     *
     * @param args параметры консоли.
     * @param type 0-сервер,1-клиент,2-приемная,3-админка,4-киоск,5-сервер хардварных кнопок
     * @return Логгер.
     */
    public static QLog initial(String[] args, QModule type) {
        loggerType = type;
        QConfig.cfg().prepareCLI(type, args);

        if (QConfig.cfg().getFont().isEmpty()) {
            switch (type) {
                case client:
                    Uses.setUIFont(Font.decode("Tahoma-Plain-16"));
                    break;
                case desktop:
                    Uses.setUIFont(Font.decode("Tahoma-Plain-20"));
                    break;
                case reception:
                    Uses.setUIFont(Font.decode("Tahoma-Plain-14"));
                    break;
                default:
                    Uses.setUIFont(Font.decode("Tahoma-Plain-12"));
            }
        } else {
            if (!"no".equalsIgnoreCase(QConfig.cfg().getFont())) {
                Uses.setUIFont(Font.decode(QConfig.cfg().getFont()));
            }
        }

        final QLog log = LogerHolder.INSTANCE;
        About.load();
        QLog.l().logger.info("\"QSystem " + About.getVer() + "\"!  date: " + About.getDate());
        QLog.l().logger.info("START LOGER. Logger: " + QLog.l().logger().getName());
        if (QConfig.cfg().getModule().isServer()) {
            QLog.l().logger.info("Version DB=" + ServerProps.getInstance().getProps().getVersion());
            QLog.l().logRep.info("START LOGGER for reports. Logger: " + QLog.l().logRep().getName());
        }
        QLog.l().logger.info("Mode: " + (QConfig.cfg().isDebug() ? "KEY_DEBUG" : (QConfig.cfg().isDemo() ? "KEY_DEMO" : "FULL")));//NOSONAR
        QLog.l().logger.info("Plugins: " + (QConfig.cfg().isNoPlugins() ? "NO" : "YES"));
        if (QConfig.cfg().isUbtnStart()) {
            QLog.l().logger.info("Auto start: YES");
        }

        return log;
    }

    private static class LogerHolder {

        private static final QLog INSTANCE = new QLog();
    }
}
