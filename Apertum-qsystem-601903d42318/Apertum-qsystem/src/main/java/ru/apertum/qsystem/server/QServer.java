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
package ru.apertum.qsystem.server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.SystemUtils;
import ru.apertum.qsystem.About;
import ru.apertum.qsystem.client.Locales;
import ru.apertum.qsystem.client.QProperties;
import ru.apertum.qsystem.client.forms.FAbout;
import ru.apertum.qsystem.common.CodepagePrintStream;
import ru.apertum.qsystem.common.GsonPool;
import ru.apertum.qsystem.common.Mailer;
import ru.apertum.qsystem.common.QConfig;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.QModule;
import ru.apertum.qsystem.common.Updater;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.cmd.JsonRPC20;
import ru.apertum.qsystem.common.exceptions.ServerException;
import ru.apertum.qsystem.common.model.ATalkingClock;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.extra.IStartServer;
import ru.apertum.qsystem.hibernate.ChangeServerAction;
import ru.apertum.qsystem.reports.model.QReportsList;
import ru.apertum.qsystem.reports.net.WebServer;
import ru.apertum.qsystem.server.controller.Executer;
import ru.apertum.qsystem.server.http.JettyRunner;
import ru.apertum.qsystem.server.model.QProperty;
import ru.apertum.qsystem.server.model.QService;
import ru.apertum.qsystem.server.model.QServiceTree;
import ru.apertum.qsystem.server.model.QUser;
import ru.apertum.qsystem.server.model.QUserList;
import ru.apertum.qsystem.server.model.postponed.QPostponedList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static ru.apertum.qsystem.common.QLog.log;
import static ru.apertum.qsystem.common.Uses.ln;
import static ru.apertum.qsystem.common.Uses.lo;

/**
 * Класс старта и exit инициализации сервера. Организация потоков выполнения заданий.
 *
 * @author Evgeniy Egorov
 */
public class QServer implements Runnable {

    private static final AtomicInteger INT_THREAD_COUNTER = new AtomicInteger();

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool((Runnable r) -> {
        final Thread thread = new Thread(r);
        thread.setName("qth-" + INT_THREAD_COUNTER.getAndIncrement());
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    });

    private final Socket socket;

    /**
     * Старт сервера очередей. Самый мегастарт.
     *
     * @param args - первым параметром передается полное имя настроечного XML-файла.
     */
    public static void main(String[] args) throws Exception {
        QLog.initial(args, QModule.server);
        About.printdef();
        Locale.setDefault(Locales.getInstance().getLangCurrent());

        //Установка вывода консольных сообщений в нужной кодировке
        if (!QConfig.cfg().isIDE() && SystemUtils.IS_OS_WINDOWS) {
            try {
                String consoleEnc = System.getProperty("console.encoding", "Cp866");
                System.setOut(new CodepagePrintStream(System.out, consoleEnc));//NOSONAR
                System.setErr(new CodepagePrintStream(System.err, consoleEnc));//NOSONAR
            } catch (UnsupportedEncodingException e) {
                ln("Unable to setup console codepage: " + e);
            }
        }

        ln("Welcome to the QSystem server. Your MySQL mast be prepared.");
        FAbout.loadVersionSt();

        printLicense();

        final long start = System.currentTimeMillis();

        // Загрузка плагинов из папки plugins
        if (!QConfig.cfg().isNoPlugins()) {
            Uses.loadPlugins("./plugins/");
        }

        // посмотрим не нужно ли стартануть jetty
        // для этого нужно запускать с ключом http
        // если етсь ключ http, то запускаем сервер и принимаем на нем команды серверу суо
        startJetty();

        // Отчетный сервер, выступающий в роли вэбсервера, обрабатывающего запросы на выдачу отчетов
        WebServer.getInstance().startWebServer(ServerProps.getInstance().getProps().getWebServerPort());
        loadPool();
        // запускаем движок индикации сообщения для кастомеров, дождемся старта вэбсервера для контента на табло.
        int wall = 0;
        ln("Jetty: ");
        while (!JettyRunner.STARTED_FLAG.get() && ++wall < 20) {
            lo(".");
            Thread.sleep(1000);
        }
        ln("");
        if (JettyRunner.STARTED_FLAG.get()) {
            log().info("Jetty was started normally on port {}.", QConfig.cfg().getHttp());
        } else {
            if (QConfig.cfg().getHttp() > 0) {
                log().error("Jetty was not started on port {}.", QConfig.cfg().getHttp());
            }
        }
        MainBoard.getInstance().showBoard();

        startCommonTimers();

        // подключения плагинов, которые стартуют в самом начале.
        // поддержка расширяемости плагинами
        for (final IStartServer event : ServiceLoader.load(IStartServer.class)) {
            log().info("Вызов SPI расширения. Описание: " + event.getDescription());
            try {
                new Thread(event::start).start();
            } catch (Exception tr) {
                log().error("Вызов SPI расширения завершился ошибкой. Описание: " + tr);
            }
        }

        // привинтить сокет на локалхост, порт 3128
        makeSocketAndRun(ServerProps.getInstance().getProps().getServerPort());

        log().debug("Останов Jetty.");
        JettyRunner.stop();
        log().debug("Останов отчетного вэбсервера.");
        WebServer.getInstance().stopWebServer();
        log().debug("Выключение центрального табло.");
        MainBoard.getInstance().close();

        deleteTempFile();
        Thread.sleep(1500);
        log().info("Сервер штатно завершил работу. Время работы: " + Uses.roundAs(((double) (System.currentTimeMillis() - start)) / 1000 / 60, 2) + " мин.");
        System.exit(0);
    }

    private static void printLicense() {
        if (Locales.getInstance().isRuss()) {

            ln("Добро пожаловать на сервер QSystem. Для работы необходим MySQL5.6 или выше.");
            ln("Версия сервера: " + FAbout.getQversion() + "_" + FAbout.getQbuild() + "-community QSystem Server (GPL)");
            ln("Версия базы данных: " + FAbout.getQversionDb() + " for MySQL 5.6-community Server (GPL)");
            ln("Дата выпуска : " + FAbout.getQdate());
            ln("Copyright (c) 2018, Apertum Projects. Все права защищены.");
            ln("QSystem является свободным программным обеспечением, вы можете");
            ln("распространять и/или изменять его согласно условиям Стандартной Общественной");
            ln("Лицензии GNU (GNU GPL), опубликованной Фондом свободного программного");
            ln("обеспечения (FSF), либо Лицензии версии 3, либо более поздней версии.");

            ln("Вы должны были получить копию Стандартной Общественной Лицензии GNU вместе");
            ln("с этой программой. Если это не так, напишите в Фонд Свободного ПО ");
            ln("(Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA)");

            ln("Набирите 'exit' чтобы штатно остановить работу сервера.");
            ln(null);
        } else {
            ln("Server version: " + FAbout.getQversion() + "_" + FAbout.getQbuild() + "-community QSystem Server (GPL)");
            ln("Database version: " + FAbout.getQversionDb() + " for MySQL 5.6-community Server (GPL)");
            ln("Released : " + FAbout.getQdate());

            ln("Copyright (c) 2010-2018, Apertum Projects and/or its affiliates. All rights reserved.");
            ln("This software comes with ABSOLUTELY NO WARRANTY. This is free software,");
            ln("and you are welcome to modify and redistribute it under the GPL v3 license");
            ln("Text of this license on your language located in the folder with the program.");

            ln("Type 'exit' to stop work and close server.");
            ln(null);
        }
    }

    private static void startJetty() {
        // посмотрим не нужно ли стартануть jetty
        // для этого нужно запускать с ключом http
        // если етсь ключ http, то запускаем сервер и принимаем на нем команды серверу суо
        if (QConfig.cfg().getHttp() > 0) {
            log().info("Run Jetty.");
            try {
                JettyRunner.start(QConfig.cfg().getHttp());
            } catch (NumberFormatException ex) {
                log().error("Номер порта для Jetty в параметрах запуска не является числом. Формат параметра для порта 8081 '-http 8081'.", ex);
            }
        }
    }

    private static void startCommonTimers() {
        final SimpleDateFormat formatHhMm = new SimpleDateFormat("HH:mm");
        final SimpleDateFormat formatDdMmYyyy = new SimpleDateFormat("dd.MM.yyyy");
        if (!(formatHhMm.format(ServerProps.getInstance().getProps().getStartTime()).equals(formatHhMm.format(ServerProps.getInstance().getProps().getFinishTime())))) {
            /**
             * Таймер, по которому будем Очистка всех услуг и рассылка спама с дневным отчетом.
             */
            ATalkingClock clearServices = new ATalkingClock(Uses.DELAY_CHECK_TO_LOCK, 0) {

                @Override
                public void run() {
                    final String HH_MM = formatHhMm.format(new Date());
                    // это обнуление
                    if (!QConfig.cfg().isRetain() && formatHhMm.format(new Date(new Date().getTime() + 10 * 60 * 1000))
                            .equals(formatHhMm.format(ServerProps.getInstance().getProps().getStartTime()))) {
                        log().info("Очистка всех услуг.");
                        // почистим все услуги от трупов кастомеров с прошлого дня
                        QServer.clearAllQueue();
                    }

                    // это рассылка дневного отчета
                    final String p1 = ServerProps.getInstance().getProperty(QProperties.SECTION_REPORTS, "day_report_enable", "0");
                    final String p2 = Mailer.fetchConfig().getProperty("mailing");
                    final boolean doReport = "1".equals(p1) || "true".equalsIgnoreCase(p1) || "true".equalsIgnoreCase(p2) || "1".equals(p2);
                    if (doReport) {
                        final QProperty prop = ServerProps.getInstance().getProperty(QProperties.SECTION_REPORTS, "day_report_time_HH:mm");
                        final boolean onTime = prop == null
                                ? formatHhMm.format(new Date(new Date().getTime() - 30 * 60 * 1000)).equals(formatHhMm.format(ServerProps.getInstance().getProps().getFinishTime()))
                                : HH_MM.equals(prop.getValue());
                        if (onTime) {
                            final String report = ServerProps.getInstance().getProperty(QProperties.SECTION_REPORTS, "day_report_link", "distribution_job_day.pdf");
                            log().info("Day report. Рассылка дневного отчета. " + report);
                            // почистим все услуги от трупов кастомеров с прошлого дня
                            for (QUser user : QUserList.getInstance().getItems()) {
                                if (user.getReportAccess()) {
                                    final HashMap<String, String> p = new HashMap<>();
                                    p.put("date", formatDdMmYyyy.format(new Date()));
                                    final byte[] result = QReportsList.getInstance().generate(user, "/" + report, p);
                                    try {
                                        try (FileOutputStream fos = new FileOutputStream("temp/" + report)) {
                                            fos.write(result);
                                            fos.flush();
                                        }
                                        Mailer.sendReporterMailAtFon(null, null, null, "temp/" + report);
                                    } catch (Exception ex) {
                                        log().error("Какой-то облом с дневным отчетом", ex);
                                    }
                                    break;
                                }
                            }
                        }
                    }

                    // это скачивание и раззиповка архива раз в день.
                    QProperty prop = ServerProps.getInstance().getProperty(QProperties.SECTION_UPDATER, "download_time_HH:mm");
                    if (prop != null && HH_MM.equals(prop.getValue())) {
                        Updater.load().download(true);
                    }
                    prop = ServerProps.getInstance().getProperty(QProperties.SECTION_UPDATER, "unzip_time_HH:mm");
                    if (prop != null && HH_MM.equals(prop.getValue())) {
                        Updater.load().unzip();
                    }
                }
            };
            clearServices.start();
        }
    }

    /**
     * <h3>Привинтить сокет и слушать его.</h3>
     * Создадим сокет, начнем его слушать, принимать команды и передавать на исполнение.
     */
    public static void makeSocketAndRun(int port) {
        // привинтить сокет на порт
        try (final ServerSocket server = new ServerSocket(port)) {
            log().info("Сервер системы захватывает порт \"" + port + "\".");
            server.setSoTimeout(500);
            final ChangeServerAction as = new ChangeServerAction();
            ln("Server QSystem started.\n");
            log().info("Сервер системы 'Очередь' запущен. DB name='" + as.getTitle() + "' url=" + as.getUrl());
            int pos = 0;
            final long start = System.currentTimeMillis();
            boolean exit = false;
            // слушаем порт
            while (!globalExit && !exit) {
                // ждём нового подключения, после чего запускаем обработку клиента
                // в новый вычислительный поток и увеличиваем счётчик на единичку
                makeServerAndHandleSocket(server);
                if (!QConfig.cfg().isDebug()) {
                    lo((pos % 2 == 0 ? "<-" : "->") + " Process: ***************".substring(0, 11 + pos) + "               ".substring(0, 14 - pos)
                            + " EXECUTING [" + Uses.msecToStr(System.currentTimeMillis() - start) + "]");//NOSONAR
                    System.out.write(13);//NOSONAR '\b' - возвращает корретку на одну позицию назад
                    if (++pos == 13) {
                        pos = 0;
                    }
                }

                exit = chechExit();
            }// while

            log().debug("Закрываем серверный сокет.");
        } catch (IOException e) {
            throw new ServerException("Network error. Creating net socket is not possible: " + e);
        } catch (Exception e) {
            throw new ServerException("Network error: " + e);
        }
    }

    private static boolean chechExit() throws IOException {
        // Попробуем считать нажатую клавишу
        // если нажади ENTER, то завершаем работу сервера
        // и затираем файл временного состояния Uses.TEMP_STATE_FILE
        int bytesAvailable = System.in.available();
        if (bytesAvailable > 0) {
            byte[] data = new byte[bytesAvailable];
            final int readed = System.in.read(data);
            if (bytesAvailable == 5 && readed > 4
                    && data[0] == 101
                    && data[1] == 120
                    && data[2] == 105
                    && data[3] == 116
                    && ((data[4] == 10) || (data[4] == 13))) {
                // набрали команду "exit" и нажали ENTER
                log().info("Завершение работы сервера.");
                return true;
            }
        }
        return false;
    }

    private static volatile boolean globalExit = false;

    /**
     * Сосдаем сервачек наш.
     *
     * @param socket привинтили сеть на него.
     */
    private QServer(Socket socket) {
        this.socket = socket;
    }

    /**
     * Присосемся к сокету, подождем, если пришло, то отправим пришедшее на обработку.
     *
     * @param server К сокету, к которому присосемся.
     */
    private static void makeServerAndHandleSocket(ServerSocket server) {
        try {
            final QServer qServer = new QServer(server.accept());
            EXECUTOR_SERVICE.execute(qServer);
            if (QConfig.cfg().isDebug()) {
                ln(null);
            }
        } catch (SocketTimeoutException e) {
            // ничего страшного, гасим исключение стобы дать возможность отработать входному/выходному потоку
        } catch (Exception e) {
            throw new ServerException("Network error: " + e);
        }
    }


    /**
     * Из сокета клиента берём поток входящих данных.
     *
     * @param socket В нем что-то есть пришедшее из сети.
     * @return Строка, пришедшая по сети.
     */
    private String readInputStrim(Socket socket) {
        try {
            final InputStream is = socket.getInputStream();
            // подождать пока хоть что-то приползет из сети, но не более 10 сек.
            int i = 0;
            while (is.available() == 0 && i < 100) {
                Thread.sleep(100);//бля
                i++;
            }

            final StringBuilder sb = new StringBuilder(new String(Uses.readInputStream(is), StandardCharsets.UTF_8));
            while (is.available() != 0) {
                sb.append(new String(Uses.readInputStream(is), StandardCharsets.UTF_8));
                Thread.sleep(150);//бля
            }
            return URLDecoder.decode(sb.toString(), "utf-8");
        } catch (IOException ex) {
            throw new ServerException("Ошибка при чтении из входного потока: " + ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServerException("Проблема со сном: " + ex);
        } catch (IllegalArgumentException ex) {
            throw new ServerException("Ошибка декодирования сетевого сообщения: " + ex);
        }
    }

    private String doTaskAndSerializeAnswer(String data) {
        final JsonRPC20 rpc;
        final Gson gson = GsonPool.getInstance().borrowGson();
        try {
            rpc = gson.fromJson(data, JsonRPC20.class);
            // полученное задание передаем в пул
            final Object result = Executer.getInstance().doTask(rpc, socket.getInetAddress().getHostAddress(), socket.getInetAddress().getAddress());
            return gson.toJson(result);
        } catch (JsonSyntaxException ex) {
            log().error("Received data \"" + data + "\" has not correct JSOM format. ", ex);
            throw new ServerException("Received data \"" + data + "\" has not correct JSOM format. " + Arrays.toString(ex.getStackTrace()));
        } catch (Exception ex) {
            log().error("Late caught the error when running the command. ", ex);
            throw new ServerException("Поздно пойманная ошибка при выполнении команды: " + Arrays.toString(ex.getStackTrace()));
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
    }

    @Override
    public void run() {
        try {
            log().debug(" Start thread for receiving task. host=" + socket.getInetAddress().getHostAddress() + " ip=" + Arrays.toString(socket.getInetAddress().getAddress()));

            // из сокета клиента берём поток входящих данных
            final String data = readInputStrim(socket);
            log().trace("Task:\n" + (data.length() > 200 ? (data.substring(0, 200) + "...") : data));
            /*
             Если по сетке поймали exit, то это значит что запустили останавливающий батник.
             */
            if ("exit".equalsIgnoreCase(data)) {
                globalExit = true; // NOSONAR
                return;
            }
            final String answer = doTaskAndSerializeAnswer(data);
            // выводим данные:
            log().trace("Response:\n" + (answer.length() > 200 ? (answer.substring(0, 200) + "...") : answer));
            // Передача данных ответа
            try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
                writer.print(URLEncoder.encode(answer, "utf-8"));
                writer.flush();
            }
        } catch (Exception ex) {
            throw new ServerException("Ошибка при выполнении задания.", ex);
        } finally {
            // завершаем соединение
            try {
                //оборачиваем close, т.к. он сам может сгенерировать ошибку IOExeption. Просто выкинем Стек-трейс
                socket.close();
            } catch (IOException e) {
                log().trace(e);
            }
            log().trace("Response was finished");
        }
    }

    /**
     * Сохранение состояния пула услуг в xml-файл на диск.
     */
    public static synchronized void savePool() {
        final long start = System.currentTimeMillis();
        log().info("Save the state.");
        final LinkedList<QCustomer> backup = new LinkedList<>();// создаем список сохраняемых кастомеров
        final LinkedList<QCustomer> parallelBackup = new LinkedList<>();// создаем список сохраняемых Parallel кастомеров
        final LinkedList<Long> pauses = new LinkedList<>();// создаем список юзеров у которых менопауза
        QServiceTree.getInstance().getNodes().stream().forEach(service -> backup.addAll(service.getClients()));

        QUserList.getInstance().getItems().forEach(user -> {
            if (user.getCustomer() != null) {
                backup.add(user.getCustomer());
            }
            parallelBackup.addAll(user.getParallelCustomers().values());
            if (user.isPause()) {
                pauses.add(user.getId());
            }
        });
        // в темповый файл
        new File(Uses.TEMP_FOLDER).mkdir();
        final Gson gson = GsonPool.getInstance().borrowGson();
        try (final FileOutputStream fos = new FileOutputStream(new File(Uses.TEMP_FOLDER + File.separator + Uses.TEMP_STATE_FILE))) {
            fos.write(gson.toJson(new TempList(backup, parallelBackup, QPostponedList.getInstance().getPostponedCustomers(), pauses)).getBytes(StandardCharsets.UTF_8));
            fos.flush();
        } catch (FileNotFoundException ex) {
            throw new ServerException("Не возможно создать временный файл состояния. " + ex.getMessage());
        } catch (IOException ex) {
            throw new ServerException("Не возможно сохранить изменения в поток." + ex.getMessage());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        log().info("Состояние сохранено. Затрачено времени: " + ((double) (System.currentTimeMillis() - start)) / 1000 + " сек.");
    }

    /**
     * Временный список всяких там.
     */
    @SuppressWarnings("squid:S1319")
    public static class TempList {

        public TempList() {
        }

        /**
         * Временный список всяких там.
         */
        public TempList(LinkedList<QCustomer> backup, LinkedList<QCustomer> parallelBackup, LinkedList<QCustomer> postponed) {
            this.backup = backup;
            this.parallelBackup = parallelBackup;
            this.postponed = postponed;
        }

        /**
         * Временный список всяких там.
         */
        public TempList(LinkedList<QCustomer> backup, LinkedList<QCustomer> parallelBackup, LinkedList<QCustomer> postponed, LinkedList<Long> pauses) {
            this.backup = backup;
            this.parallelBackup = parallelBackup;
            this.postponed = postponed;
            this.pauses = pauses;
        }

        @Expose
        @SerializedName("backup")
        public LinkedList<QCustomer> backup;
        @Expose
        @SerializedName("parallelBackup")
        public LinkedList<QCustomer> parallelBackup;
        @Expose
        @SerializedName("postponed")
        public LinkedList<QCustomer> postponed;
        @Expose
        @SerializedName("method")
        public String method = null;
        @Expose
        @SerializedName("pauses")
        public LinkedList<Long> pauses = null;
        @Expose
        @SerializedName("date")
        public Long date = new Date().getTime();

        @Expose
        @SerializedName("tempUserData")
        public TempUserData tempUserData;

        public static class TempUserData {

            /**
             * Временный пользователь.
             */
            public TempUserData(Long userId, Long seviceId, Long customerId, String userPoint) {
                this.userId = userId;
                this.seviceId = seviceId;
                this.customerId = customerId;
                this.userPoint = userPoint;
            }

            public TempUserData() {
            }

            @Expose
            @SerializedName("userId")
            public Long userId;
            @Expose
            @SerializedName("serviceId")
            public Long seviceId;
            @Expose
            @SerializedName("customerId")
            public Long customerId;
            @Expose
            @SerializedName("userPoint")
            public String userPoint = null;
        }
    }

    /**
     * Загрузка состояния пула услуг из временного json-файла.
     */
    public static void loadPool() {
        final long start = System.currentTimeMillis();
        // если есть временный файлик сохранения состояния, то надо его загрузить.
        // все ошибки чтения и парсинга игнорить.
        log().info("Пробуем восстановить состояние системы.");
        File recovFile = new File(Uses.TEMP_FOLDER + File.separator + Uses.TEMP_STATE_FILE);
        if (recovFile.exists()) {
            log().warn(Locales.locMes("came_back"));
            //восстанавливаем состояние

            final StringBuilder recData = new StringBuilder();
            try (final FileInputStream fis = new FileInputStream(recovFile)) {
                final Scanner scan = new Scanner(fis, "utf8");
                while (scan.hasNextLine()) {
                    recData.append(scan.nextLine());
                }
            } catch (IOException ex) {
                throw new ServerException(ex);
            }

            final TempList recList;
            final Gson gson = GsonPool.getInstance().borrowGson();
            try {
                recList = gson.fromJson(recData.toString(), TempList.class);
            } catch (JsonSyntaxException ex) {
                throw new ServerException("Не возможно интерпритировать сохраненные данные.\n" + ex.toString());
            } finally {
                GsonPool.getInstance().returnGson(gson);
                recData.setLength(0);
            }

            // Проверим не просрочился ли кеш. Время просточки 3 часа.
            if (!QConfig.cfg().isRetain() && (recList.date == null || new Date().getTime() - recList.date > 3 * 60 * 60 * 1000)) {
                // Просрочился кеш, не грузим
                log().warn("Срок давности хранения состояния истек. Если в системе ничего не происходит 3 часа, то считается что сохраненные данные устарели безвозвратно.");
            } else {
                // Свежий, загружаем в сервер данные кеша

                try {
                    QPostponedList.getInstance().loadPostponedList(recList.postponed);
                    for (QCustomer recCustomer : recList.backup) {
                        // в эту очередь он был
                        final QService service = QServiceTree.getInstance().getById(recCustomer.getService().getId());
                        if (service == null) {
                            log().warn("Попытка  добавить клиента \"" + recCustomer.getPrefix() + recCustomer.getNumber()
                                    + "\" к  услуге \"" + recCustomer.getService().getName() + "\" не успешна. Услуга не обнаружена!");
                        } else {
                            service.setCountPerDay(recCustomer.getService().getCountPerDay());
                            service.setDay(recCustomer.getService().getDay());
                            // так зовут юзера его обрабатываюшего
                            final QUser user = recCustomer.getUser();
                            // кастомер ща стоит к этой услуге к какой стоит
                            recCustomer.setService(service);
                            // смотрим к чему привязан кастомер. либо в очереди стоит, либо у юзера обрабатыватся
                            if (user == null) {
                                // сохраненный кастомер стоял в очереди и ждал, но его еще никто не звал
                                QServiceTree.getInstance().getById(recCustomer.getService().getId()).addCustomerForRecoveryOnly(recCustomer);
                                log().debug("Добавили  клиента \"" + recCustomer.getPrefix() + recCustomer.getNumber()
                                        + "\" к услуге \"" + recCustomer.getService().getName() + "\"");
                            } else {
                                // сохраненный кастомер обрабатывался юзером с именем userId
                                if (QUserList.getInstance().getById(user.getId()) == null) {
                                    log().warn("Попытка добавить клиента \"" + recCustomer.getPrefix() + recCustomer.getNumber()
                                            + "\" к  юзеру \"" + user.getName() + "\" не успешна. Юзер не обнаружен!");
                                } else {
                                    QUserList.getInstance().getById(user.getId()).setCustomer(recCustomer);
                                    recCustomer.setUser(QUserList.getInstance().getById(user.getId()));
                                    log().debug("Добавили клиента \"" + recCustomer.getPrefix() + recCustomer.getNumber() + "\" к юзеру  \"" + user.getName() + "\"");
                                }
                            }
                        }
                    }
                    // Параллельные кастомеры, загрузим
                    for (QCustomer recCustomer : recList.parallelBackup) {
                        // в эту очередь он был
                        final QService service = QServiceTree.getInstance().getById(recCustomer.getService().getId());
                        if (service == null) {
                            log().warn("Попытка добавить клиента \"" + recCustomer.getPrefix() + recCustomer.getNumber()
                                    + "\" к услуге \"" + recCustomer.getService().getName() + "\" не успешна. Услуга не обнаружена!");
                        } else {
                            service.setCountPerDay(recCustomer.getService().getCountPerDay());
                            service.setDay(recCustomer.getService().getDay());
                            // так зовут юзера его обрабатываюшего
                            final QUser user = recCustomer.getUser();
                            // кастомер ща стоит к этой услуге к какой стоит
                            recCustomer.setService(service);
                            // смотрим к чему привязан кастомер. либо в очереди стоит, либо у юзера обрабатыватся
                            if (user == null) {
                                log().warn("Для параллельного клиента \"" + recCustomer.getPrefix() + recCustomer.getNumber()
                                        + "\" добавление к юзеру не успешна. Юзер потерялся!");
                            } else {
                                // сохраненный кастомер обрабатывался юзером с именем userId
                                if (QUserList.getInstance().getById(user.getId()) == null) {
                                    log().warn("Попытка добавить параллельного клиента \"" + recCustomer.getPrefix() + recCustomer.getNumber()
                                            + "\" к юзеру \"" + user.getName() + "\" не успешна. Юзер не обнаружен!");
                                } else {
                                    QUserList.getInstance().getById(user.getId()).setCustomer(recCustomer);
                                    recCustomer.setUser(QUserList.getInstance().getById(user.getId()));
                                    log().debug("Добавили клиента \"" + recCustomer.getPrefix() + recCustomer.getNumber() + "\" к юзеру \"" + user.getName() + "\"");
                                }
                            }
                        }
                    }
                    recList.pauses.stream().map(idUser -> QUserList.getInstance().getById(idUser)).filter(Objects::nonNull).forEachOrdered(user -> user.setPause(Boolean.TRUE));
                } catch (ServerException ex) {
                    clearAllQueue();
                    log().error("Востановление состояния сервера после изменения конфигурации. Для выключения сервера используйте команду exit. ", ex);
                }
            }
        }
        log().info("Восстановление состояния системы завершено. Затрачено времени: " + ((double) (System.currentTimeMillis() - start)) / 1000 + " сек.");
    }

    /**
     * Всех зомбаков выгоним из очередей.
     */
    public static void clearAllQueue() {
        // почистим все услуги от трупов кастомеров
        QServiceTree.getInstance().getNodes().forEach(service -> {
            service.clearNextNumber();
            service.freeCustomers();
        });
        QService.clearNextStNumber();

        QPostponedList.getInstance().clear();
        MainBoard.getInstance().clear();

        // Сотрем временные файлы
        try {
            deleteTempFile();
            log().info("Очистка всех пользователей от привязанных кастомеров.");
        } catch (IOException e) {
            log().error("Очистка всех пользователей не удалила файлы.", e);
        }
        QUserList.getInstance().getItems().forEach(user -> {
            user.setCustomer(null);
            user.getParallelCustomers().clear();
            user.setShadow(null);
            user.getPlanServices().forEach(plan -> {
                plan.setAvgWait(0);
                plan.setAvgWork(0);
                plan.setKilled(0);
                plan.setWorked(0);
            });
        });
    }

    /**
     * Сотрем временный файл состояния.
     */
    public static void deleteTempFile() throws IOException {
        log().debug("Remove " + Uses.TEMP_FOLDER + File.separator + Uses.TEMP_STATE_FILE);
        File file = new File(Uses.TEMP_FOLDER + File.separator + Uses.TEMP_STATE_FILE);
        if (file.exists()) {
            Files.delete(file.toPath());
        }
        log().debug("Remove " + Uses.TEMP_FOLDER + File.separator + Uses.TEMP_STATATISTIC_FILE);
        file = new File(Uses.TEMP_FOLDER + File.separator + Uses.TEMP_STATATISTIC_FILE);
        if (file.exists()) {
            Files.delete(file.toPath());
        }
    }
}
