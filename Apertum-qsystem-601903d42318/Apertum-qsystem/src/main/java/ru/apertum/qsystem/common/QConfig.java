/*
 *  Copyright (C) 2016 {Apertum}Projects. web: www.apertum.ru email: info@apertum.ru
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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedBuilderParametersImpl;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.ex.ConfigurationException;
import ru.apertum.qsystem.common.exceptions.ServerException;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Manager of configure. It Holds all mechanisms for using properties and providing it for other consumers.
 *
 * @author evgeniy.egorov
 */
@SuppressWarnings("squid:S106")
public final class QConfig {

    private static final String KEY_DEBUG = "debug";
    // ключ, отвечающий за режим демонстрации. При нем не надо прятать мышку и убирать шапку формы
    // Режим демонстрации. При нем не надо прятать мышку и убирать шапку формы.
    private static final String KEY_DEMO = "demo";
    private static final String KEY_IDE = "ide";
    private static final String KEY_START = "ubtn-start";
    // ключ, отвечающий за возможность загрузки плагинов. 
    private static final String KEY_NOPLUGINS = "noplugins";
    // ключ, отвечающий за паузу на старте. 
    private static final String KEY_DELAY = "delay";
    // ключ, отвечающий за возможность работы клиента на терминальном сервере. 
    private static final String KEY_TERMINAL = "terminal";
    // ключ, отвечающий за возможность работы регистрации в кнопочном исполнении.
    // ключ, отвечающий за возможность работы регистрации при наличии только некой клавиатуры. Список услуг в виде картинки с указанием что нажать на клаве для той или иной услуги 
    //touch,info,med,btn,kbd
    public static final String KEY_WELCOME_MODE = "welcome-mode";
    public static final String KEY_WELCOME_TOUCH = "touch";
    public static final String KEY_WELCOME_INFO = "info";
    public static final String KEY_WELCOME_MED = "med";
    public static final String KEY_WELCOME_BTN = "btn";
    public static final String KEY_WELCOME_KBD = "kbd";
    //Всегда грузим temp.json и никогда не чистим состояние.
    private static final String KEY_RETAIN = "retain";
    private static final String KEY_CLANGS = "change-langs";
    // ключ, отвечающий за паузу при вызове только что вставшего с очередь. Чтоб в зал успел вбежать.
    private static final String KEY_DELAY_INVITE_FIRST = "delay-first-invite";
    private static final String KEY_HTTP = "http-server";
    private static final String KEY_HTTP_PROTOCOL_POST = "http-protocol-post";
    private static final String KEY_HTTP_PROTOCOL_GET = "http-protocol-get";
    private static final String KEY_POINT = "point";
    private static final String KEY_BOARD_CFG = "board-config";
    private static final String KEY_BOARD_FX_CFG = "board-fx-config";
    private static final String KEY_S = "server-address";
    private static final String KEY_S_PORT = "server-port";
    private static final String KEY_C_PORT = "client-port";
    private static final String KEY_USER = "user";

    private static final String KEY_USE_EXT_PRIORITY = "use-ext-prority";
    private static final String KEY_USE_HIDE_PROPS = "use-hide-props";
    private static final String KEY_UNIT_TEST = "unit-test";

    private static final String KEY_NUM_DIVIDER = "number-divider";

    private static final String ZKEY_BOARD_CFG = "zboard-config";
    private static final String TKEY_BOARD_CFG = "tboard-config";

    private static final String KEY_NO_HIDE_CURSOR = "no-hide-cursor";
    private static final String KEY_PROXY = "proxy";
    private static final String KEY_NO_PROXY = "noproxy";

    private static final String KEY_VOICE = "voice";
    private static final String KEY_FONT = "font";
    private static final String KEY_NO_HIDE_TRAY = "no-hide-to-tray";
    private static final String KEY_NO_TRAY = "no-tray";

    private static final String KEY_LOG4J2_CONFIG = "log4j2-config";

    private final FileBasedConfiguration config;

    private QConfig() {
        try {
            line = parser.parse(new Options(), new String[0]);
            if (new File(Uses.PROPERTIES_FILE).exists()) {
                final FileBasedConfigurationBuilder<FileBasedConfiguration> builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(new FileBasedBuilderParametersImpl().setFileName(Uses.PROPERTIES_FILE).setEncoding("utf8"));
                builder.setAutoSave(true);
                config = builder.getConfiguration();
                // config contains all properties read from the file
            } else {
                this.config = new PropertiesConfiguration();
            }
        } catch (ParseException | ConfigurationException ex) {
            throw new RuntimeException("Properties file wasn't read.", ex); // NOSONAR
        }
    }

    @SuppressWarnings("squid:S3776")
    private Options initOptions(QModule type) {
        final Options options = new Options();
        this.qModule = type;

        options.addOption("?", "hey", false, "Show information about command line arguments");
        options.addOption("h", "help", false, "Show information about command line arguments");
        options.addOption("logcfg", KEY_LOG4J2_CONFIG, true, "Config file for log4j2.");

        /*
         CLIENT: ide -s 127.0.0.1 -cport 3129 -sport 3128 -cfg config/clientboard.xml -cfgfx1 config/clientboardfx.properties -point1 234 debug -terminal1
         RECEPTION: ide -s 127.0.0.1 -cport 3129 -sport 3128  debug
         WELCOME: ide -s 127.0.0.1 -sport 3128 -cport 3129 debug med info1 -buttons1 demo1 -clangs1 -keyboard1
         */
        // 0-сервер,1-клиент,2-приемная,3-админка,4-киоск,5-сервер хардварных кнопок, 26 - зональник, 17 - редактор табло
        options.addOption("ndiv", KEY_NUM_DIVIDER, true, "Divider for client ticket number between prefix and number. For ex: A-800. Default is empty.");
        options.addOption("d", KEY_DEBUG, false, "Debug mode. Show all messages in console and do not make forms fulscreen.");
        if (type.isUnknown() || type.isServer() || type.isClient() || type.isDesktop() || type.isWelcome() || type.isZoneBoard()) {
            options.addOption(KEY_DEMO, false, "Demo mode. You can use mouse and you can see header of forms.");
            options.addOption("nhc", KEY_NO_HIDE_CURSOR, false, "No-hide-cursor mode. In some linux GUI could be problen with hide cursor.");
        }

        options.addOption(KEY_IDE, false, "Do not touch it!");

        if (type.isUnknown() || type.isUB()) {
            options.addOption("ubs", KEY_START, false, "Auto start for hardware user buttons.");
        }

        options.addOption("np", KEY_NOPLUGINS, false, "Do not load plugins.");
        options.addOption("nty", KEY_NO_TRAY, false, "Do not use tray.");

        Option option = new Option("p", KEY_DELAY, true, "Do delay before starting. It can be useful for waiting for prepared other components of QSystem.");
        option.setArgName("in seconds");
        options.addOption(option);

        if (type.isUnknown() || type.isServer() || type.isClient()) {
            options.addOption("t", KEY_TERMINAL, false, "If QSystem working in terminal environment. Not on dedicated computers.");
        }

        if (type.isUnknown() || type.isWelcome()) {
            option = new Option("wm", KEY_WELCOME_MODE, true, "If welcome app is not a touch kiosk.\ninfo - just show and print an information.\n"
                    + "med - Input some number and stand for advance.\nbtn - if it is special hardware buttons device.\n"
                    + "kbd- Ability to work for registration point if there is only a keyboard ar mouse. "
                    + "A list of services in the form of a picture which indicate that to press on keyboard or mouse for a particular service.");
            option.setArgName("touch,info,med,btn,kbd");
            options.addOption(option);
            options.addOption("cl", KEY_CLANGS, false, "Manage multi language mode before start welcome point.");
        }

        if (type.isUnknown() || type.isServer()) {
            options.addOption("r", KEY_RETAIN, false, "Always to keep the state after restart the server QSystem.");
            option = new Option("dfi", KEY_DELAY_INVITE_FIRST, true, "Pause before calling a client by user after getting line. To have time to run into the room.");
            option.setArgName("in seconds");
            options.addOption(option);
            option = new Option("http", KEY_HTTP, true, "To start built-in http server which support servlets and web-socket. Specify a port.");
            option.setArgName("port");
            options.addOption(option);
        }

        if (type.isUnknown() || type.isClient() || type.isDesktop()) {
            option = new Option("pt", KEY_POINT, true, "Alternative label for user's workplace..");
            option.setOptionalArg(true);
            option.setArgName("label");
            options.addOption(option);
            option = new Option("cfg", KEY_BOARD_CFG, true, "Config xml file for main board.");
            option.setArgName("xml-file");
            options.addOption(option);
            option = new Option("cfgfx", KEY_BOARD_FX_CFG, true, "Config properties file for main board as FX form.");
            option.setArgName("file");
            options.addOption(option);
            option = new Option("u", KEY_USER, true, "User ID for fast login into client app. From DB.");
            option.setArgName("user ID");
            options.addOption(option);
            options.addOption("v", KEY_VOICE, false, "User ID for fast login into client app. From DB.");
            options.addOption("nhtty", KEY_NO_HIDE_TRAY, false, "Do not to hide to tray.");
        }

        if (type.isUnknown() || type.isClient() || type.isReception() || type.isWelcome() || type.isDesktop() || type.isTabloRedaktor()) {
            //-s 127.0.0.1 -sport 3128 -cport 3129
            option = new Option("s", KEY_S, true, "Address of QMS QSystem server.");
            option.setArgName("label");
            options.addOption(option);
            option = new Option("sport", KEY_S_PORT, true, "TCP port of QMS QSystem server.");
            option.setArgName("port");
            options.addOption(option);
            option = new Option("cport", KEY_C_PORT, true, "UDP port of user's computer for receiving message from server.");
            option.setArgName("port");
            options.addOption(option);
        }
        if (type.isUnknown() || type.isClient() || type.isReception() || type.isAdminApp() || type.isWelcome() || type.isUB() || type.isDesktop()) {
            option = new Option("httpp", KEY_HTTP_PROTOCOL_POST, true, "Use HTTP as protocol for transporting POST commands from clients to server.");
            option.setArgName("port");
            options.addOption(option);

            option = new Option("httpg", KEY_HTTP_PROTOCOL_GET, true, "Use HTTP as protocol for transporting GET commands from clients to server.");
            option.setArgName("port");
            options.addOption(option);

            option = new Option("px", KEY_PROXY, true, "Proxy settings hostName:port. Example 127.0.0.1:2130");
            option.setArgName("hostName:port");
            options.addOption(option);

            option = new Option("nopx", KEY_NO_PROXY, false, "Do not use proxy.");
            options.addOption(option);

            option = new Option("f", KEY_FONT, true, "Font for GUI. 'no' = default. Use pattern like name-type-size. In which style is one of the four case-insensitive strings: "
                    + "\"PLAIN\", \"BOLD\", \"BOLDITALIC\", or \"ITALIC\", and pointsize is a positive decimal integer representation of the point size. "
                    + "For example, if you want a font that is Arial, bold, with a point size of 18, you would call this method with: \"Arial-BOLD-18\"."
                    + " For ex.: Tahoma-Plain-14 or Arial-Bold-12 or Tahoma-Italic-16");
            option.setArgName("name-type-size");
            options.addOption(option);
        }
        if (type.isUnknown() || type.isAdminApp()) {
            options.addOption("uep", KEY_USE_EXT_PRIORITY, false, "Bad. Forget about it. This is amount of additional priorities for services.");
            options.addOption("uhp", KEY_USE_HIDE_PROPS, false, "Attention. It is for fine tuning and experienced administrators.");
            options.addOption("ut", KEY_UNIT_TEST, false, "Editing DB for unit tests.");
        }
        if (type.isUnknown() || type.isZoneBoard()) {
            option = new Option("zcfg", ZKEY_BOARD_CFG, true, "Config xml file for zone board.");
            option.setArgName("xml-file zone");
            options.addOption(option);
        }
        if (type.isUnknown() || type.isTabloRedaktor()) {
            option = new Option("tcfg", TKEY_BOARD_CFG, true, "Config xml file for board.");
            option.setArgName("xml-file red");
            options.addOption(option);
        }
        try {
            // create the parser
            line = parser.parse(options, new String[0]);
        } catch (ParseException ex) {
            // задавить по тихому.
        }

        try {
            tcpServerAddress = InetAddress.getByName(getServerAddress());
        } catch (UnknownHostException exception) {
            throw new ServerException("Address TCP server is not correct.", exception);
        }

        return options;
    }

    public static QConfig cfg() {
        return ConfigHolder.INSTANCE;
    }

    /**
     * qModule 0-сервер,1-клиент,2-приемная,3-админка,4-киоск,5-сервер хардварных кнопок.
     */
    private QModule qModule = QModule.unknown;

    public QModule getModule() {
        return qModule;
    }

    /**
     * Порт для приема сообщения выключения.
     */
    public int getStoppingPort() {
        return line.hasOption("stoppingport")
                ? Integer.parseInt(line.getOptionValue("stoppingport", "27001"))
                : 27001;
    }

    private static class ConfigHolder {

        private static final QConfig INSTANCE = new QConfig();
    }

    private CommandLine line;
    private final CommandLineParser parser = new DefaultParser();

    /**
     * Готовим аргументы.
     *
     * @param module для какого модуля
     * @param args   cmd params
     * @return конфигурация.
     */
    public QConfig prepareCLI(QModule module, String[] args) {
        final Options options = initOptions(module);
        try {
            // parse the command line arguments
            line = parser.parse(options, args);
        } catch (ParseException exp) {
            // oops, something went wrong
            System.err.println("\n\n\nCommand line commands were failed.  Reason: " + exp.getMessage());
            System.exit(2);
        }

        new HelpFormatter().printHelp("command line parameters for QMS QSystem...", options);
        // automatically generate the help statement
        if (line.hasOption("help") || line.hasOption("h") || line.hasOption("?")) {
            System.exit(0);
        }
        QLog.l().logger().info("Properties are ready.");
        return this;
    }

    public boolean isDebug() {
        return line.hasOption(KEY_DEBUG) || config.getBoolean(KEY_DEBUG, false);
    }

    public String getLog4j2Cfg() {
        return line.hasOption(KEY_LOG4J2_CONFIG) ? config.getString(KEY_LOG4J2_CONFIG) : null;
    }

    public boolean isDemo() {
        return line.hasOption(KEY_DEMO) || config.getBoolean(KEY_DEMO, false);
    }

    public boolean isHideCursor() {
        return !line.hasOption(KEY_NO_HIDE_CURSOR) && !config.getBoolean(KEY_NO_HIDE_CURSOR, false);
    }

    public boolean isIDE() {
        return line.hasOption(KEY_IDE) || config.getBoolean(KEY_IDE, false);
    }

    public boolean isUbtnStart() {
        return line.hasOption(KEY_START) || config.getBoolean(KEY_START, false);
    }

    public boolean isNoPlugins() {
        return line.hasOption(KEY_NOPLUGINS) || config.getBoolean(KEY_NOPLUGINS, false);
    }

    public boolean isPlaginable() {
        return !isNoPlugins();
    }

    /**
     * Задержка перед стартом.
     *
     * @return секунды.
     */
    public int getDelay() {
        try {
            return line.hasOption(KEY_DELAY)
                    ? Integer.parseInt(line.getOptionValue(KEY_DELAY, "0"))
                    : config.getInt(KEY_DELAY, 0);
        } catch (NumberFormatException ex) {
            System.err.println(ex);
            return 15;
        }
    }

    public boolean isTerminal() {
        return line.hasOption(KEY_TERMINAL) || config.getBoolean(KEY_TERMINAL, false);
    }

    /**
     * touch,info,med,btn,kbd.
     *
     * @return mode.
     */
    public String getWelcomeMode() {
        return line.hasOption(KEY_WELCOME_MODE)
                ? line.getOptionValue(KEY_WELCOME_MODE, KEY_WELCOME_TOUCH)
                : config.getString(KEY_WELCOME_MODE, KEY_WELCOME_TOUCH);
    }

    /**
     * -wm(--welcome-mode) info.
     *
     * @return mode is info.
     */
    public boolean isWelcomeInfo() {
        return KEY_WELCOME_INFO.equalsIgnoreCase(getWelcomeMode());
    }

    public boolean isChangeLangs() {
        return line.hasOption(KEY_CLANGS) || config.getBoolean(KEY_CLANGS, false);
    }

    public boolean isRetain() {
        return line.hasOption(KEY_RETAIN) || config.getBoolean(KEY_RETAIN, false);
    }

    /**
     * Задержка перед вызовом только что пришедшего человека.
     *
     * @return секунды.
     */
    public int getDelayFirstInvite() {
        try {
            return line.hasOption(KEY_DELAY_INVITE_FIRST)
                    ? Integer.parseInt(line.getOptionValue(KEY_DELAY_INVITE_FIRST, "15"))
                    : config.getInt(KEY_DELAY_INVITE_FIRST, 15);
        } catch (NumberFormatException ex) {
            System.err.println(ex);
            return 15;
        }
    }

    /**
     * Порт HTTP.
     *
     * @return порт.
     */
    public int getHttp() {
        try {
            return line.hasOption(KEY_HTTP)
                    ? Integer.parseInt(line.getOptionValue(KEY_HTTP, "0"))
                    : config.getInt(KEY_HTTP, 0);
        } catch (NumberFormatException ex) {
            System.err.println(ex);
            return 0;
        }
    }

    /**
     * Порт на который надо отправлять HTTP запросы. 0 - отключено.
     *
     * @return Порт на который надо отправлять HTTP запросы. 0 - отключено
     */
    public int getHttpProtocol() {
        try {
            return line.hasOption(KEY_HTTP_PROTOCOL_GET) || line.hasOption(KEY_HTTP_PROTOCOL_POST)
                    ? Integer.parseInt(line.getOptionValue(KEY_HTTP_PROTOCOL_GET, line.getOptionValue(KEY_HTTP_PROTOCOL_POST, "0")))
                    : config.getInt(KEY_HTTP_PROTOCOL_GET, config.getInt(KEY_HTTP_PROTOCOL_POST, 0));
        } catch (NumberFormatException ex) {
            System.err.println(ex);
            return 0;
        }
    }

    /**
     * Проверка на протокол общения с сервером и тип запросов.
     *
     * @return null - не использовать http, true - POST, false - GET
     */
    public HttpRequestType getHttpRequestType() {
        if (getHttpProtocol() > 0) {
            return line.hasOption(KEY_HTTP_PROTOCOL_POST) || config.getInt(KEY_HTTP_PROTOCOL_POST, 0) > 0 ? HttpRequestType.POST : HttpRequestType.GET;
        } else {
            return HttpRequestType.NOT_USE_HTTP;
        }
    }

    public enum HttpRequestType {
        GET, POST, NOT_USE_HTTP;

        public boolean isPost() {
            return this == POST;
        }

        public boolean isGet() {
            return this == GET;
        }

        public boolean isNotUseHttp() {
            return this == NOT_USE_HTTP;
        }

        public boolean doUseHttp() {
            return this != NOT_USE_HTTP;
        }
    }

    /**
     * Использовать дополнительные приоритеты.
     */
    public boolean useExtPriorities() {
        return line.hasOption(KEY_USE_EXT_PRIORITY) || config.getBoolean(KEY_USE_EXT_PRIORITY, false);
    }

    /**
     * Показывать скрытые настройки.
     * @return 
     */
    public boolean showHidenProps() {
        return line.hasOption(KEY_USE_HIDE_PROPS) || config.getBoolean(KEY_USE_HIDE_PROPS, false);
    }

    /**
     * Переопределение пункта оператора.
     */
    public String getPoint() {
        return line.hasOption(KEY_POINT)
                ? line.getOptionValue(KEY_POINT, "")
                : config.getString(KEY_POINT, "");
    }

    public String getPointN() {
        return getPoint() == null || getPoint().isEmpty() ? null : getPoint();
    }

    public String getUserID() {
        return line.getOptionValue(KEY_USER, "");
    }

    /**
     * Настройка таблушки.
     */
    public String getBoardCfgFile() {
        return line.hasOption(KEY_BOARD_CFG)
                ? line.getOptionValue(KEY_BOARD_CFG, "")
                : config.getString(KEY_BOARD_CFG, "");
    }

    /**
     * настройка FX табло.
     */
    public String getBoardCfgFXfile() {
        return line.hasOption(KEY_BOARD_FX_CFG)
                ? line.getOptionValue(KEY_BOARD_FX_CFG, "")
                : config.getString(KEY_BOARD_FX_CFG, "");
    }

    /**
     * Адрес сервера.
     */
    public String getServerAddress() {
        return line.hasOption(KEY_S)
                ? line.getOptionValue(KEY_S, "127.0.0.1")
                : config.getString(KEY_S, "127.0.0.1");
    }

    private InetAddress tcpServerAddress;

    public InetAddress getInetServerAddress() {
        return tcpServerAddress;
    }

    /**
     * Серверный порт.
     */
    public int getServerPort() {
        try {
            return line.hasOption(KEY_S_PORT)
                    ? Integer.parseInt(line.getOptionValue(KEY_S_PORT, "3128"))
                    : config.getInt(KEY_S_PORT, 3128);
        } catch (NumberFormatException ex) {
            System.err.println(ex);
            return 3128;
        }
    }

    /**
     * Порт клиента.
     */
    public int getClientPort() {
        try {
            return line.hasOption(KEY_C_PORT)
                    ? Integer.parseInt(line.getOptionValue(KEY_C_PORT, "3129"))
                    : config.getInt(KEY_C_PORT, 3129);
        } catch (NumberFormatException ex) {
            System.err.println(ex);
            return 3129;
        }
    }

    /**
     * Чем разделять номер между префиксом и числом.
     *
     * @param prefix префикс.
     */
    public String getNumDivider(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return "";
        } else {
            return line.hasOption(KEY_NUM_DIVIDER) ? line.getOptionValue(KEY_NUM_DIVIDER, "").replaceAll("_", " ") : "";
        }
    }

    /**
     * Конфиг зональника.
     */
    public String getZoneBoardCfgFile() {
        return line.hasOption(ZKEY_BOARD_CFG)
                ? line.getOptionValue(ZKEY_BOARD_CFG, "")
                : config.getString(ZKEY_BOARD_CFG, "");
    }

    /**
     * Файл конфигурации главного табло.
     */
    public String getTabloBoardCfgFile() {
        return line.hasOption(TKEY_BOARD_CFG)
                ? line.getOptionValue(TKEY_BOARD_CFG, "")
                : config.getString(TKEY_BOARD_CFG, "");
    }

    public static final class QProxy {

        public final String host;
        public final int port;

        public QProxy(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

    /**
     * Прокси настройки из аргументов.
     *
     * @return QProxy для сети.
     */
    public QProxy getProxy() {
        if (line.hasOption(KEY_PROXY)) {
            final String[] ss = line.getOptionValue(KEY_PROXY, "").split(":");
            if (ss.length == 2 && Uses.isInt(ss[1])) {
                return new QProxy(ss[0], Integer.parseInt(ss[1]));
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Не использовать прокси в сети.
     *
     * @return да или нет.
     */
    public boolean doNotUseProxy() {
        return line.hasOption(KEY_NO_PROXY) || config.getBoolean(KEY_NO_PROXY, false);
    }

    /**
     * Проговорить ли голосом вызов нв клиенте.
     *
     * @return true если есть ключ в консоле или файле настроек.
     */
    public boolean needVoice() {
        return line.hasOption(KEY_VOICE) || config.getBoolean(KEY_VOICE, false);
    }

    public String getFont() {
        return line.hasOption(KEY_FONT) ? line.getOptionValue(KEY_FONT, "") : "";
    }

    /**
     * Иногда трей не поддерживается.
     *
     * @return true если не надо трей.
     */
    public boolean doNotUseTray() {
        return line.hasOption(KEY_NO_TRAY) || config.getBoolean(KEY_NO_TRAY, false);
    }

    /**
     * Иногда не надо скрывать в трей клиента. true если не надо в трей.
     *
     * @return true если не надо в трей.
     */
    public boolean doNotHideToTray() {
        return doNotUseTray() || line.hasOption(KEY_NO_HIDE_TRAY) || config.getBoolean(KEY_NO_HIDE_TRAY, false);
    }

    /**
     * Для редактирования БД Н2 для тестов installation/resource/bin/QSystemDB_UT.mv.db.
     *
     * @return true если подключаем БД installation/resource/bin/QSystemDB_UT.mv.db.
     */
    public boolean isUnitTest() {
        return line.hasOption(KEY_UNIT_TEST);
    }
}
