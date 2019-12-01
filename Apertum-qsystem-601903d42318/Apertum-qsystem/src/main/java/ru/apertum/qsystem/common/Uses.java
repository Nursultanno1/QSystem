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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.util.Pair;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.FontUIResource;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.dom4j.Element;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import ru.apertum.qsystem.QSystem;
import ru.apertum.qsystem.client.Locales;
import ru.apertum.qsystem.client.forms.FServicePriority;
import ru.apertum.qsystem.common.exceptions.ServerException;
import ru.apertum.qsystem.extra.Agent;
import ru.apertum.qsystem.server.ServerProps;

/**
 * Утилитныйе ресурсы.
 *
 * @author Evgeniy Egorov Сдесь находятся константы и общеиспользуемые конструкции
 */
@SuppressWarnings("squid:S1319")
public final class Uses {

    private Uses() {
        // утилитный класс
    }

    // ключи системных параметров
    public static final String KEY_TICKET_NUMBERING = "ticket_numbering";

    // значения приоритета "очередника"
    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_NORMAL = 1;
    public static final int PRIORITY_HI = 2;
    public static final int PRIORITY_VIP = 3;
    public static final int[] PRIORITYS = {PRIORITY_LOW, PRIORITY_NORMAL, PRIORITY_HI, PRIORITY_VIP};//NOSONAR
    private static final LinkedHashMap<Integer, String> PRIORITYS_WORD = new LinkedHashMap<>();
    public static final String PROPERTIES_FILE = "config/qsystem.properties";

    /**
     * Как обзывать приоритеты для клиентов.
     *
     * @return список названий приоритетов.
     */
    public static LinkedHashMap<Integer, String> getPrioritysWord() {
        PRIORITYS_WORD.put(PRIORITY_LOW, FServicePriority.getLocaleMessage("client.priority.low"));
        PRIORITYS_WORD.put(PRIORITY_NORMAL, FServicePriority.getLocaleMessage("client.priority.standard"));
        PRIORITYS_WORD.put(PRIORITY_HI, FServicePriority.getLocaleMessage("client.priority.hi"));
        PRIORITYS_WORD.put(PRIORITY_VIP, FServicePriority.getLocaleMessage("client.priority.vip"));
        return PRIORITYS_WORD;
    }

    // значения приоритета обрабатываемых услуг для юзера
    public static final int SERVICE_REMAINS = 0;
    public static final int SERVICE_NORMAL = 1;
    public static final int SERVICE_VIP = 2;
    private static int extPriorClient = 0; // дополнительные приоритеты.

    public static void setExtPriorClient(int extPriorClient) {
        Uses.extPriorClient = extPriorClient;
    }

    private static final LinkedHashMap<Integer, String> COEFF_WORD = new LinkedHashMap<>();

    /**
     * Как обзывать приоритеты для услуг.
     *
     * @return список названий приоритетов.
     */
    public static LinkedHashMap<Integer, String> getCoeffWord() {
        COEFF_WORD.put(SERVICE_REMAINS, FServicePriority.getLocaleMessage("service.priority.low"));
        COEFF_WORD.put(SERVICE_NORMAL, FServicePriority.getLocaleMessage("service.priority.basic"));
        int n = 0;
        if (QConfig.cfg().getModule().isAdminApp()) {
            n = ServerProps.getInstance().getProps().getExtPriorNumber();
        }
        if (QConfig.cfg().getModule().isClient()) {
            n = extPriorClient;
        }
        for (int i = 2; i <= n + 1; i++) {
            COEFF_WORD.put(i, Integer.toString(i));
        }
        COEFF_WORD.put(SERVICE_VIP + n, FServicePriority.getLocaleMessage("service.priority.vip"));

        return COEFF_WORD;
    }

    // теги и имена атрибутов конфигурационных файлов главных табло
    public static final String TAG_BOARD_PROPS = "Параметры";
    public static final String TAG_BOARD_PROP = "Параметер";
    public static final String TAG_BOARD_NAME = "Наименование";
    public static final String TAG_BOARD_VALUE = "Значение";
    public static final String TAG_BOARD_TYPE = "Тип";
    public static final String TAG_BOARD_READ_ONLY = "ReadOnly";
    // имена параметров для табло 
    public static final String TAG_BOARD_FRACTAL = "Fractal";
    public static final String TAG_BOARD_MONITOR = "Номер дополнительного монитора для табло";
    public static final String TAG_BOARD_LINES_COUNT = "Количество строк на табло";
    public static final String TAG_BOARD_COLS_COUNT = "Количество столбцов на табло";
    public static final String TAG_BOARD_DELAY_VISIBLE = "Минимальное время индикации на табло";
    public static final String TAG_BOARD_FON_IMG = "Фоновое изображение";
    public static final String TAG_BOARD_FONT_SIZE = "Размер шрифта";
    public static final String TAG_BOARD_FONT_COLOR = "Цвет шрифта";
    public static final String TAG_BOARD_PANEL_SIZE = "Размер";
    public static final String TAG_BOARD_RUNNING_TEXT = "Бегущий текст";
    public static final String TAG_BOARD_VIDEO_FILE = "Видеофайл";
    public static final String TAG_BOARD_VISIBLE_PANEL = "visible";
    public static final String TAG_BOARD_SPEED_TEXT = "Скорость бегущего текста";
    public static final String TAG_BOARD_GRID_NEXT_COLS = "Колонки табл след";
    public static final String TAG_BOARD_GRID_NEXT_ROWS = "Строки табл след";
    public static final String TAG_BOARD_SIMPLE_DATE = "Простая дата";
    public static final String TAG_BOARD_GRID_NEXT = "Таблица следующих";
    public static final String TAG_BOARD_FON_COLOR = "Цвет фона";
    public static final String TAG_BOARD_FONT_SIZE_CAPTION = "Размер шрифта заголовка";
    public static final String TAG_BOARD_FONT_NAME = "Font name";
    public static final String TAG_BOARD_FONT_SIZE_LINE = "Размер шрифта строк";
    public static final String TAG_BOARD_FONT_COLOR_CAPTION = "Цвет шрифта заголовка";
    public static final String TAG_BOARD_FONT_COLOR_LEFT = "Цвет шрифта левого столбца";
    public static final String TAG_BOARD_FONT_COLOR_RIGHT = "Цвет шрифта правого столбца";
    public static final String TAG_BOARD_FONT_COLOR_LINE = "Цвет надписи строки табло";
    public static final String TAG_BOARD_LINE_BORDER = "Окантовка строк";
    public static final String TAG_BOARD_LINE_DELIMITER = "Разделитель столбцов";
    public static final String TAG_BOARD_LEFT_PIC = "Left column pic";
    public static final String TAG_BOARD_RIGHT_PIC = "Right column pic";
    public static final String TAG_BOARD_EXT_PIC = "Ext column pic";
    public static final String TAG_BOARD_LEFT_CAPTION = "Заголовок левого столбца";
    public static final String TAG_BOARD_RIGHT_CAPTION = "Заголовок правого столбца";
    public static final String TAG_BOARD_EXT_CAPTION = "Заголовок дополнительного столбца";
    public static final String TAG_BOARD_EXT_POSITION = "Порядок дополнительного столбца";
    public static final String TAG_BOARD_GRID_NEXT_CAPTION = "Заголовок таблицы следующих";
    public static final String TAG_BOARD_GRID_NEXT_FRAME_BORDER = "Рамка таблицы следующих";
    public static final String TAG_BOARD_LINE_COLOR = "Цвет рамки строки табло";
    public static final String TAG_BOARD_LINE_CAPTION = "Надпись строки табло";
    public static final String TAG_BOARD_CALL_PANEL = "Панель вызванного";
    public static final String TAG_BOARD_CALL_PANEL_BACKGROUND = "Картинка панели вызванного";
    public static final String TAG_BOARD_CALL_PANEL_X = "Панель вызванного-X";
    public static final String TAG_BOARD_CALL_PANEL_Y = "Панель вызванного-Y";
    public static final String TAG_BOARD_CALL_PANEL_WIDTH = "Панель вызванного-ширина";
    public static final String TAG_BOARD_CALL_PANEL_HEIGHT = "Панель вызванного-высота";
    public static final String TAG_BOARD_CALL_PANEL_DELAY = "Панель вызванного-время показа сек";
    public static final String TAG_BOARD_CALL_PANEL_TEMPLATE = "Панель вызванного-текст html+###";
    public static final String TAG_BOARD_HIDE_INVITE_BY_WORK = "Hide invitation by start working";
    //имена тегов-разделов для табло
    public static final String TAG_BOARD = "Board";
    public static final String TAG_BOARD_MAIN = "Main";
    public static final String TAG_BOARD_TOP = "Top";
    public static final String TAG_BOARD_BOTTOM = "Bottom";
    public static final String TAG_BOARD_BOTTOM_2 = "Bottom2";
    public static final String TAG_BOARD_LEFT = "Left";
    public static final String TAG_BOARD_RIGHT = "Right";
    // Наименования параметров конфигурационных файлов главных табло
    public static final String BOARD_VALUE_PAUSE = "Время присутствия записи на табло";
    public static final String BOARD_ADRESS_MAIN_BOARD = "Адрес главного табло системы";
    public static final int BOARD_TYPE_INT = 1;
    public static final int BOARD_TYPE_DOUBLE = 2;
    public static final int BOARD_TYPE_STR = 3;
    public static final int BOARD_TYPE_BOOL = 4;
    // Наименования заданий
    public static final String TASK_FOR_ALL_SITE = "Для всех сайтов домена";
    public static final String TASK_STAND_IN = "Поставить в очередь";
    public static final String TASK_STAND_COMPLEX = "Поставить в несколько очередей";
    public static final String TASK_ADVANCE_STAND_IN = "Поставить в очередь предварительно";
    public static final String TASK_ADVANCE_CHECK_AND_STAND = "Поставить предварительно записанного";
    public static final String TASK_REMOVE_ADVANCE_CUSTOMER = "Удалить предварительно записанного";
    public static final String TASK_REDIRECT_CUSTOMER = "Переадресовать клиента к другой услуге";
    public static final String TASK_GET_SERVICES = "Получить перечень услуг";
    public static final String TASK_ABOUT_SERVICE = "Получить описание услуги";
    public static final String TASK_GET_SERVICE_CONSISANCY = "Получить очередь услуги";
    public static final String TASK_ABOUT_SERVICE_PERSON_LIMIT = "Получить возможность встать с этими данными";
    public static final String TASK_GET_SERVICE_PREINFO = "Получить информацию по услуге";
    public static final String TASK_GET_INFO_PRINT = "Получить информацию для печати";
    public static final String TASK_GET_USERS = "Получить перечень пользователей";
    public static final String TASK_GET_SELF = "Получить описание пользователя";
    public static final String TASK_GET_SELF_SERVICES = "Получить состояние очередей";
    public static final String TASK_GET_POSTPONED_POOL = "Получить состояние пула отложенных";
    public static final String TASK_GET_BAN_LIST = "Получить список забаненых";
    public static final String TASK_INVITE_POSTPONED = "Вызвать отложенного из пула отложенных";
    public static final String TASK_GET_SELF_SERVICES_CHECK = "Получить состояние очередей с проверкой";
    public static final String TASK_INVITE_NEXT_CUSTOMER = "Получить следующего клиента";
    public static final String TASK_KILL_NEXT_CUSTOMER = "Удалить следующего клиента";
    public static final String TASK_CUSTOMER_TO_POSTPON = "Клиента в пул отложенных";
    public static final String TASK_POSTPON_CHANGE_STATUS = "Сменить статус отложенному";
    public static final String TASK_START_CUSTOMER = "Начать работу с клиентом";
    public static final String TASK_FINISH_CUSTOMER = "Закончить работу с клиентом";
    public static final String TASK_I_AM_LIVE = "Я горец!";
    public static final String TASK_RESTART = "RESTART";
    public static final String TASK_RESTART_MAIN_TABLO = "Рестарт главного твбло";
    public static final String TASK_REFRESH_POSTPONED_POOL = "NEW_POSTPONED_NOW";
    public static final String TASK_SERVER_STATE = "Получить состояние сервера";
    public static final String TASK_SET_SERVICE_FIRE = "Добавить услугу на горячую";
    public static final String TASK_DELETE_SERVICE_FIRE = "Удалить услугу на горячую";    // Наименования отчетов, сдесь писать исключительно маленькими латинскими буквами без пробелов
    public static final String TASK_GET_BOARD_CONFIG = "Получить конфигурацию табло";
    public static final String TASK_SAVE_BOARD_CONFIG = "Сохранить конфигурацию табло";
    public static final String TASK_GET_GRID_OF_WEEK = "Получить недельную предварительную таблицу";
    public static final String TASK_GET_GRID_OF_DAY = "Получить дневную предварительную таблицу";
    public static final String TASK_GET_INFO_TREE = "Получить информационное дерево";
    public static final String TASK_GET_RESULTS_LIST = "Получить получение списка возможных результатов";
    public static final String TASK_GET_RESPONSE_LIST = "Получить список отзывов";
    public static final String TASK_SET_RESPONSE_ANSWER = "Оставить отзыв";
    public static final String REPORT_CURRENT_USERS = "current_users";
    public static final String REPORT_CURRENT_SERVICES = "current_services";
    public static final String TASK_GET_CLIENT_AUTHORIZATION = "Идентифицировать клиента";
    public static final String TASK_SET_CUSTOMER_PRIORITY = "Изменить приоритет";
    public static final String TASK_CHECK_CUSTOMER_NUMBER = "Проверить номер";
    public static final String TASK_CHANGE_FLEX_PRIORITY = "Изменить гибкий приоритет";
    public static final String TASK_CHANGE_RUNNING_TEXT_ON_BOARD = "Изменить бегущий текст на табло";
    public static final String TASK_CHANGE_TEMP_AVAILABLE_SERVICE = "Изменить временную доступность";
    public static final String TASK_GET_STANDARDS = "Получить нормативы";
    public static final String TASK_SET_BUSSY = "Перерыв оператора";
    public static final String TASK_GET_PROPERTIES = "Все параметры из БД";
    public static final String TASK_SAVE_PROPERTIES = "Сохранить все параметры в БД";
    public static final String TASK_INIT_PROPERTIES = "Создать все параметры в БД";
    public static final String TASK_REINIT_ROLL = "Reinit roll of tickets";
    // Формат отчетов
    public static final String REPORT_FORMAT_HTML = "html";
    public static final String REPORT_FORMAT_RTF = "rtf";
    public static final String REPORT_FORMAT_PDF = "pdf";
    public static final String REPORT_FORMAT_XLSX = "xlsx";
    public static final String REPORT_FORMAT_CSV = "csv";
    // Якорь для списка аналитических отчетов
    public static final String ANCHOR_REPORT_LIST = "<tr><td><center>#REPORT_LIST_ANCHOR#</center></td></tr>";
    public static final String ANCHOR_DATA_FOR_REPORT = "#DATA_FOR_REPORT#";
    public static final String ANCHOR_ERROR_INPUT_DATA = "#ERROR_INPUT_DATA#";
    public static final String ANCHOR_USERS_FOR_REPORT = "#USERS_LIST_ANCHOR#";
    public static final String ANCHOR_PROJECT_NAME_FOR_REPORT = "#PROJECT_NAME_ANCHOR#";
    public static final String ANCHOR_COOCIES = "#COOCIES_ANCHOR#";
    // Задания для пункта регистрации
    public static final String WELCOME_LOCK = "#WELCOME_LOCK#";
    public static final String WELCOME_UNLOCK = "#WELCOME_UNLOCK#";
    public static final String WELCOME_OFF = "#WELCOME_OFF#";
    public static final String WELCOME_REINIT = "#WELCOME_REINIT#";

    public static String getRusDate(Date date, String format) {
        return new SimpleDateFormat(format, Locales.getInstance().getRussSymbolDateFormat()).format(date);
    }

    public static String getUkrDate(Date date, String format) {
        return new SimpleDateFormat(format, Locales.getInstance().getUkrSymbolDateFormat()).format(date);
    }

    /**
     * Временная папка для файлов сохранения состояния для помехоустойчивости.
     */
    public static final String TEMP_FOLDER = "temp";
    /**
     * Папка для файлов настроек.
     */
    public static final String CONFIG_FOLDER = "config";
    /**
     * временный файл сохранения состояния для помехоустойчивости.
     */
    public static final String TEMP_STATE_FILE = "temp.json";
    /**
     * временный файл сохранения состояния для помехоустойчивости.
     */
    public static final String TEMP_UPDLOG_FILE = "updlog.json";
    /**
     * временный файл сохранения конфигурации комплексных услуг.
     */
    public static final String TEMP_COMPLEX_FILE = "complex.json";
    /**
     * временный файл сохранения текущей статистики для помехоустойчивости.
     */
    public static final String TEMP_STATATISTIC_FILE = "temp_statistic.xml";
    /**
     * Задержка перед возвратом в корень меню при вопросе "Желаете встать в очередь?" когда в очереди более трех человек.
     */
    public static final int DELAY_BACK_TO_ROOT = 10000;
    /**
     * Задержка перед проверкой блокирования пункта регистрации.
     */
    public static final int DELAY_CHECK_TO_LOCK = 55000;
    /**
     * Константа возврата в пункт регистрации кол-во клиентов в очереди, в случае если услуга не обрабатывается ни одним пользователем.
     */
    public static final int LOCK_INT = 1000000000;
    /**
     * Константа возврата в пункт регистрации кол-во клиентов в очереди, в случае если услуга не оказывается учитывая расписание.
     */
    public static final int LOCK_FREE_INT = 1000000011;
    /**
     * Константа возврата в пункт регистрации кол-во клиентов в очереди, в случае если услуга не оказывается учитывая ограничение посещений в день и лимит.
     * достигнут
     */
    public static final int LOCK_PER_DAY_INT = 1000000022;

    /**
     * Константа возврата в пункт регистрации кол-во клиентов в очереди, в случае если нормально встаем в очередь, нет причин не встать.
     */
    public static final int NORMAL_RESPONCE = 0;
    /**
     * Константа возврата в пункт регистрации кол-во клиентов в очереди, в случае если есть причина для отказа. Текст отказа смотри в переменной сообщения.
     */
    public static final int NOT_POSSIBLE_IN_LINE = 2;

    /**
     * Вопрос о живости.
     */
    public static final String HOW_DO_YOU_DO = "do you live?";

    /**
     * Вывести строку в консоль с переходом на новую строку.
     *
     * @param text Может быть null.
     */
    public static void ln(String text) {
        if (text == null) {
            System.out.println(); //NOSONAR
            return;
        }
        System.out.println(text); //NOSONAR
    }

    /**
     * Вывести строку в консоль без перехода на новую строку.
     *
     * @param text Может быть null.
     */
    public static void lo(String text) {
        if (text == null) {
            System.out.println(); //NOSONAR
            return;
        }
        System.out.print(text); //NOSONAR
    }

    /**
     * Рекурентный формирователь для public static ArrayList elementsByAttr(...).
     *
     * @param list      массив элементов
     * @param el        корневой элемент ветви
     * @param attrName  имя искомых атрибутов
     * @param attrValue значение атрибута
     */
    private static void getList(ArrayList list, Element el, String attrName, String attrValue) {
        if (attrValue.equals(el.attributeValue(attrName))) {
            list.add(el);
        }
        el.elements().stream().forEach(obj -> getList(list, (Element) obj, attrName, attrValue));
    }

    /**
     * Возвращает массив эолементов с определенным значением атрибута из ветви.
     *
     * @param root      корневой элемент ветви
     * @param attrName  имя искомых атрибутов
     * @param attrValue значение атрибута
     * @return массив элементов
     */
    public static ArrayList<Element> elementsByAttr(Element root, String attrName, String attrValue) {
        ArrayList<Element> list = new ArrayList<>();
        getList(list, root, attrName, attrValue);
        return list;
    }

    /**
     * Послать сообщение по UDP.
     *
     * @param message текст посылаемого сообщения
     * @param address адрес получателя. Если адрес "255.255.255.255", то рассылка будет широковещательной.
     * @param port    порт получателя
     */
    public static void sendUDPMessage(String message, InetAddress address, int port) {
        QLog.l().logger().trace("Отправка UDP сообшение \"" + message + "\" по адресу \"" + address.getHostAddress() + "\" на порт \"" + port + "\"");
        final DatagramSocket socket;
        final byte[] messageBytes = message.getBytes();
        final DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, address, port);
        try {
            socket = new DatagramSocket();
        } catch (SocketException ex) {
            throw new ServerException("Проблемы с сокетом UDP." + ex.getMessage());
        }
        try {
            socket.send(packet);
        } catch (IOException io) {
            throw new ServerException("Ошибка отправки сообщения по UDP. " + io.getMessage());
        } finally {
            socket.close();
        }
    }

    /**
     * Послать сообщение по UDP широковещательно.
     *
     * @param message текст посылаемого сообщения
     * @param port    порт получателя
     */
    public static void sendUDPBroadcast(String message, int port) {
        try {
            sendUDPMessage(message, InetAddress.getByName("255.255.255.255"), port);
        } catch (UnknownHostException ex) {
            throw new ServerException("Проблемы с адресом " + ex.getMessage());
        }
    }

    /**
     * Загрузка ресурса из jar-файла.
     *
     * @param o            - класс, нужен для получения ресурса
     * @param resourceName путь к ресурсу в jar-файле
     * @return массив байт, содержащий ресурс
     * @throws IOException при чтении из джарника.
     */
    public static byte[] readResource(Object o, String resourceName) throws IOException {
        // Выдаем ресурс  "/ru/apertum/qsystem/reports/web/name.jpg"
        final InputStream inStream = o.getClass().getResourceAsStream(resourceName);
        return readInputStream(inStream);
    }

    /**
     * грузит картинку из файла или ресурсов. Если Параметр пустой, то возвращает null.
     *
     * @param o                   Объект для загрузки ресурса из jar, чаще всего класс в котором понадобилась эта картинка.
     * @param resourceName        путь к ресурсу или файлу картинки. Может быть пустым.
     * @param defaultResourceName Если нифайла ни ресурса не найдется, то загрузится этот ресурс
     * @return картинка.
     */
    @SuppressWarnings("squid:S3776")
    public static Image loadImage(Object o, String resourceName, String defaultResourceName) {
        if ("".equals(resourceName)) {
            return null;
        } else {
            Image img = HASH_IMG.get(resourceName);
            if (img != null) {
                return img;
            }

            final File f = new File(resourceName);
            if (f.exists()) {
                img = new ImageIcon(resourceName).getImage();
                HASH_IMG.put(resourceName, img);
                return img;
            } else {
                final InputStream is = o.getClass().getResourceAsStream(resourceName);
                if (is == null) {
                    if (defaultResourceName == null || defaultResourceName.isEmpty()) {
                        return new BufferedImage(2000, 2000, BufferedImage.TYPE_INT_RGB);
                    }
                    final InputStream stream = o.getClass().getResourceAsStream(defaultResourceName);
                    if (stream == null) {
                        QLog.l().logger().error("При загрузки ресурса не нашлось ни файла, ни ресурса, НИ ДЕФОЛТНОГО РЕСУРСА \"" + defaultResourceName + "\"");
                        return new BufferedImage(2000, 2000, BufferedImage.TYPE_INT_RGB);
                    } else {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            QLog.l().logger().error("При загрузки стрим не закрылся.");
                        }
                    }
                }

                byte[] b = null;
                try (final DataInputStream inStream = is == null ? new DataInputStream(o.getClass().getResourceAsStream(defaultResourceName)) : new DataInputStream(is)) {
                    b = new byte[inStream.available()];
                    inStream.readFully(b);
                } catch (IOException ex) {
                    QLog.l().logger().error(ex);
                }
                img = new ImageIcon(b).getImage();
                HASH_IMG.put(resourceName, img);
                return img;
            }
        }
    }

    private static final HashMap<String, Image> HASH_IMG = new HashMap<>();

    /**
     * Для чтения байт из потока. не применять для потока связанного с сокетом. readSocketInputStream(InputStream stream)
     *
     * @param stream из него читаем
     * @return byte[] результат
     * @throws java.io.IOException при чтении.
     */
    public static byte[] readInputStream(InputStream stream) throws IOException {
        final byte[] result;
        final DataInputStream dis = new DataInputStream(stream);
        result = new byte[stream.available()];
        dis.readFully(result);
        return result;
    }

    /**
     * Округление до нескольких знаков после запятой.
     *
     * @return Готовое обрезанное дробное число.
     */
    public static double roundAs(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.UP).doubleValue();
    }

    /**
     * Вызывает диалог выбора файла.
     *
     * @param parent      Относительно чего показывать форму диалога.
     * @param title       Заголовок диалогового окна.
     * @param description Описание фильтра, например "Файлы XML(*.xml)".
     * @param extension   Фильтр по расширению файлов, например "xml".
     * @return Полное имя файла или null если не выбрали.
     */
    public static String getFileName(Component parent, String title, String description, String extension) {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setLocale(Locales.getInstance().getLangCurrent());
        fileChooser.resetChoosableFileFilters();
        final FileNameExtensionFilter filter = new FileNameExtensionFilter(description, extension);
        fileChooser.setFileFilter(filter);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle(title);

        if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile().exists()) {
            return fileChooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    /**
     * Отцентирируем Окно по центру экрана.
     *
     * @param component это окно и будем центрировать.
     */
    public static void setLocation(JDialog component) {
        component.setLocationRelativeTo(null);
    }

    public static void setLocation(JDialog component, Frame parent) {
        component.setLocationRelativeTo(parent);
    }

    public static void setLocation(JFrame component) {
        component.setLocationRelativeTo(null);
    }

    public static void setLocation(JFrame component, Frame parent) {
        component.setLocationRelativeTo(parent);
    }

    /**
     * Растянем окно на весь экран.
     *
     * @param component это окно и будем растягивать.
     */
    public static void setFullSize(Component component) {
        component.setBounds(0, 0, firstMonitor.getDefaultConfiguration().getBounds().width, firstMonitor.getDefaultConfiguration().getBounds().height);
    }

    /**
     * mointors.
     */
    public static final HashMap<Integer, Rectangle> DISPLAYS = new HashMap<>();//NOSONAR
    public static GraphicsDevice firstMonitor = null;//NOSONAR

    static {
        /**
         * Инициализация.
         */
        GraphicsDevice[] screenDevices = null;
        try {
            screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        } catch (HeadlessException th) {
            QLog.l().logger().error("No screen Devices");
        }
        if (screenDevices != null && screenDevices.length > 0) {
            firstMonitor = screenDevices[0];
            int i = 1;
            for (GraphicsDevice graphicsDevice : screenDevices) {
                QLog.l().logger().info("monitor " + i + "; graphicsDevice = " + graphicsDevice.getIDstring() + " " + graphicsDevice.toString()
                        + "; height, width = " + graphicsDevice.getDefaultConfiguration().getBounds().height + "x" + graphicsDevice.getDefaultConfiguration().getBounds().width
                        + "; Coloreness = " + graphicsDevice.getDisplayMode().getBitDepth()
                        + "; RefreshRate = " + graphicsDevice.getDisplayMode().getRefreshRate()
                        + "; Origin(x, y) = " + graphicsDevice.getDefaultConfiguration().getBounds().x
                        + "-" + graphicsDevice.getDefaultConfiguration().getBounds().y);
                DISPLAYS.put(i++, graphicsDevice.getDefaultConfiguration().getBounds());
                if (graphicsDevice.getDefaultConfiguration().getBounds().x == 0
                        && graphicsDevice.getDefaultConfiguration().getBounds().y == 0) {
                    firstMonitor = graphicsDevice;
                }
            }
        }
        COEFF_WORD.put(SERVICE_REMAINS, FServicePriority.getLocaleMessage("service.priority.low"));
        COEFF_WORD.put(SERVICE_NORMAL, FServicePriority.getLocaleMessage("service.priority.basic"));
        COEFF_WORD.put(SERVICE_VIP, FServicePriority.getLocaleMessage("service.priority.vip"));
    }

    /**
     * Загрузка всех jar из папки в класспаф.
     *
     * @param folder из этой папки закрузим.
     */
    public static void loadPlugins(String folder) {
        // Загрузка плагинов из папки plugins
        QLog.l().logger().info("Загрузка плагинов из папки plugins.");
        final File[] list = new File(folder).listFiles((File dir, String name) -> name.toLowerCase().endsWith(".jar"));
        if (list != null && list.length != 0) {
            for (File file : list) { // NOSONAR
                QLog.l().logger().debug("Плагин " + file.getName());
                Agent.addClassPath(file);

                // посмотрим, есть ли описательная часть и загрузим её
                final String propFile = file.getName().substring(0, file.getName().indexOf('.'));
                final Properties properties = new Properties();
                final InputStream inStream = folder.getClass().getResourceAsStream("/" + propFile + ".properties");
                if (inStream != null) {
                    try (final InputStream inStream2 = inStream) {
                        properties.load(inStream2);
                    } catch (IOException ex) {
                        QLog.l().logger().error("Plugin description \"" + propFile + "+.properties\" was NOT loaded. " + ex);
                        continue;
                    }
                    QPlugins.get().addPlugin(new QPlugins.QPlugin(propFile,
                            properties.getProperty("type") == null ? QPlugins.Type.QPLUGIN : QPlugins.Type.valueOf(properties.getProperty("type")),
                            properties.getProperty("language"),
                            properties.getProperty("package")));
                }
            }
        }
    }

    /**
     * Милисеунды в строковое представление.
     *
     * @param msec милисекунды.
     * @return Строка со временем.
     */
    public static String msecToStr(long msec) {
        String str = "s.";
        msec = msec / 1000;
        str = msec % 60 + str;
        if (msec / 60 > 0) {
            msec = msec / 60;
            str = msec % 60 + "m. " + str;
            if (msec / 60 > 0) {
                msec = msec / 60;
                str = msec % 24 + "h. " + str;
                if (msec / 24 > 0) {
                    str = msec / 24 + "d. " + str;
                }
            }
        }
        return str;
    }

    /**
     * Класс заставки.
     */
    @SuppressWarnings("squid:MaximumInheritanceDepth")
    private static class SplashScreen extends JFrame {

        final BorderLayout borderLayout1 = new BorderLayout();
        final JLabel imageLabel = new JLabel();
        final JLabel imageLabel2 = new JLabel();
        final JLayeredPane lp = new JDesktopPane();
        final ImageIcon imageIcon;
        final ImageIcon imageIcon2;

        public SplashScreen() {
            imageIcon = new ImageIcon(SplashScreen.class.getResource("/ru/apertum/qsystem/client/forms/resources/fon_login_bl.jpg"));
            imageIcon2 = new ImageIcon(SplashScreen.class.getResource("/ru/apertum/qsystem/client/forms/resources/loading.gif"));
            init();
        }

        private void init() {
            try {
                setIconImage(ImageIO.read(SplashScreen.class.getResource("/ru/apertum/qsystem/client/forms/resources/client.png")));
            } catch (IOException ex) {
                QLog.l().logger().error(ex);
            }
            setTitle("Запуск QSystem");
            setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());
            imageLabel.setIcon(imageIcon);
            imageLabel2.setIcon(imageIcon2);
            lp.setBounds(0, 0, 400, 400);
            lp.setOpaque(false);
            add(lp);
            this.getContentPane().add(imageLabel, BorderLayout.CENTER);
            imageLabel2.setBounds(175, 165, 300, 30);
            lp.add(imageLabel2, null);
            timer.start();
        }

        final Timer timer = new Timer(100, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!sh) {
                    stopTimer();
                    setVisible(false);
                }
            }

            private void stopTimer() {
                timer.stop();
            }
        });
    }

    private static class SplashRun implements Runnable {

        @Override
        public void run() {
            final SplashScreen screen = new SplashScreen();
            screen.setUndecorated(true);
            screen.setResizable(false);
            setLocation(screen);
            screen.pack();
            screen.setVisible(true);
            screen.setAlwaysOnTop(true);
        }
    }

    private static boolean sh = false;

    /**
     * Создание и показ сплэш-заставки с блокировкой запуска второй копии.
     */
    public static void startSplashClient() {
        if (!QConfig.cfg().isTerminal()) {
            try {
                stopStartSecond = new ServerSocket(21210);
            } catch (Exception ex) {
                QLog.l().logger().error(".QSystem: Application alredy started!!!");
                System.exit(15685);
            }
        }
        showSplash();
    }

    static ServerSocket stopStartSecond;

    /**
     * Создание и показ сплэш-заставки без блокировки запуска второй копии.
     */
    public static void showSplash() {
        sh = true;
        SwingUtilities.invokeLater(new SplashRun());
    }

    /**
     * Скрытие сплэш-заставки.
     */
    public static void closeSplash() {
        sh = false;
    }

    /**
     * Установить внешни вид всей проги.
     */
    public static void setLookAndFeel() {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                /*Metal Nimbus CDE/Motif Windows   Windows Classic  */
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            QLog.l().logger().error(ex);
        }
    }

    /**
     * Выставить шрифт всему приложению.
     *
     * @param f шрифт.
     */
    public static void setUIFont(Font f) {
        Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource(f));
            }
        }
    }

    /**
     * Поправить урлы картинок в html.
     *
     * @param html текст
     * @return текст с поправленными урлами.
     */
    public static String prepareAbsolutPathForImg(String html) {
        final String file = "file:///";
        File f = new File(html.startsWith(file) ? html.substring(8) : html);
        if (f.exists()) {
            return f.toURI().toString().replace("file:/", file);
        }
        final Pattern pattern = Pattern.compile("<\\s*img\\s*src\\s*=\\s*['\"].*?['\"]\\s*>");//<img src='file:///E:\WORK\apertum-qsystem\config\board\q.jpg'>
        final Matcher matcher = pattern.matcher(html);
        String res = html;
        while (matcher.find()) {
            String img = matcher.group();
            final String tci = img.contains("'") ? "'" : "\"";
            img = img.substring(img.indexOf(tci) + 1, img.indexOf(tci, img.indexOf(tci) + 1));

            String ff = img;
            if (img.startsWith(file)) {
                ff = img.substring(8);
            }
            f = new File(ff);
            if (f.isFile()) {
                res = res.replace(tci + img + tci, tci + f.toURI().toString().replace("file:/", file) + tci);
            } else {
                QLog.l().logger().error("Не найден файл \"" + img + "\" для HTML.");
            }
        }
        return res;
    }

    public static boolean isInt(String str) {
        return str.matches("^-?\\d+$");
    }

    /**
     * Достать цыфры из строки.
     *
     * @param str строка с цыфрами.
     * @return список чисел.
     */
    public static ArrayList<Integer> getIntsFromString(String str) {
        return getIntsFromString(str, "[\\D]+");
    }

    /**
     * Достать цыфры из строки.
     *
     * @param str           строка с цыфрами.
     * @param regExpDivider регулярка-разделитель.
     * @return список чисел.
     */
    public static ArrayList<Integer> getIntsFromString(String str, String regExpDivider) {
        final ArrayList<Integer> res = new ArrayList<>();
        try (final Scanner s = new Scanner(str == null ? "" : str).useDelimiter(regExpDivider == null ? "[\\D]+" : regExpDivider)) {
            while (s.hasNextInt()) {
                res.add(s.nextInt());
            }
        }
        return res;
    }

    public static int[] getIntsFromString2(String str) {
        return Arrays.asList(str.split("\\D+")).stream().filter(line -> !line.isEmpty()).mapToInt(Integer::parseInt).toArray();
    }

    public static boolean isFloat(String str) {
        return str.matches("^-?\\d+\\.{0,1}\\d+$");
    }

    /**
     * Скачать файл по урлу.
     *
     * @param url                 от сюда качаем.
     * @param destinationFilePath сюда кладем.
     * @throws IOException по чтении или записи.
     */
    public static void downloadFile(String url, String destinationFilePath) throws IOException {
        final URL website = new URL(url);
        try (final ReadableByteChannel rbc = Channels.newChannel(website.openStream())) {
            try (final FileOutputStream fos = new FileOutputStream(destinationFilePath)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
        }
    }

    public static void unzip(String sourceFile, String destinationFolder) throws ZipException {
        final ZipFile zipFile = new ZipFile(sourceFile);
        zipFile.extractAll(destinationFolder);
    }

    /**
     * Получить MD5.
     *
     * @param filePath этого файла.
     * @return строка md5.
     * @throws IOException при чтении файла.
     */
    public static String md5(String filePath) throws IOException {
        
        try (FileInputStream fis = new FileInputStream(new File(filePath))) {
            
            return  org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
        }
    }

    /**
     * Получить параметры файла, доступного по URL.
     *
     * @param fileUrl URL расположения файла
     * @return Пара значений [key=LastModified, value= ContentLength]
     * @throws IOException чтение не удалось.
     */
    public static Pair<Long, Long> aboutFile(String fileUrl) throws IOException {
        final URL url = new URL(fileUrl);
        final URLConnection urlConnection = url.openConnection();
        return new Pair<>(urlConnection.getLastModified(), urlConnection.getContentLengthLong());
    }

    private static ResourceMap localeMap = null;

    /**
     * ЛОкализованные строки из ресурса QSystem.class.
     *
     * @param key по ключу.
     * @return строка на языке.
     */
    public static String getLocaleMessage(String key) {
        if (localeMap == null) {
            localeMap = Application.getInstance(QSystem.class).getContext().getResourceMap(Uses.class);
        }
        return localeMap.getString(key);
    }
}
