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
package ru.apertum.qsystem.client.forms;

import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import gnu.io.SerialPortEvent;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeException;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.output.OutputException;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import ru.apertum.qsystem.QSystem;
import ru.apertum.qsystem.client.Locales;
import ru.apertum.qsystem.client.QProperties;
import ru.apertum.qsystem.client.common.BackDoor;
import ru.apertum.qsystem.client.common.ClientNetProperty;
import ru.apertum.qsystem.client.common.WelcomeBGparams;
import ru.apertum.qsystem.client.common.WelcomeParams;
import ru.apertum.qsystem.client.model.QButton;
import ru.apertum.qsystem.client.model.QPanel;
import ru.apertum.qsystem.common.BrowserFX;
import ru.apertum.qsystem.common.GsonPool;
import ru.apertum.qsystem.common.Mailer;
import ru.apertum.qsystem.common.NetCommander;
import ru.apertum.qsystem.common.QConfig;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.QModule;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.cmd.CmdParams;
import ru.apertum.qsystem.common.cmd.JsonRPC20;
import ru.apertum.qsystem.common.cmd.RpcGetAllServices;
import ru.apertum.qsystem.common.cmd.RpcGetSrt;
import ru.apertum.qsystem.common.cmd.RpcStandInService;
import ru.apertum.qsystem.common.exceptions.ClientException;
import ru.apertum.qsystem.common.exceptions.QException;
import ru.apertum.qsystem.common.exceptions.ServerException;
import ru.apertum.qsystem.common.model.ATalkingClock;
import ru.apertum.qsystem.common.model.IClientNetProperty;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.extra.IBytesButtensAdapter;
import ru.apertum.qsystem.extra.IPrintTicket;
import ru.apertum.qsystem.extra.IWelcome;
import ru.apertum.qsystem.server.model.QAdvanceCustomer;
import ru.apertum.qsystem.server.model.QAuthorizationCustomer;
import ru.apertum.qsystem.server.model.QService;
import ru.apertum.qsystem.server.model.QServiceTree;
import ru.apertum.qsystem.server.model.infosystem.QInfoItem;
import ru.apertum.qsystem.server.model.response.QRespItem;
import ru.apertum.qsystem.server.model.response.QResponseTree;
import ru.evgenic.rxtx.serialPort.IReceiveListener;
import ru.evgenic.rxtx.serialPort.ISerialPort;
import ru.evgenic.rxtx.serialPort.RxtxSerialPort;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.TreeNode;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.MemoryImageSource;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;
import java.util.ServiceLoader;

import static ru.apertum.qsystem.client.common.WelcomeBGparams.BARCODE_TEXT;
import static ru.apertum.qsystem.common.QConfig.KEY_WELCOME_KBD;

/**
 * Модуль показа окна выбора услуги для постановки в очередь. Created on 8 Сентябрь 2008 г., 16:07 Класс, который покажит форму с кнопками, соответствующими
 * услуга. При нажатии на кнопку, кастомер пытается встать в очередь.
 *
 * @author Evgeniy Egorov
 */
@SuppressWarnings({"squid:S3358", "squid:S1192", "squid:S1172", "squid:S1450", "squid:S1604", "squid:S1161", "squid:MaximumInheritanceDepth"})
public class FWelcome extends javax.swing.JFrame {

    private static final ResourceMap LOCALE_MAP = Application.getInstance(QSystem.class).getContext().getResourceMap(FWelcome.class);

    public static String msg(String key) {
        return LOCALE_MAP.getString(key);
    }

    /**
     * Формат даты.
     */
    public final SimpleDateFormat formatHhMm = new SimpleDateFormat("HH:mm");

    // Состояния пункта регистрации
    private static final String LOCK = msg("lock");
    private static final String UNLOCK = msg("unlock");
    private static final String OFF = msg("off");
    public static final String LOCK_MESSAGE = "<HTML><p align=center><b><span style='font-size:40.0pt;color:red'>" + msg("messages.lock_messages") + "</span></b></p>";
    private static final String STOP_MESSAGE = "<HTML><p align=center><b><span style='font-size:40.0pt;color:red'>" + msg("messages.stop_messages")
            + "</span></b></p>";
    private static QService root;
    private int pageNumber = 0;// на одном уровне может понадобиться листать услуги, не то они расползуться. Это вместо скрола.
    /**
     * XML-список отзывов. перврначально null, грузится при первом обращении. Использовать через геттер.
     */
    private static QRespItem response = null;

    /**
     * Получаем список отзывов.
     *
     * @return корень дерева отзывов.
     */
    public static QRespItem getResponse() {
        if (response == null) {
            response = NetCommander.getResporseList(netProperty);
            QResponseTree.formTree(response);
        }
        return response;
    }

    /**
     * XML- дерево информации. перврначально null, грузится при первом обращении. Использовать через геттер.
     */
    private static QInfoItem infoTree = null;

    /**
     * Получаем инфодерево от сервера.
     *
     * @return корень инфодерева.
     */
    public static QInfoItem getInfoTree() {
        if (infoTree == null) {
            infoTree = NetCommander.getInfoTree(netProperty);
        }
        return infoTree;
    }

    protected static QService current;
    /**
     * это печатаем под картинкой если без домена.
     */
    private static String caption;

    public static void setCaption(String caption) {
        FWelcome.caption = caption;
    }

    /**
     * время блокировки/разблокировки пункта регистрации.
     */
    protected static Date startTime;
    protected static Date finishTime;
    protected static boolean btnFreeDesign;
    /**
     * Информация для взаимодействия по сети. Формируется по данным из командной строки.
     */
    private static IClientNetProperty netProperty;

    public static IClientNetProperty getNetProperty() {
        return netProperty;
    }

    /**
     * Режим предварительной записи в поликлинике.
     */
    private static boolean isMed = false;
    /**
     * Режим инфокиоска, когда получить всю инфу с пункта регистрации можно, а встать в очередь нельзя.
     */
    private static boolean isInfo = false;

    public static boolean isInfo() {
        return isInfo;
    }

    //******************************************************************************************************************
    //******************************************************************************************************************
    //*****************************************Сервер удаленного управления ********************************************
    /**
     * Сервер удаленного управления.
     */
    private final transient Thread server = new Thread(new CommandServer());
    /**
     * Флаг завершения сервера удаленного управления.
     */
    boolean exitServer = false;

    private class CommandServer implements Runnable {

        @Override
        public void run() {
            // привинтить сокет на локалхост, порт 3129
            final ServerSocket serverSocket;
            try {
                serverSocket = new ServerSocket(netProperty.getClientPort());
            } catch (IOException e) {
                throw new ClientException("Ошибка при создании серверного сокета: " + e);
            }

            try {
                serverSocket.setSoTimeout(500);

                System.out.println("Server for managment of registration point started.\n");
                QLog.l().logger().info("Сервер управления пунктом регистрации запущен.");

                // слушаем порт
                while (!exitServer) {
                    // ждём нового подключения, после чего запускаем обработку клиента
                    // в новый вычислительный поток и увеличиваем счётчик на единичку
                    try (final Socket socket = serverSocket.accept()) {
                        doCommand(socket);
                    } catch (IOException | ServerException e) {
                        // press hard
                    }
                }
            } catch (IOException e) {
                throw new ClientException("Ошибка при конфигурировании серверного сокета: " + e);
            } finally {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    QLog.l().logger().error(e);
                }
            }
        }

        private void doCommand(Socket socket) {
            // из сокета клиента берём поток входящих данных
            final String data;
            try {
                final InputStream is = socket.getInputStream();
                // подождать пока хоть что-то приползет из сети, но не более 10 сек.
                int i = 0;
                while (is.available() == 0 && i < 100) {
                    Thread.sleep(100);//бля
                    i++;
                }
                Thread.sleep(100);//бля
                data = URLDecoder.decode(new String(Uses.readInputStream(is)).trim(), "utf-8");
            } catch (IOException ex) {
                throw new ServerException("Ошибка при чтении из входного потока: " + Arrays.toString(ex.getStackTrace()));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new ServerException("Проблема со сном: " + Arrays.toString(ex.getStackTrace()));
            }
            QLog.l().logger().trace("Задание:\n" + data);

            final JsonRPC20 rpc;
            final Gson gson = GsonPool.getInstance().borrowGson();
            try {
                rpc = gson.fromJson(data, JsonRPC20.class);
            } finally {
                GsonPool.getInstance().returnGson(gson);
            }

            // Обрабатываем задание
            //С рабочего места администратора должна быть возможность заблокировать пункт постановки в очередь,
            //разблокировать, выключить, провести инициализация заново.
            // В любом другом случае будет выслано состояние.
            if (Uses.WELCOME_LOCK.equals(rpc.getMethod())) {
                lockWelcome(LOCK_MESSAGE);
            }
            if (Uses.WELCOME_UNLOCK.equals(rpc.getMethod())) {
                unlockWelcome();
            }
            if (Uses.WELCOME_OFF.equals(rpc.getMethod())) {
                offWelcome();
            }
            if (Uses.WELCOME_REINIT.equals(rpc.getMethod())) {
                reinit(rpc.getParams());
            }

            String upp = ".  " + increaseTicketCount(0) + " " + msg("tickets_were_printed");
            // выводим данные:
            QLog.l().logger().trace("Ответ: " + stateWindow + upp);
            final String rpc_resp;
            final Gson gsonResp = GsonPool.getInstance().borrowGson();
            try {
                rpc_resp = gson.toJson(new RpcGetSrt(stateWindow + upp));
            } finally {
                GsonPool.getInstance().returnGson(gsonResp);
            }
            try (final PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
                // Передача данных ответа
                writer.print(rpc_resp);
                writer.flush();
            } catch (IOException e) {
                throw new ServerException("Ошибка при записи в поток: " + Arrays.toString(e.getStackTrace()));
            }

            //Если команда была "выключить"
            if (OFF.equals(stateWindow)) {
                System.exit(0);
            }
        }
    }

    //*****************************************Сервер удаленного управления ********************************************
    /**
     * Таймер, по которому будем выходить в корень меню.
     */
    private transient ATalkingClock clockBack = new ATalkingClock(WelcomeParams.getInstance().delayBack, 1) {

        @Override
        public void run() {
            setAdvanceRegim(false);
            showMed();
        }
    };

    public ATalkingClock getClockBack() {
        return clockBack;
    }

    /**
     * Таймер, по которому будем разблокировать и выходить в корень меню.
     */
    private transient ATalkingClock clockUnlockBack = new ATalkingClock(WelcomeParams.getInstance().delayPrint, 1) {

        @Override
        public void run() {
            unlockWelcome();
            setAdvanceRegim(false);
            buttonToBeginActionPerformed(null);
        }
    };

    public ATalkingClock getClockUnlockBack() {
        return clockUnlockBack;
    }

    private static ISerialPort serialPort;

    /**
     * Приложение киоска.
     */
    public static void main(final String[] args) throws Exception {
        QLog.initial(args, QModule.welcome);
        // Загрузка плагинов из папки plugins
        Uses.loadPlugins("./plugins/");
        Locale.setDefault(Locales.getInstance().getLangCurrent());

        // выберем нужные языки на велкоме если первый раз запускаем или ключ -clangs
        if ((Locales.getInstance().isWelcomeMultylangs() && Locales.getInstance().isWelcomeFirstLaunch()) || QConfig.cfg().isChangeLangs()) {

            JFrame form = new FLangsOnWelcome();
            java.awt.EventQueue.invokeLater(() ->
                    form.setVisible(true)
            );
            Thread.sleep(2000);
            while (form.isVisible()) {
                Thread.sleep(1000);
            }
        }

        netProperty = new ClientNetProperty(args);
        //Загрузим серверные параметры
        QProperties.get().load(netProperty);
        // определим режим пользовательского интерфейса
        for (String arg : args) {
            if ("med".equals(arg)) {
                isMed = true;
                if (!"".equals(WelcomeParams.getInstance().buttonsCOM)) {
                    serialPort = new RxtxSerialPort(WelcomeParams.getInstance().buttonsCOM);
                    serialPort.setDataBits(WelcomeParams.getInstance().buttonsDatabits);
                    serialPort.setParity(WelcomeParams.getInstance().buttonsParity);
                    serialPort.setSpeed(WelcomeParams.getInstance().buttonsSpeed);
                    serialPort.setStopBits(WelcomeParams.getInstance().buttonsStopbits);
                }
            }
            if ("info".equals(arg)) {
                isInfo = true;
            }
        }
        final RpcGetAllServices.ServicesForWelcome servs;
        try {
            servs = NetCommander.getServices(netProperty);
        } catch (Exception t) {
            QLog.l().logger().error("Start Welcome was failed.", t);
            System.exit(117);
            throw new ServerException(t);
        }
        root = servs.getRoot();
        FWelcome.startTime = servs.getStartTime();
        FWelcome.finishTime = servs.getFinishTime();
        FWelcome.btnFreeDesign = servs.getButtonFreeDesign();

        for (final IWelcome event : ServiceLoader.load(IWelcome.class)) {
            QLog.l().logger().info("Вызов SPI расширения. Описание: " + event.getDescription());
            try {
                event.start(netProperty, servs);
            } catch (Exception tr) {
                QLog.l().logger().error("Вызов SPI расширения завершился ошибкой. Описание: " + tr);
            }
        }

        //touch,info,med,btn,kbd
        switch (QConfig.cfg().getWelcomeMode()) {
            case KEY_WELCOME_KBD: {
                // ***************************************************************************************************************************************
                // ***  Это клавиатурный ввод символа
                // ***************************************************************************************************************************************
                QLog.l().logger().info("Keyboard mode is starting...");

                final GregorianCalendar gc = new GregorianCalendar();
                gc.setTime(startTime);
                final long stime = gc.get(GregorianCalendar.HOUR_OF_DAY) * 60L + gc.get(GregorianCalendar.MINUTE);
                gc.setTime(finishTime);
                final long ftime = gc.get(GregorianCalendar.HOUR_OF_DAY) * 60L + gc.get(GregorianCalendar.MINUTE);

                final HashMap<String, QService> addrs = new HashMap<>();
                final File addrFile = new File("config/welcome_buttons.properties");
                try (FileInputStream fis = new FileInputStream(addrFile);
                     Scanner s = new Scanner(fis)) {
                    while (s.hasNextLine()) {
                        final String line = s.nextLine().trim();
                        if (!line.startsWith("#")) {
                            final String[] ss = line.split("=");
                            QServiceTree.sailToStorm(root, (TreeNode service) -> {
                                if (((QService) service).getId().equals(Long.valueOf(ss[1]))) {
                                    QLog.l().logger().debug("Key " + ss[0] + " = " + ss[1] + " " + ((QService) service).getName());
                                    addrs.put(ss[0], (QService) service);
                                }
                            });
                        }
                    }
                } catch (IOException ex) {
                    throw new ServerException(ex);
                }

                final JFrame fr = new JFrame("Keyboard input");

                // спрячем курсор мыши
                if (QConfig.cfg().isHideCursor()) {
                    final int[] pixels = new int[16 * 16];
                    final Image image = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16, 16, pixels, 0, 16));
                    Cursor transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0, 0), "invisibleCursor");
                    fr.setCursor(transparentCursor);
                }

                fr.setUndecorated(true);
                fr.setVisible(true);
                fr.setAlwaysOnTop(false);
                fr.setAlwaysOnTop(true);
                fr.setVisible(true);
                fr.toFront();
                fr.requestFocus();
                fr.setForeground(Color.red);
                fr.setBackground(Color.red);
                fr.setOpacity(0.1f);
                final Robot r = new Robot();
                fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                fr.setSize(5, 5);
                fr.addKeyListener(new KeyListener() {

                    long time = 0;

                    @Override
                    public void keyTyped(KeyEvent e) {
                        // not for using.
                    }

                    @Override
                    public void keyPressed(KeyEvent e) {

                        if (System.currentTimeMillis() - time < 5000) {
                            return;
                        }
                        time = System.currentTimeMillis();
                        final GregorianCalendar gc = new GregorianCalendar();
                        final long now = gc.get(GregorianCalendar.HOUR_OF_DAY) * 60L + gc.get(GregorianCalendar.MINUTE);
                        if (now > stime && now < ftime) {
                            final QService serv = addrs.get("" + e.getKeyChar());
                            if (serv == null) {
                                QLog.l().logger().error("Service is not found by " + e.getKeyChar());
                                return;
                            }
                            final QCustomer customer;
                            try {
                                customer = NetCommander.standInService(netProperty, serv.getId(), "1", 1, "");
                            } catch (Exception ex) {
                                QLog.l().logger().error("Fail to put in line " + serv.getName() + "  ID=" + serv.getId(), ex);
                                return;
                            }
                            FWelcome.printTicket(customer, root.getTextToLocale(QService.Field.NAME));
                        } else {
                            QLog.l().logger().warn("Client is out of time: " + new Date());
                        }
                    }

                    @Override
                    public void keyReleased(KeyEvent e) {
                        // not for using.
                    }
                });
                fr.addMouseListener(new MouseListener() {

                    long time = 0;

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        System.out.println("MouseEvent=" + e.getButton());
                        if (System.currentTimeMillis() - time < 5000) {
                            return;
                        }
                        time = System.currentTimeMillis();
                        final GregorianCalendar gc = new GregorianCalendar();
                        final long now = gc.get(GregorianCalendar.HOUR_OF_DAY) * 60L + gc.get(GregorianCalendar.MINUTE);
                        if (now > stime && now < ftime) {
                            final QService serv = addrs.get("" + e.getButton());
                            if (serv == null) {
                                QLog.l().logger().error("Service was not found by " + e.getButton());
                                return;
                            }
                            final QCustomer customer;
                            try {
                                customer = NetCommander.standInService(netProperty, serv.getId(), "1", 1, "");
                            } catch (Exception ex) {
                                QLog.l().logger().error("Fail to put in line '" + serv.getName() + "'  ID=" + serv.getId(), ex);
                                return;
                            }
                            FWelcome.printTicket(customer, root.getTextToLocale(QService.Field.NAME));
                        } else {
                            QLog.l().logger().warn("Client is out of time: " + new Date());
                        }
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        // not for using.
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        // not for using.
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        // not for using.
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        // not for using.
                    }
                });

                fr.setState(JFrame.NORMAL);
                fr.setVisible(true);
                final Timer t = new Timer(30000, (ActionEvent e) -> {
                    fr.setState(JFrame.NORMAL);
                    fr.setAlwaysOnTop(false);
                    fr.setAlwaysOnTop(true);
                    fr.setVisible(true);
                    fr.setAlwaysOnTop(true);
                    fr.toFront();
                    fr.requestFocus();
                    r.mouseMove(fr.getLocation().x + 3, fr.getLocation().y + 3);
                    r.mousePress(InputEvent.BUTTON1_MASK);
                });
                t.start();
                break;
                // ***************************************************************************************************************************************
            }
            case QConfig.KEY_WELCOME_BTN: {
                // ***************************************************************************************************************************************
                // ***  Это кнопочный терминал
                // ***************************************************************************************************************************************
                QLog.l().logger().info("Кнопочный режим пункта регистрации включен.");

                final GregorianCalendar gc = new GregorianCalendar();
                gc.setTime(startTime);
                final long stime = gc.get(GregorianCalendar.HOUR_OF_DAY) * 60L + gc.get(GregorianCalendar.MINUTE);
                gc.setTime(finishTime);
                final long ftime = gc.get(GregorianCalendar.HOUR_OF_DAY) * 60L + gc.get(GregorianCalendar.MINUTE);

                final HashMap<Byte, QService> addrs = new HashMap<>();
                final File addrFile = new File("config/welcome_buttons.properties");
                try (FileInputStream fis = new FileInputStream(addrFile);
                     Scanner s = new Scanner(fis)) {
                    while (s.hasNextLine()) {
                        final String line = s.nextLine().trim();
                        if (line != null && !line.startsWith("#") && line.contains("=")) {
                            final String[] ss = line.split("=");
                            QServiceTree.sailToStorm(root, (TreeNode service) -> {
                                if (((QService) service).getId().equals(Long.valueOf(ss[1]))) {
                                    QLog.l().logger().debug(ss[0] + " = " + ss[1] + " " + ((QService) service).getName());
                                    addrs.put(Byte.valueOf(ss[0]), (QService) service);
                                }
                            });
                        }
                    }
                } catch (IOException ex) {
                    throw new ServerException(ex);
                }

                serialPort = new RxtxSerialPort(WelcomeParams.getInstance().buttonsCOM);
                serialPort.setDataBits(WelcomeParams.getInstance().buttonsDatabits);
                serialPort.setParity(WelcomeParams.getInstance().buttonsParity);
                serialPort.setSpeed(WelcomeParams.getInstance().buttonsSpeed);
                serialPort.setStopBits(WelcomeParams.getInstance().buttonsStopbits);
                serialPort.bind(new IReceiveListener() {

                    @Override
                    public void actionPerformed(SerialPortEvent spe, byte[] bytes) {
                        final GregorianCalendar gc = new GregorianCalendar();
                        final long now = gc.get(GregorianCalendar.HOUR_OF_DAY) * 60L + gc.get(GregorianCalendar.MINUTE);
                        if (now > stime && now < ftime) {
                            // поддержка расширяемости плагинами
                            Byte flag = null;
                            for (final IBytesButtensAdapter event : ServiceLoader.load(IBytesButtensAdapter.class)) {
                                QLog.l().logger().info("Вызов SPI расширения. Описание: " + event.getDescription());
                                try {
                                    flag = event.convert(bytes);
                                } catch (Exception tr) {
                                    QLog.l().logger().error("Вызов SPI расширения завершился ошибкой. Описание: ", tr);
                                }
                                // раз конвертнули и хорошь
                                if (flag != null) {
                                    break;
                                }
                            }
                            if (flag != null || (bytes.length == 4 && bytes[0] == 0x01 && bytes[3] == 0x07)) {
                                final QService serv = addrs.get(flag != null ? flag : bytes[2]);
                                if (serv == null) {
                                    QLog.l().logger().error("Не найдена услуга по нажатию кнопки " + (flag != null ? flag : bytes[2]));
                                    return;
                                }
                                final QCustomer customer;
                                try {
                                    customer = NetCommander.standInService(netProperty, serv.getId(), "1", 1, "");
                                } catch (Exception ex) {
                                    QLog.l().logger().error("Не поставлен в очередь в " + serv.getName() + "  ID=" + serv.getId(), ex);
                                    return;
                                }
                                FWelcome.printTicket(customer, root.getTextToLocale(QService.Field.NAME));
                            } else {
                                String inputButes = "";
                                for (byte b : bytes) {
                                    inputButes = inputButes + (b & 0xFF) + "_";
                                }
                                QLog.l().logger().error("Collision! Package lenght not 4 bytes or broken: \"" + inputButes + "\"");
                            }
                        } else {
                            QLog.l().logger().warn("Не поставлен в очередь т.к. не приемные часы в " + new Date());
                        }
                    }

                    @Override
                    public void actionPerformed(SerialPortEvent spe) {
                        // not for using.
                    }
                });
                int pos = 0;
                boolean exit = false;
                // индикатор
                while (!exit) {
                    Thread.sleep(1500);
                    // ждём нового подключения, после чего запускаем обработку клиента
                    // в новый вычислительный поток и увеличиваем счётчик на единичку

                    if (!QConfig.cfg().isDebug()) {
                        System.out.print((pos % 2 == 0 ? "<-" : "->") + " Process: ***************".substring(0, 11 + pos) + "               ".substring(0, 14 - pos));//NOSONAR
                        System.out.write(13);//NOSONAR '\b' - возвращает корретку на одну позицию назад
                        if (++pos == 13) {
                            pos = 0;
                        }
                    }

                    // Попробуем считать нажатую клавишу
                    // если нажади ENTER, то завершаем работу сервера
                    // и затираем файл временного состояния Uses.TEMP_STATE_FILE
                    int bytesAvailable = System.in.available();
                    if (bytesAvailable > 0) {
                        byte[] data = new byte[bytesAvailable];
                        final int readed = System.in.read(data);
                        if (bytesAvailable == 5
                                && readed == bytesAvailable
                                && data[0] == 101
                                && data[1] == 120
                                && data[2] == 105
                                && data[3] == 116
                                && ((data[4] == 10) || (data[4] == 13))) {
                            // набрали команду "exit" и нажали ENTER
                            QLog.l().logger().info("Завершение работы сервера.");
                            exit = true;
                        }
                    }
                }// while
                serialPort.free();
                break;
                // ***************************************************************************************************************************************
            }
            default: {
                // ***************************************************************************************************************************************
                // ***  Это тачевый терминал
                // ***************************************************************************************************************************************
                java.awt.EventQueue.invokeLater(() -> {
                    final FWelcome w = new FWelcome(root);
                    w.setVisible(true);
                });
                break;
            }
        }
    }

    public FWelcome(QService root) {
        init(root);
    }

    //1024х1280.
    public static int getScreenHorizontal() {
        return WelcomeParams.getInstance().horizontal;
    }

    public static int getScreenVertical() {
        return WelcomeParams.getInstance().vertical;
    }

    private void init(QService root) {
        QLog.l().logger().info("Создаем окно приглашения.");
        setLocation(0, 0);
        if (!QConfig.cfg().isDebug()) {
            if (!QConfig.cfg().isDemo()) {
                setUndecorated(true);

                // спрячем курсор мыши
                if (QConfig.cfg().isHideCursor()) {
                    final int[] pixels = new int[16 * 16];
                    final Image image = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16, 16, pixels, 0, 16));
                    Cursor transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0, 0), "invisibleCursor");
                    setCursor(transparentCursor);
                }
            }
            addWindowListener(new WindowAdapter() {

                @Override
                public void windowOpened(WindowEvent e) {
                    setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
            });
        }

        if (QConfig.cfg().isDemo()) {
            addWindowListener(new WindowAdapter() {

                @Override
                public void windowOpened(WindowEvent e) {
                    setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
            });
        }
        initComponents();
        if (WelcomeParams.getInstance().topSize >= 0) {
            panelCaption.setPreferredSize(new Dimension(panelCaption.getWidth(), WelcomeParams.getInstance().topSize));
        }
        try {
            // пиктограмки на кнопках "назад" и "в начало"
            if (WelcomeParams.getInstance().buttonGoBackImg != null) {
                buttonBack.setIcon(new ImageIcon(WelcomeParams.getInstance().buttonGoBackImg.toURI().toURL()));
            }
            if (WelcomeParams.getInstance().buttonToStratImg != null) {
                buttonToBegin.setIcon(new ImageIcon(WelcomeParams.getInstance().buttonToStratImg.toURI().toURL()));
            }
        } catch (MalformedURLException ex) {
            QLog.l().logger().error("Button icons! ", ex);
        }
        //На верхней панели пункта регистрации, там где заголовок и картинка в углу, можно вывести вэб-контент по URL. Оставьте пустым если не требуется
        if (!WelcomeParams.getInstance().topURL.isEmpty()) {
            panelCaption.removeAll();
            final BrowserFX bro = new BrowserFX();
            final GridLayout gl = new GridLayout(1, 1);
            panelCaption.setLayout(gl);
            panelCaption.add(bro);
            bro.load(Uses.prepareAbsolutPathForImg(WelcomeParams.getInstance().topURL));
        }
        try {
            setIconImage(ImageIO.read(FAdmin.class.getResource("/ru/apertum/qsystem/client/forms/resources/checkIn.png")));
        } catch (IOException ex) {
            QLog.l().logger().error(ex);
        }
        if (QConfig.cfg().isDebug()) {
            setSize(getScreenHorizontal(), getScreenVertical());
        }
        FWelcome.root = root;
        FWelcome.current = root;
        FWelcome.response = null;
        FWelcome.infoTree = null;
        try {
            loadRootParam();
        } catch (Exception ex) {
            QLog.l().logger().error(ex);
            System.exit(0);
        }
        server.start();
        if (!(formatHhMm.format(finishTime).equals(formatHhMm.format(startTime)))) {
            lockWelcomeTimer.start();
        }

        if (WelcomeParams.getInstance().btnAdvFont != null) {
            buttonStandAdvance.setFont(WelcomeParams.getInstance().btnAdvFont);
            buttonAdvance.setFont(WelcomeParams.getInstance().btnAdvFont);
            buttonBackPage.setFont(WelcomeParams.getInstance().btnAdvFont);
            buttonForwardPage.setFont(WelcomeParams.getInstance().btnAdvFont);
        }
        if (WelcomeParams.getInstance().btnFont != null) {
            buttonBack.setFont(WelcomeParams.getInstance().btnFont);
            buttonToBegin.setFont(WelcomeParams.getInstance().btnFont);
        }
        /*
         * Кнопки открываются по настройке
         */
        buttonInfo.setVisible(WelcomeParams.getInstance().info);
        buttonResponse.setVisible(WelcomeParams.getInstance().response);
        if (!"".equals(WelcomeParams.getInstance().infoHtml)) {
            buttonInfo.setText(WelcomeParams.getInstance().infoHtml); // NOI18N 
        }
        if (!"".equals(WelcomeParams.getInstance().responseHtml)) {
            buttonResponse.setText(WelcomeParams.getInstance().responseHtml); // NOI18N
        }
        buttonAdvance.setVisible(WelcomeParams.getInstance().advance);
        buttonStandAdvance.setVisible(WelcomeParams.getInstance().standAdvance);
        panelLngs.setVisible(Locales.getInstance().isWelcomeMultylangs());
        if (Locales.getInstance().isWelcomeMultylangs()) {
            FlowLayout la = new FlowLayout(Locales.getInstance().getMultylangsPosition(), 50, 10);
            panelLngs.setLayout(la);
            Locales.getInstance().getAvailableLangs().stream().map(lng -> {
                final JButton btn = new JButton(Uses.prepareAbsolutPathForImg(Locales.getInstance().getLangButtonText(lng)));
                btn.setContentAreaFilled(Locales.getInstance().isWelcomeMultylangsButtonsFilled());
                btn.setFocusPainted(false);
                btn.setBorderPainted(Locales.getInstance().isWelcomeMultylangsButtonsBorder());
                btn.addActionListener(new LngBtnAction(Locales.getInstance().getLocaleByName(lng)));
                btn.setVisible("1".equals(Locales.getInstance().getLangWelcome(lng)));
                return btn;
            }).forEach(panelLngs::add);
        }
        showMed();
        // Если режим инфокиоска, то не показываем кнопки предвариловки
        // Показали информацию и все
        if (FWelcome.isInfo) {
            buttonAdvance.setVisible(false);
            buttonStandAdvance.setVisible(false);
        }
    }

    private class LngBtnAction extends AbstractAction {

        private final Locale locale;

        public LngBtnAction(Locale locale) {
            this.locale = locale;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Locale.setDefault(locale);
            changeTextToLocale();
            labelCaption.setText(Uses.prepareAbsolutPathForImg(root.getTextToLocale(QService.Field.BUTTON_TEXT)));
            for (Component cmp : panelMain.getComponents()) {
                if (cmp instanceof QButton) {
                    ((QButton) cmp).refreshText();
                }
            }
        }
    }

    /**
     * Загрузка и инициализация неких параметров из корня дерева описания для старта или реинициализации.
     */
    private void loadRootParam() {
        FWelcome.caption = root.getTextToLocale(QService.Field.NAME);
        FWelcome.caption = ".".equals(FWelcome.caption) ? "" : FWelcome.caption;
        labelCaption.setText(Uses.prepareAbsolutPathForImg(root.getTextToLocale(QService.Field.BUTTON_TEXT)));
        setStateWindow(UNLOCK);
        showButtons(root, panelMain);
    }

    /**
     * Это когда происходит авторизация клиента при постановке в очередь, например перед выбором услуге в регистуре, то сюда попадает ID этого авторизованного
     * пользователя. Дальше этот ID передать в команду постановки предварительного и там если по нему найдется этот клиент, то он должен попасть в табличку
     * предварительно зарегиных.
     */
    private long advancedCustomer = -1;

    /**
     * Это когда происходит авторизация клиента при постановке в очередь, например перед выбором услуге в регистуре, то сюда попадает ID этого авторизованного
     * пользователя. Дальше этот ID передать в команду постановки предварительного и там если по нему найдется этот клиент, то он должен попасть в табличку
     * предварительно зарегиных.
     */
    public long getAdvancedCustomer() {
        return advancedCustomer;
    }

    /**
     * Заставка если режим Мед.
     */
    public final void showMed() {
        if (isMed) {
            final ATalkingClock cl = new ATalkingClock(10, 1) {

                @Override
                public void run() {
                    if (!FMedCheckIn.isShowen()) {
                        final QAuthorizationCustomer customer = FMedCheckIn.showMedCheckIn(null, true, netProperty, false, serialPort);
                        if (customer != null) {
                            advancedCustomer = customer.getId();
                            setAdvanceRegim(true);
                            labelCaption.setText("<html><p align=center><span style='font-size:55.0pt;color:green'>" + customer.getSurname() + " " + customer.getName()
                                    + " " + customer.getOtchestvo() + "<br></span><span style='font-size:40.0pt;color:red'>" + msg("messages.select_adv_servece"));
                        } else {
                            throw new ClientException("Нельзя выбирать услугу если не идентифицирован клиент.");
                        }
                    }
                }
            };
            cl.start();
        }
    }

    @Override
    protected void finalize() throws Throwable { // NOSONAR
        offWelcome();
        lockWelcomeTimer.stop();
        try {
            if (serialPort != null) {
                serialPort.free();
            }
        } catch (Exception ex) {
            throw new ClientException("Ошибка освобождения порта. " + ex);
        }
        super.finalize();
    }

    /**
     * Создаем и расставляем кнопки по форме.
     *
     * @param current уровень отображения кнопок.
     * @param panel   на эту панель.
     */
    public void showButtons(QService current, JPanel panel) {

        QLog.l().logger().info("Показываем набор кнопок уровня: " + current.getName() + " ID=" + current.getId());
        if (current != FWelcome.current) { // если смена уровней то страница уровня становится нулевая
            pageNumber = 0;
        }
        clearPanel(panel);
        // картинки для подложки с каждым набором кнопок из WelcomeBGparams. По дефолту из welcome.properties
        ((QPanel) panelBackground).setBackgroundImgage(WelcomeBGparams.get().getScreenImg(current.getId()));
        final Insets insets = WelcomeBGparams.get().getScreenInsets(current.getId());
        if (insets != null) {
            panel.setBorder(new EmptyBorder(insets));
        } else {
            panel.setBorder(new EmptyBorder(0, 0, 0, 0));
        }

        if (current != root && current.getParent() == null) {
            current.setParent(FWelcome.current);
        }
        FWelcome.current = current;


        int delta;
        switch (Toolkit.getDefaultToolkit().getScreenSize().width) {
            case 640:
                delta = 10;
                break;
            case 800:
                delta = 20;
                break;
            case 1024:
                delta = 30;
                break;
            case 1366:
                delta = 25;
                break;
            case 1280:
                delta = 40;
                break;
            case 1600:
                delta = 50;
                break;
            case 1920:
                delta = 60;
                break;
            default:
                delta = 10;
        }
        if (QConfig.cfg().isDebug() || QConfig.cfg().isDemo()) {
            delta = 25;
        }
        int cols = 3;
        int rows = 5;

        // посмотрим сколько реальных кнопок нужно отобразить
        // тут есть невидимые услуги и услуги не с того киоска
        int childCount = 0;
        childCount = current.getChildren().stream()
                .filter(service -> (!(isAdvanceRegim() && service.getAdvanceLimit() == 0) && service.getStatus() != -1
                        && (WelcomeParams.getInstance().point == 0 || (service.getPoint() == 0 || service.getPoint() == WelcomeParams.getInstance().point))))
                .map(item -> 1).reduce(childCount, Integer::sum);

        if (childCount <= WelcomeParams.getInstance().oneColumnButtonCount) {
            cols = 1;
            rows = childCount < 3 ? 3 : childCount;
        }
        if (childCount > WelcomeParams.getInstance().oneColumnButtonCount && childCount <= WelcomeParams.getInstance().twoColumnButtonCount) {
            cols = 2;
            rows = Math.round((float) childCount / 2f);
        }
        if (childCount > WelcomeParams.getInstance().twoColumnButtonCount) {
            cols = 3;
            rows = Math.round(0.3f + (float) childCount / 3);
        }

        // поправка на то что если кнопок на уровне много и они уже в три колонки, то задействуем ограничение по линиям, а то расползутся
        if (rows > WelcomeParams.getInstance().linesButtonCount && cols >= 3) {
            rows = WelcomeParams.getInstance().linesButtonCount;
            panelForPaging.setVisible(true);
        } else {
            panelForPaging.setVisible(false);
        }

        if (btnFreeDesign) {
            panel.setLayout(null);
        } else {
            final ArrayList<Integer> gaps = WelcomeBGparams.get().getScreenGaps(current.getId());
            final GridLayout la = gaps == null ? new GridLayout(rows, cols, delta, delta / 2) : new GridLayout(rows, cols, gaps.get(0), gaps.get(1));
            panel.setLayout(la);
        }
        int i = 0;
        for (QService service : current.getChildren()) {
            boolean f = true;
            if (i / (cols * rows) != pageNumber) { // смотрим каая страница из текущего уровня отображается
                f = false;
            }

            final QButton button = new QButton(service, this, panelMain, WelcomeBGparams.get().getButtonImg(service.getId()), WelcomeBGparams.get().getButtonIcon(service));
            if (!(isAdvanceRegim() && service.getAdvanceLimit() == 0) && button.isIsVisible()
                    && (WelcomeParams.getInstance().point == 0 || (service.getPoint() == 0 || service.getPoint() == WelcomeParams.getInstance().point))) {
                if (f) {
                    final Insets insetsB = WelcomeBGparams.get().getButtonInsets(service.getId());
                    if (insetsB == null) {
                        panel.add(button);
                    } else {
                        final JPanel p = new JPanel(new GridLayout(1, 1));
                        p.setBorder(new EmptyBorder(insetsB));
                        p.setOpaque(false);
                        p.add(button);
                        panel.add(p);
                    }
                    if (btnFreeDesign) {
                        button.setBounds(service.getButX(), service.getButY(), service.getButB(), service.getButH());
                    }
                    buttonForwardPage.setEnabled((i + 1) != childCount); // это чтоб кнопки листания небыли доступны когда листать дальше некуда
                }
                i++;
            }
        }
        buttonBackPage.setEnabled(pageNumber > 0); // это чтоб кнопки листания небыли доступны когда листать дальше некуда

        setVisible(true);
        buttonBack.setVisible(current != root);
        buttonToBegin.setVisible(current != root);
    }

    /**
     * Просто все контролы с палеои удалим.
     *
     * @param panel с этой панели.
     */
    public void clearPanel(JPanel panel) {
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));
        panel.removeAll();
        panel.repaint();
    }

    public static final String TEMP_FILE_PROPS = "temp/wlcm.properties";

    private static synchronized int increaseTicketCount(int d) {
        File f = new File("temp");
        if (!f.exists()) {
            f.mkdir();
        }

        f = new File(TEMP_FILE_PROPS);
        if (!f.exists()) {
            try {
                Files.createFile(f.toPath());
            } catch (IOException ex) {
                QLog.l().logger().error(ex);
            }
        }
        final Properties p = new Properties();
        try (final FileInputStream fis = new FileInputStream(f)) {
            p.load(fis);
        } catch (IOException ex) {
            throw new ServerException(ex);
        }
        int i = Math.max(Integer.parseInt(p.getProperty("tickets_cnt", "0").trim()) + d, 0);
        p.setProperty("tickets_cnt", String.valueOf(i));

        try (final FileOutputStream fos = new FileOutputStream(f)) {
            p.store(fos, "QSystem Welcome temp properties");
        } catch (IOException ex) {
            throw new ServerException(ex);
        }
        // разошлем весточку о том что бумага заканчивается
        int st = (i - WelcomeParams.getInstance().paperSizeAlarm) % WelcomeParams.getInstance().paperAlarmStep;
        if (0 <= st && st < d) {
            final String m = Mailer.fetchConfig().getProperty("paper_alarm_mailing", "0");
            if ("1".equals(m) || "true".equalsIgnoreCase(m)) {
                QLog.l().logger().info("QSystem. Paper is running out / израсходование бумаги. " + i + " tickets were printed.");
                Mailer.sendReporterMailAtFon(Mailer.fetchConfig().getProperty("mail.paper_alarm_subject", "QSystem. Printing paper run out!"),
                        "QSystem. Paper is running out / израсходование бумаги. " + i + " tickets were printed.",
                        Mailer.fetchConfig().getProperty("mail.smtp.paper_alarm_to"),
                        null);
            }
        }
        return i;
    }

    private static int write(Graphics2D g2, String text, int line, int x, double kx, double ky, int initY) {
        g2.scale(kx, ky);
        final int y = (int) Math.round((initY + line * WelcomeParams.getInstance().lineHeigth) / ky);
        g2.drawString(text, x, y);
        g2.scale(1 / kx, 1 / ky);
        return y;
    }

    /**
     * Че как по горизонтале.
     *
     * @param str       text for positioning
     * @param alignment -1 left, 0 center, 1 right
     * @return coordinate X
     */
    private static int getHalignment(Graphics2D g2, String str, int alignment, double kx) {
        if (alignment < 0) {
            return WelcomeParams.getInstance().leftMargin;
        }
        final int sw = (int) (CMP.getFontMetrics(g2.getFont()).stringWidth(str) * kx);
        int pos = alignment == 0 ? (WelcomeParams.getInstance().paperWidht - sw) / 2 : (WelcomeParams.getInstance().paperWidht - sw);

        return (int) Math.round(pos / kx);
    }

    private static int getAlign(String str, int alignmentDef) {
        if (str.toLowerCase().startsWith("[c")) {
            return 0;
        }
        if (str.toLowerCase().startsWith("[r")) {
            return 1;
        }
        if (str.toLowerCase().startsWith("[l")) {
            return -1;
        }
        return alignmentDef == -1 || alignmentDef == 0 || alignmentDef == 1 ? alignmentDef : -1;
    }

    private static String getTrim(String str) {
        return str.trim().replaceFirst("^\\[.+?\\]", "");
    }

    private static final JLabel CMP = new JLabel();

    /**
     * Строчку на канву принтера.
     *
     * @param text      Эту строчку.
     * @param g2        конва принтера.
     * @param alignment -1 left, 0 center, 1 right
     * @param kx        коэф растяжения.
     * @param ky        коэф растяжения.
     * @return new initY
     */
    private static int ptintLines(Graphics2D g2, String text, int alignment, double kx, double ky, int initY, int line) {
        final FontMetrics fm = CMP.getFontMetrics(g2.getFont());
        String capt = text;
        while (capt.length() != 0) {
            String prn;
            int leC = fm.stringWidth(capt);
            if (capt.length() > WelcomeParams.getInstance().lineLenght || leC > WelcomeParams.getInstance().paperWidht) {
                int fl = 0;

                int br = capt.toLowerCase().indexOf("<br>");
                if (br > 0 && br < WelcomeParams.getInstance().lineLenght) {
                    fl = br;
                }
                if (fl > 0) {
                    prn = capt.substring(0, fl).replaceFirst("<br>", "");
                    int le = fm.stringWidth(prn);
                    if (le > WelcomeParams.getInstance().paperWidht) {
                        fl = 0;
                    }
                }

                for (int i = Math.min(WelcomeParams.getInstance().lineLenght, capt.length()); i > 0 && fl == 0; i--) {

                    if (" ".equals(capt.substring(i - 1, i))) {
                        fl = i;
                        prn = capt.substring(0, fl);
                        int le = fm.stringWidth(prn);
                        if (le > WelcomeParams.getInstance().paperWidht) {
                            fl = 0;
                        } else {
                            break;
                        }
                    }
                }
                int pos = fl == 0 ? WelcomeParams.getInstance().lineLenght : fl;
                prn = capt.substring(0, pos).trim();
                capt = capt.substring(pos).trim();
                if (capt.toLowerCase().startsWith("<br>")) {
                    capt = capt.replaceFirst("<br>", "");
                }
            } else {
                prn = capt.trim();
                capt = "";
            }
            write(g2, getTrim(prn), line, (int) getHalignment(g2, getTrim(prn), getAlign(prn, alignment), kx), kx, ky, initY);
            int height = CMP.getFontMetrics(g2.getFont()).getHeight();
            if (!capt.isEmpty()) {
                initY = initY + Math.round(height * (height > 30 ? (height > 60 ? 0.65f : 0.7f) : (height > 10 ? 0.83f : 1f)));
            }
        }
        return initY;
    }

    public static void printTicket(QCustomer customer, String caption) {
        FWelcome.caption = ".".equals(caption) ? "" : caption;
        printTicket(customer);
    }

    /**
     * Печатаем талон.
     *
     * @param customer Этому печатаем.
     */
    public static synchronized void printTicket(final QCustomer customer) {
        if (!WelcomeParams.getInstance().print) {
            return;
        }
        increaseTicketCount(1);
        // поддержка расширяемости плагинами
        boolean flag = false;
        for (final IPrintTicket event : ServiceLoader.load(IPrintTicket.class)) {
            QLog.l().logger().info("Вызов SPI расширения. Описание: " + event.getDescription());
            try {
                flag = event.printTicket(customer, FWelcome.caption);
            } catch (Exception tr) {
                QLog.l().logger().error("Вызов SPI расширения завершился ошибкой. Описание: ", tr);
            }
            // раз напечатили и хорошь
            if (flag) {
                return;
            }
        }

        final Printable canvas = new Printable() {

            private int initY = WelcomeParams.getInstance().topMargin;

            private final JLabel comp = new JLabel();
            Graphics2D g2;

            @Override
            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                initY = WelcomeParams.getInstance().topMargin;
                if (pageIndex >= 1) {
                    return Printable.NO_SUCH_PAGE;
                }
                g2 = (Graphics2D) graphics;
                final Font fontStandard;
                if (WelcomeParams.getInstance().ticketFontName != null && !WelcomeParams.getInstance().ticketFontName.isEmpty()) {
                    fontStandard = (new Font(WelcomeParams.getInstance().ticketFontName, g2.getFont().getStyle(),
                            WelcomeParams.getInstance().ticketFontSize > 2 ? WelcomeParams.getInstance().ticketFontSize : g2.getFont().getSize()));
                } else {
                    fontStandard = g2.getFont();
                }
                g2.setFont(fontStandard);
                g2.drawLine(WelcomeParams.getInstance().paperWidht + 20, 0, WelcomeParams.getInstance().paperWidht + 20, 20);
                if (WelcomeParams.getInstance().logo) {
                    g2.drawImage(Uses.loadImage(this, WelcomeParams.getInstance().logoImg, "/ru/apertum/qsystem/client/forms/resources/logo_ticket_a.png"),
                            WelcomeParams.getInstance().logoLeft, WelcomeParams.getInstance().logoTop, null);
                }
                g2.scale(WelcomeParams.getInstance().scaleHorizontal, WelcomeParams.getInstance().scaleVertical);
                //позиционируем начало координат 
                g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                int line = 0;

                if (caption != null && !caption.isEmpty()) {
                    line = 1;
                    g2.setFont(new Font(g2.getFont().getName(), g2.getFont().getStyle(), WelcomeParams.getInstance().ticketFontH2Size));
                    initY = ptintLines(g2, caption, 0, 1, 1, initY, line);
                    initY = initY + WelcomeParams.getInstance().lineHeigth / 3;
                }
                g2.setFont(fontStandard);
                write(g2, msg("ticket.your_number"), ++line, getHalignment(g2, msg("ticket.your_number"), 0, 1), 1, 1, initY);

                final String num = customer.getFullNumber();
                g2.setFont(new Font(g2.getFont().getName(), g2.getFont().getStyle(),
                        WelcomeParams.getInstance().ticketFontH1Size / (num.length() < 6 ? 1 : (num.length() < 10 ? 2 : 3))));
                int height = comp.getFontMetrics(g2.getFont()).getHeight();
                initY = initY + Math.round(height * (height > 30 ? (height > 60 ? 0.65f : 0.7f) : 1f));
                write(g2, num, line, getHalignment(g2, num, 0, 1), 1, 1, initY);
                initY = initY + WelcomeParams.getInstance().lineHeigth / 3;
                g2.setFont(fontStandard);

                g2.setFont(new Font(g2.getFont().getName(), Font.BOLD, g2.getFont().getSize()));
                write(g2, msg("ticket.service"), ++line, WelcomeParams.getInstance().leftMargin, 1, 1, initY);
                g2.setFont(fontStandard);
                String name = customer.getService().getTextToLocale(QService.Field.NAME);
                initY = ptintLines(g2, name, -1, 1, 1, initY, ++line);

                initY = initY + WelcomeParams.getInstance().lineHeigth / 3;

                g2.setFont(new Font(g2.getFont().getName(), Font.BOLD, g2.getFont().getSize()));
                write(g2, msg("ticket.time"), ++line, WelcomeParams.getInstance().leftMargin, 1, 1, initY);
                g2.setFont(fontStandard);

                write(g2, Locales.getInstance().isRuss()
                                ? Uses.getRusDate(customer.getStandTime(), "dd MMMM HH:mm")
                                : (Locales.getInstance().isUkr()
                                        ? Uses.getUkrDate(customer.getStandTime(), "dd MMMM HH:mm")
                                        : (Locales.getInstance().isEng()
                                                ? Locales.getInstance().formatForPrint.format(customer.getStandTime())
                                                : Locales.getInstance().formatForPrintShort.format(customer.getStandTime()))),
                        ++line, WelcomeParams.getInstance().leftMargin, 1, 1, initY);
                // если клиент что-то ввел, то напечатаем это на его талоне
                if (customer.getService().getInputRequired()) {
                    initY = initY + WelcomeParams.getInstance().lineHeigth / 3;
                    g2.setFont(new Font(g2.getFont().getName(), Font.BOLD, g2.getFont().getSize()));
                    write(g2, customer.getService().getTextToLocale(QService.Field.INPUT_CAPTION).replaceAll("<.*?>", ""),
                            ++line, WelcomeParams.getInstance().leftMargin, 1, 1, initY);
                    g2.setFont(fontStandard);
                    write(g2, customer.getInputData(), ++line, WelcomeParams.getInstance().leftMargin, 1, 1, initY);
                    // если требуется, то введеное напечатаем как qr-код для быстрого считывания сканером
                    if (WelcomeParams.getInstance().inputDataQrcode) {
                        try {
                            final int matrixWidth = 130;
                            final HashMap<EncodeHintType, String> hints = new HashMap();
                            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                            final BitMatrix matrix = new QRCodeWriter().encode(customer.getInputData(), BarcodeFormat.QR_CODE, matrixWidth, matrixWidth, hints);
                            //Write Bit Matrix as image
                            final int y = Math.round((WelcomeParams.getInstance().topMargin + line * WelcomeParams.getInstance().lineHeigth) / 1f);
                            for (int i = 0; i < matrixWidth; i++) {
                                for (int j = 0; j < matrixWidth; j++) {
                                    if (matrix.get(i, j)) {
                                        g2.fillRect(WelcomeParams.getInstance().leftMargin * 2 + i, y + j - 10, 1, 1);
                                    }
                                }
                            }
                            line = line + 9;
                        } catch (WriterException ex) {
                            QLog.l().logger().error("Ошибка вывода штрихкода QR. " + ex);
                        }
                    }
                }
                // если в услуге есть что напечатать на талоне, то напечатаем это на его талоне
                if (customer.getService().getTextToLocale(QService.Field.TICKET_TEXT) != null && !customer.getService().getTextToLocale(QService.Field.TICKET_TEXT).isEmpty()) {
                    initY = initY + WelcomeParams.getInstance().lineHeigth / 3;
                    String tt = customer.getService().getTextToLocale(QService.Field.TICKET_TEXT);
                    initY = ptintLines(g2, tt, -1, 1, 1, initY, ++line);
                }

                String someText = WelcomeParams.getInstance().waitText == null || WelcomeParams.getInstance().waitText.isEmpty()
                        ? ("[c]" + msg("ticket.wait"))
                        : WelcomeParams.getInstance().waitText;
                if (someText != null && !someText.trim().isEmpty() && !".".equals(someText)) {
                    initY = initY + WelcomeParams.getInstance().lineHeigth / 3;
                    write(g2, getTrim(someText), ++line, getHalignment(g2, getTrim(someText), getAlign(someText, -1), 1.45), 1.45, 1, initY);
                }
                someText = WelcomeParams.getInstance().promoText;
                if (someText != null && !someText.isEmpty() && !".".equals(someText)) {
                    write(g2, getTrim(someText), ++line, getHalignment(g2, getTrim(someText), getAlign(someText, -1), 0.7), 0.7, 0.4, initY);
                }

                if (WelcomeParams.getInstance().barcode != 0) {
                    int coordY = write(g2, "", ++line, 0, 1, 1, initY);

                    String contents = customer.getId().toString();
                    if (WelcomeParams.getInstance().barcode == 2) {
                        try {
                            final int matrixWidth = 100;
                            final HashMap<EncodeHintType, String> hints = new HashMap();
                            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                            contents = QProperties.get().getProperty(QProperties.SECTION_WELCOME, BARCODE_TEXT, contents)
                                    .replace("#input", customer.getInputData())
                                    .replace("#id", customer.getFullNumber());
                            final BitMatrix matrix = new QRCodeWriter().encode(contents, BarcodeFormat.QR_CODE, matrixWidth, matrixWidth, hints);
                            //Write Bit Matrix as image
                            for (int i = 0; i < matrixWidth; i++) {
                                for (int j = 0; j < matrixWidth; j++) {
                                    if (matrix.get(i, j)) {
                                        g2.fillRect(WelcomeParams.getInstance().leftMargin * 2 + i, coordY + j - 18, 1, 1);
                                    }
                                }
                            }
                            line = line + 6;
                        } catch (WriterException ex) {
                            QLog.l().logger().error("Ошибка вывода штрихкода QR. " + ex);
                        }
                    }

                    if (WelcomeParams.getInstance().barcode == 1) {
                        try {
                            final Barcode barcode = BarcodeFactory.createCode128B(contents);
                            barcode.setBarHeight(5);
                            barcode.setBarWidth(1);
                            barcode.setDrawingText(false);
                            barcode.setDrawingQuietSection(false);
                            barcode.draw(g2, (WelcomeParams.getInstance().paperWidht - barcode.getSize().width) / 2, coordY - 7);
                            line = line + 2;
                        } catch (BarcodeException | OutputException ex) {
                            QLog.l().logger().error("Ошибка вывода штрихкода 128B. " + ex);
                        }
                    }
                }

                //Напечатаем текст внизу билета
                name = WelcomeParams.getInstance().bottomText;
                int al = getAlign(name, -1);
                name = getTrim(name);
                if (name != null && !name.isEmpty() && !".".equals(name)) {
                    initY = ptintLines(g2, name, al, 1, 1, initY, ++line);
                }
                if (WelcomeParams.getInstance().bottomGap > 0) {
                    write(g2, ".", ++line + WelcomeParams.getInstance().bottomGap, 0, 1, 1, initY);
                }

                return Printable.PAGE_EXISTS;
            }
        };
        final PrinterJob job = PrinterJob.getPrinterJob();
        if (WelcomeParams.getInstance().printService != null) {
            try {
                job.setPrintService(WelcomeParams.getInstance().printService);
            } catch (PrinterException ex) {
                QLog.l().logger().error("Ошибка установки принтера: ", ex);
            }
        }
        job.setPrintable(canvas);
        try {
            job.print(WelcomeParams.getInstance().printAttributeSet);
        } catch (PrinterException ex) {
            QLog.l().logger().error("Ошибка печати: ", ex);
        }
    }

    public static void printTicketAdvance(QAdvanceCustomer advCustomer, String caption) {
        FWelcome.caption = ".".equals(caption) ? "" : caption;
        printTicketAdvance(advCustomer);
    }

    /**
     * Печатаем талон предварительного.
     *
     * @param advCustomer предварительный кастомер.
     */
    public static synchronized void printTicketAdvance(final QAdvanceCustomer advCustomer) {
        if (!WelcomeParams.getInstance().print) {
            return;
        }
        increaseTicketCount(1);
        // поддержка расширяемости плагинами
        boolean flag = false;
        for (final IPrintTicket event : ServiceLoader.load(IPrintTicket.class)) {
            QLog.l().logger().info("Вызов SPI расширения. Описание: " + event.getDescription());
            try {
                flag = event.printTicketAdvance(advCustomer, FWelcome.caption);
            } catch (Exception tr) {
                QLog.l().logger().error("Вызов SPI расширения завершился ошибкой. Описание: " + tr);
            }
            // раз напечатили и хорошь
            if (flag) {
                return;
            }
        }

        final Printable canvas = new Printable() {
            private int initY = WelcomeParams.getInstance().topMargin;

            Graphics2D g2;

            @Override
            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                initY = WelcomeParams.getInstance().topMargin;
                if (pageIndex >= 1) {
                    return Printable.NO_SUCH_PAGE;
                }
                g2 = (Graphics2D) graphics;
                final Font fontStandard;
                if (WelcomeParams.getInstance().ticketFontName != null && !WelcomeParams.getInstance().ticketFontName.isEmpty()) {
                    fontStandard = (new Font(WelcomeParams.getInstance().ticketFontName, g2.getFont().getStyle(),
                            WelcomeParams.getInstance().ticketFontSize > 2 ? WelcomeParams.getInstance().ticketFontSize : g2.getFont().getSize()));
                } else {
                    fontStandard = g2.getFont();
                }
                g2.setFont(fontStandard);
                g2.drawLine(WelcomeParams.getInstance().paperWidht + 20, 0, WelcomeParams.getInstance().paperWidht + 20, 20);

                if (WelcomeParams.getInstance().logo) {
                    g2.drawImage(Uses.loadImage(this, WelcomeParams.getInstance().logoImg, "/ru/apertum/qsystem/client/forms/resources/logo_ticket_a.png"),
                            WelcomeParams.getInstance().logoLeft, WelcomeParams.getInstance().logoTop, null);
                }
                g2.scale(WelcomeParams.getInstance().scaleHorizontal, WelcomeParams.getInstance().scaleVertical);
                //позиционируем начало координат
                g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                int line = 0;

                if (caption != null && !caption.isEmpty()) {
                    line = 1;
                    g2.setFont(new Font(g2.getFont().getName(), g2.getFont().getStyle(), WelcomeParams.getInstance().ticketFontH2Size));
                    initY = ptintLines(g2, caption, 0, 1, 1, initY, line);
                    initY = initY + WelcomeParams.getInstance().lineHeigth / 3;
                }

                g2.setFont(fontStandard);
                write(g2, msg("ticket.adv_purpose"), ++line, getHalignment(g2, msg("ticket.adv_purpose"), 0, 1), 1, 1, initY);

                final GregorianCalendar gcTime = new GregorianCalendar();
                gcTime.setTime(advCustomer.getAdvanceTime());
                int hours = gcTime.get(GregorianCalendar.HOUR_OF_DAY);
                int mins = gcTime.get(GregorianCalendar.MINUTE);
                if (hours == 0) {
                    hours = 24;
                    gcTime.add(GregorianCalendar.HOUR_OF_DAY, -1);
                }
                String minutes = ("" + mins + "0000").substring(0, 2);
                g2.setFont(new Font(g2.getFont().getName(), Font.BOLD, g2.getFont().getSize()));
                String tx = Locales.getInstance().isRuss()
                        ? Uses.getRusDate(gcTime.getTime(), Locales.DATE_FORMAT_FULL)
                        : Locales.getInstance().formatDdMmYyyy.format(gcTime.getTime());
                write(g2, tx, ++line, getHalignment(g2, tx, 0, 1), 1, 1, initY);
                tx = FWelcome.msg("qbutton.take_adv_ticket_come_to") + " " + (hours) + ":" + minutes;
                write(g2, tx, ++line, getHalignment(g2, tx, 0, 1), 1, 1, initY);
                g2.setFont(fontStandard);
                initY = initY + WelcomeParams.getInstance().lineHeigth / 3;

                g2.setFont(new Font(g2.getFont().getName(), Font.BOLD, g2.getFont().getSize()));
                write(g2, msg("ticket.service"), ++line, WelcomeParams.getInstance().leftMargin, 1, 1, initY);
                g2.setFont(fontStandard);
                String name = advCustomer.getService().getTextToLocale(QService.Field.NAME);
                initY = ptintLines(g2, name, -1, 1, 1, initY, ++line);

                initY = initY + WelcomeParams.getInstance().lineHeigth / 3;
                g2.setFont(new Font(g2.getFont().getName(), Font.BOLD, g2.getFont().getSize()));
                write(g2, msg("ticket.reg_time"), ++line, WelcomeParams.getInstance().leftMargin, 1, 1, initY);
                g2.setFont(fontStandard);

                write(g2, Locales.getInstance().isRuss() ? Uses.getRusDate(new Date(), "dd MMMM HH:mm")
                        : Locales.getInstance().formatForPrintShort.format(new Date()), ++line, WelcomeParams.getInstance().leftMargin, 1, 1, initY);

                // если клиент что-то ввел, то напечатаем это на его талоне
                if (advCustomer.getService().getInputRequired()) {
                    initY = initY + WelcomeParams.getInstance().lineHeigth / 3;
                    g2.setFont(new Font(g2.getFont().getName(), Font.BOLD, g2.getFont().getSize()));
                    write(g2, advCustomer.getService().getTextToLocale(QService.Field.INPUT_CAPTION).replaceAll("<.*?>", ""),
                            ++line, WelcomeParams.getInstance().leftMargin, 1, 1, initY);
                    g2.setFont(fontStandard);
                    write(g2, advCustomer.getInputData(), ++line, WelcomeParams.getInstance().leftMargin, 1, 1, initY);
                    // если требуется, то введеное напечатаем как qr-код для быстрого считывания сканером
                    if (WelcomeParams.getInstance().inputDataQrcode) {
                        try {
                            final int matrixWidth = 130;
                            final HashMap<EncodeHintType, String> hints = new HashMap();
                            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                            final BitMatrix matrix =
                                    new QRCodeWriter().encode(advCustomer.getAuthorizationCustomer().getName(), BarcodeFormat.QR_CODE, matrixWidth, matrixWidth, hints);
                            //Write Bit Matrix as image
                            final int y = Math.round((WelcomeParams.getInstance().topMargin + line * WelcomeParams.getInstance().lineHeigth) / 1f);
                            for (int i = 0; i < matrixWidth; i++) {
                                for (int j = 0; j < matrixWidth; j++) {
                                    if (matrix.get(i, j)) {
                                        g2.fillRect(WelcomeParams.getInstance().leftMargin * 2 + i, y + j - 10, 1, 1);
                                    }
                                }
                            }
                            line = line + 9;
                        } catch (WriterException ex) {
                            QLog.l().logger().error("Ошибка вывода штрихкода QR. " + ex);
                        }
                    }
                }

                // если в услуге есть что напечатать на талоне, то напечатаем это на его талоне
                if (advCustomer.getService().getTextToLocale(QService.Field.TICKET_TEXT) != null
                        && !advCustomer.getService().getTextToLocale(QService.Field.TICKET_TEXT).isEmpty()) {
                    initY = initY + WelcomeParams.getInstance().lineHeigth / 3;
                    String tt = advCustomer.getService().getTextToLocale(QService.Field.TICKET_TEXT);
                    initY = ptintLines(g2, tt, -1, 1, 1, initY, ++line);
                }

                write(g2, msg("ticket.adv_code"), ++line, getHalignment(g2, msg("ticket.adv_code"), 0, 1), 1, 1, initY);
                int coordY = write(g2, "", ++line, 0, 1, 1, initY);
                String contents = advCustomer.getId().toString();
                if (WelcomeParams.getInstance().barcode != 0) {

                    if (WelcomeParams.getInstance().barcode == 2) {
                        try {
                            final int matrixWidth = 100;
                            final HashMap<EncodeHintType, String> hints = new HashMap();
                            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                            // а переподсунем текст из настроек.
                            contents = QProperties.get().getProperty(QProperties.SECTION_WELCOME, BARCODE_TEXT, contents).replace("#id", advCustomer.getId().toString());
                            final BitMatrix matrix = new QRCodeWriter().encode(contents, BarcodeFormat.QR_CODE, matrixWidth, matrixWidth, hints);
                            //Write Bit Matrix as image
                            for (int i = 0; i < matrixWidth; i++) {
                                for (int j = 0; j < matrixWidth; j++) {
                                    if (matrix.get(i, j)) {
                                        g2.fillRect(WelcomeParams.getInstance().leftMargin * 2 + i, coordY + j - 18, 1, 1);
                                    }
                                }
                            }
                            line = line + 6;
                            write(g2, contents, ++line, getHalignment(g2, contents, 0, 2), 2.0, 1.7, initY);
                        } catch (WriterException ex) {
                            QLog.l().logger().error("Ошибка вывода штрихкода QR. " + ex);
                        }
                    }

                    if (WelcomeParams.getInstance().barcode == 1) {
                        try {
                            final Barcode barcode = BarcodeFactory.createCode128B(contents);
                            barcode.setBarHeight(5);
                            barcode.setBarWidth(1);
                            barcode.setDrawingText(true);
                            barcode.setDrawingQuietSection(true);
                            barcode.draw(g2, (WelcomeParams.getInstance().paperWidht - barcode.getSize().width) / 2, coordY - 7);
                            line = line + 3;
                        } catch (BarcodeException | OutputException ex) {
                            QLog.l().logger().error("Ошибка вывода штрихкода 128B. " + ex);
                        }
                    }
                } else {
                    initY = ptintLines(g2, contents, 0, 2.0, 1.7, initY, ++line);
                }

                String promoText = WelcomeParams.getInstance().promoText;
                if (promoText != null && !promoText.isEmpty() && !".".equals(promoText)) {
                    write(g2, getTrim(promoText), ++line, getHalignment(g2, getTrim(promoText), getAlign(promoText, -1), 0.7), 0.7, 0.4, initY);
                }
                //Напечатаем текст внизу билета

                name = WelcomeParams.getInstance().bottomText;
                int al = getAlign(name, -1);
                name = getTrim(name);
                if (name != null && !name.isEmpty() && !".".equals(name)) {
                    initY = ptintLines(g2, name, al, 1, 1, initY, ++line);
                }

                if (WelcomeParams.getInstance().bottomGap > 0) {
                    write(g2, ".", ++line + WelcomeParams.getInstance().bottomGap, 0, 1, 1, initY);
                }

                return Printable.PAGE_EXISTS;
            }
        };
        final PrinterJob job = PrinterJob.getPrinterJob();
        if (WelcomeParams.getInstance().printService != null) {
            try {
                job.setPrintService(WelcomeParams.getInstance().printService);
            } catch (PrinterException ex) {
                QLog.l().logger().error("Ошибка установки принтера: ", ex);
            }
        }
        job.setPrintable(canvas);
        try {
            job.print(WelcomeParams.getInstance().printAttributeSet);
        } catch (PrinterException ex) {
            QLog.l().logger().error("Ошибка печати: ", ex);
        }
    }

    /**
     * Вывод на конву принтера.
     *
     * @param preInfo текст для печати.
     */
    public static synchronized void printPreInfoText(final String preInfo) {
        increaseTicketCount(2);
        Printable canvas = new Printable() {
            private int initY = WelcomeParams.getInstance().topMargin;

            private int write(String text, int line, int x, double kx, double ky, int pageIndex) {

                if (line <= pageIndex * WelcomeParams.getInstance().pageLinesCount || line > (pageIndex + 1) * WelcomeParams.getInstance().pageLinesCount) {
                    return 0;
                }
                g2.scale(kx, ky);
                final int y = (int) Math.round((initY + line * WelcomeParams.getInstance().lineHeigth) / ky);
                g2.drawString(text, x, y);
                g2.scale(1 / kx, 1 / ky);
                return y;
            }

            Graphics2D g2;

            @Override
            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                initY = WelcomeParams.getInstance().topMargin;

                g2 = (Graphics2D) graphics;
                final Font fontStandard;
                if (WelcomeParams.getInstance().ticketFontName != null && !WelcomeParams.getInstance().ticketFontName.isEmpty()) {
                    fontStandard = (new Font(WelcomeParams.getInstance().ticketFontName, g2.getFont().getStyle(),
                            WelcomeParams.getInstance().ticketFontSize > 2 ? WelcomeParams.getInstance().ticketFontSize : g2.getFont().getSize()));
                } else {
                    fontStandard = g2.getFont();
                }
                g2.setFont(fontStandard);
                g2.drawLine(WelcomeParams.getInstance().paperWidht + 20, 0, WelcomeParams.getInstance().paperWidht + 20, 20);
                if (WelcomeParams.getInstance().logo) {
                    g2.drawImage(Uses.loadImage(this, WelcomeParams.getInstance().logoImg, "/ru/apertum/qsystem/client/forms/resources/logo_ticket_a.png"),
                            WelcomeParams.getInstance().logoLeft, WelcomeParams.getInstance().logoTop, null);
                }
                g2.scale(WelcomeParams.getInstance().scaleHorizontal, WelcomeParams.getInstance().scaleVertical);
                //позиционируем начало координат 
                g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                int line = 0;

                if (caption != null && !caption.isEmpty()) {
                    line = 1;
                    g2.setFont(new Font(g2.getFont().getName(), g2.getFont().getStyle(), WelcomeParams.getInstance().ticketFontH2Size));
                    initY = ptintLines(g2, caption, 0, 1, 1, initY, line);
                    initY = initY + WelcomeParams.getInstance().lineHeigth / 3;
                }
                g2.setFont(fontStandard);

                // напечатаем текст подсказки
                final LinkedList<String> strings = new LinkedList<>();
                try (final Scanner sc = new Scanner(preInfo.replace("<br>", "\n"))) {
                    while (sc.hasNextLine()) {
                        final String w = sc.nextLine();
                        strings.add(w);
                    }
                }
                for (String string : strings) {
                    String srt = string;
                    int al = getAlign(srt, -1);
                    srt = getTrim(srt);
                    if (srt != null && !srt.isEmpty() && !".".equals(srt)) {
                        initY = ptintLines(g2, srt, al, 1, 1, initY, ++line);
                    }
                }

                String promoText = WelcomeParams.getInstance().promoText;
                if (promoText != null && !promoText.isEmpty()) {
                    write(getTrim(promoText), ++line, getHalignment(g2, getTrim(promoText), getAlign(promoText, -1), 0.7), 0.7, 0.4, pageIndex);
                }

                //Напечатаем текст внизу билета
                String name = WelcomeParams.getInstance().bottomText;
                int al = getAlign(name, -1);
                name = getTrim(name);
                if (name != null && !name.isEmpty() && !".".equals(name)) {
                    initY = ptintLines(g2, name, al, 1, 1, initY, ++line);
                }
                if (WelcomeParams.getInstance().bottomGap > 0) {
                    write(".", ++line + WelcomeParams.getInstance().bottomGap, 0, 1, 1, pageIndex);
                }

                if ((pageIndex + 0) * WelcomeParams.getInstance().pageLinesCount > line) {
                    return Printable.NO_SUCH_PAGE;
                } else {
                    return Printable.PAGE_EXISTS;
                }
            }
        };
        final PrinterJob job = PrinterJob.getPrinterJob();
        if (WelcomeParams.getInstance().printService != null) {
            try {
                job.setPrintService(WelcomeParams.getInstance().printService);
            } catch (PrinterException ex) {
                QLog.l().logger().error("Ошибка установки принтера: ", ex);
            }
        }
        job.setPrintable(canvas);
        try {
            job.print(WelcomeParams.getInstance().printAttributeSet);
        } catch (PrinterException ex) {
            QLog.l().logger().error("Ошибка печати: ", ex);
        }
    }

    /**
     * Показываем некоторые кнопки управления.
     *
     * @param visible да или нет.
     */
    public void setVisibleButtons(boolean visible) {
        buttonBack.setVisible(visible && current != root);
        buttonToBegin.setVisible(visible && current != root);

        buttonStandAdvance.setVisible(WelcomeParams.getInstance().standAdvance && visible);
        buttonAdvance.setVisible(WelcomeParams.getInstance().advance && visible);

        buttonInfo.setVisible(WelcomeParams.getInstance().info && visible);
        buttonResponse.setVisible(WelcomeParams.getInstance().response && visible);

        int cols = 3;
        int rows = 5;

        // посмотрим сколько реальных кнопок нужно отобразить
        // тут есть невидимые услуги и услуги не с того киоска
        int childCount = 0;
        childCount = current.getChildren().stream()
                .filter(service -> (service.getStatus() != -1 && service.getStatus() != 5
                        && (WelcomeParams.getInstance().point == 0 || (service.getPoint() == 0 || service.getPoint() == WelcomeParams.getInstance().point))))
                .map(item -> 1).reduce(childCount, Integer::sum);

        if (childCount < 4) {
            cols = 1;
            rows = 3;
        }
        if (childCount > 3 && childCount < 11) {
            cols = 2;
            rows = Math.round((float) childCount / 2);
        }
        if (childCount > 10) {
            cols = 3;
            rows = Math.round(0.3f + (float) childCount / 3);
        }

        // поправка на то что если кнопок на уровне много и они уже в три колонки, то задействуем ограничение по линиям, а то расползутся
        if (visible && rows > WelcomeParams.getInstance().linesButtonCount && cols >= 3) {
            panelForPaging.setVisible(true);
        } else {
            panelForPaging.setVisible(false);
        }

        if (visible && Locales.getInstance().isWelcomeMultylangs()) {
            panelLngs.setVisible(true);
        } else {
            panelLngs.setVisible(false);
        }
    }

    //==================================================================================================================
    //С рабочего места администратора должна быть возможность заблокировать пункт постановки в очередь, 
    //разблокировать, выключить, провести инициализация заново.
    private String stateWindow = UNLOCK;

    public String getStateWindow() {
        return stateWindow;
    }

    /**
     * Выставим состояние киоска, т.е. заблокирован или нет.
     *
     * @param state состояние доступности.
     */
    public void setStateWindow(String state) {
        this.stateWindow = state;
        panelLock.setVisible(LOCK.equals(state));
        panelMain.setVisible(UNLOCK.equals(state));
        if (isMed) {
            if (LOCK.equals(state)) {
                FMedCheckIn.setBlockDialog(true);
            }
            if (UNLOCK.equals(state)) {
                FMedCheckIn.setBlockDialog(false);
            }
        }
    }

    /**
     * Заблокировать пункт постановки в очередь.
     *
     * @param message Сообщение, которое выведется на экран пункта.
     */
    public void lockWelcome(String message) {
        labelLock.setText(message);
        setStateWindow(LOCK);
        setVisibleButtons(false);
        QLog.l().logger().info("Пункт регистрации заблокирован. Состояние \"" + stateWindow + "\"");
    }

    /**
     * Разблокировать пункт постановки в очередь.
     */
    public void unlockWelcome() {
        setStateWindow(UNLOCK);
        setVisibleButtons(true);
        QLog.l().logger().info("Пункт регистрации готов к работе. Состояние \"" + stateWindow + "\"");
    }

    /**
     * Выключить пункт постановки в очередь.
     */
    public void offWelcome() {
        setStateWindow(OFF);
        exitServer = true;
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            QLog.l().logger().error("Проблемы с таймером. ", ex);
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }
        QLog.l().logger().info("Пункт регистрации выключен. Состояние \"" + stateWindow + "\"");
    }

    /**
     * Инициализация заново пункта постановки в очередь.
     *
     * @param params их перечитываем.
     */
    public void reinit(CmdParams params) {
        final RpcGetAllServices.ServicesForWelcome servs;
        try {
            servs = NetCommander.getServices(netProperty);
        } catch (QException e) {
            throw new ServerException(e);
        }
        final QService reroot = servs.getRoot();
        FWelcome.root = reroot;
        FWelcome.current = reroot;
        FWelcome.response = null;
        FWelcome.infoTree = null;
        FWelcome.startTime = servs.getStartTime();
        FWelcome.finishTime = servs.getFinishTime();
        FWelcome.btnFreeDesign = servs.getButtonFreeDesign();
        loadRootParam();
        if (params.dropTicketsCounter) {
            increaseTicketCount(Integer.MIN_VALUE);
        }
        QLog.l().logger().info("Пункт регистрации реинициализирован. Состояние \"" + stateWindow + "\"");
    }

    /**
     * Таймер, по которому будем включать и выключать пункт регистрации.
     */
    private transient ATalkingClock lockWelcomeTimer = new ATalkingClock(Uses.DELAY_CHECK_TO_LOCK, 0) {

        @Override
        public void run() {
            // если время начала и завершения совпадают, то игнор блокировки.
            if (formatHhMm.format(finishTime).equals(formatHhMm.format(startTime))) {
                return;
            }
            if (formatHhMm.format(new Date()).equals(formatHhMm.format(finishTime))) {
                lockWelcome(STOP_MESSAGE);
            }
            if (formatHhMm.format(new Date()).equals(formatHhMm.format(startTime))) {
                unlockWelcome();
            }
        }
    };
    //==================================================================================================================

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelBackground = new QPanel(WelcomeParams.getInstance().backgroundImg);
        panelCaption = new QPanel(WelcomeParams.getInstance().topImg);
        labelCaption = new javax.swing.JLabel();
        panelButtons = new javax.swing.JPanel();
        buttonAdvance = new QButton(WelcomeParams.getInstance().servButtonType);
        buttonStandAdvance = new QButton(WelcomeParams.getInstance().servButtonType);
        buttonToBegin = new QButton(WelcomeParams.getInstance().servButtonType);
        buttonBack = new QButton(WelcomeParams.getInstance().servButtonType);
        panelCentre = new javax.swing.JPanel();
        panelMain = new javax.swing.JPanel();
        panelLock = new javax.swing.JPanel();
        labelLock = new javax.swing.JLabel();
        buttonInfo = new QButton(WelcomeParams.getInstance().servVertButtonType);
        buttonResponse = new QButton(WelcomeParams.getInstance().servVertButtonType);
        panelLngs = new javax.swing.JPanel();
        panelForPaging = new javax.swing.JPanel();
        buttonBackPage = new QButton(WelcomeParams.getInstance().servButtonType);
        buttonForwardPage = new QButton(WelcomeParams.getInstance().servButtonType);
        labelForwardPage = new javax.swing.JLabel();
        labelBackPage = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(ru.apertum.qsystem.QSystem.class).getContext().getResourceMap(FWelcome.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setMinimumSize(new java.awt.Dimension(640, 480));
        setName("Form"); // NOI18N

        panelBackground.setBorder(new javax.swing.border.MatteBorder(null));
        panelBackground.setName("panelBackground"); // NOI18N

        panelCaption.setBorder(new javax.swing.border.MatteBorder(null));
        panelCaption.setName("panelCaption"); // NOI18N
        panelCaption.setOpaque(false);
        panelCaption.setPreferredSize(new java.awt.Dimension(1008, 150));

        labelCaption.setFont(resourceMap.getFont("labelCaption.font")); // NOI18N
        labelCaption.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelCaption.setText(resourceMap.getString("labelCaption.text")); // NOI18N
        labelCaption.setName("labelCaption"); // NOI18N

        javax.swing.GroupLayout panelCaptionLayout = new javax.swing.GroupLayout(panelCaption);
        panelCaption.setLayout(panelCaptionLayout);
        panelCaptionLayout.setHorizontalGroup(
                panelCaptionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(labelCaption, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        panelCaptionLayout.setVerticalGroup(
                panelCaptionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(labelCaption, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
        );

        panelButtons.setBorder(new javax.swing.border.MatteBorder(null));
        panelButtons.setName("panelButtons"); // NOI18N
        panelButtons.setOpaque(false);
        panelButtons.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                panelButtonsMouseClicked(evt);
            }
        });
        panelButtons.setLayout(new java.awt.GridLayout(1, 0, 10, 0));

        buttonAdvance.setFont(resourceMap.getFont("buttonAdvance.font")); // NOI18N
        buttonAdvance.setText(resourceMap.getString("buttonAdvance.text")); // NOI18N
        buttonAdvance.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED))));
        buttonAdvance.setFocusPainted(false);
        buttonAdvance.setName("buttonAdvance"); // NOI18N
        buttonAdvance.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAdvanceActionPerformed(evt);
            }
        });
        panelButtons.add(buttonAdvance);

        buttonStandAdvance.setFont(resourceMap.getFont("buttonStandAdvance.font")); // NOI18N
        buttonStandAdvance.setText(resourceMap.getString("buttonStandAdvance.text")); // NOI18N
        buttonStandAdvance.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED))));
        buttonStandAdvance.setFocusPainted(false);
        buttonStandAdvance.setName("buttonStandAdvance"); // NOI18N
        buttonStandAdvance.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStandAdvanceActionPerformed(evt);
            }
        });
        panelButtons.add(buttonStandAdvance);

        buttonToBegin.setFont(resourceMap.getFont("buttonToBegin.font")); // NOI18N
        buttonToBegin.setIcon(resourceMap.getIcon("buttonToBegin.icon")); // NOI18N
        buttonToBegin.setText(resourceMap.getString("buttonToBegin.text")); // NOI18N
        buttonToBegin.setActionCommand(resourceMap.getString("buttonToBegin.actionCommand")); // NOI18N
        buttonToBegin.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED))));
        buttonToBegin.setFocusPainted(false);
        buttonToBegin.setName("buttonToBegin"); // NOI18N
        buttonToBegin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonToBeginActionPerformed(evt);
            }
        });
        panelButtons.add(buttonToBegin);

        buttonBack.setFont(resourceMap.getFont("buttonBack.font")); // NOI18N
        buttonBack.setIcon(resourceMap.getIcon("buttonBack.icon")); // NOI18N
        buttonBack.setText(resourceMap.getString("buttonBack.text")); // NOI18N
        buttonBack.setActionCommand(resourceMap.getString("buttonBack.actionCommand")); // NOI18N
        buttonBack.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED))));
        buttonBack.setFocusPainted(false);
        buttonBack.setName("buttonBack"); // NOI18N
        buttonBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonBackActionPerformed(evt);
            }
        });
        panelButtons.add(buttonBack);

        panelCentre.setBorder(new javax.swing.border.MatteBorder(null));
        panelCentre.setName("panelCentre"); // NOI18N
        panelCentre.setOpaque(false);

        panelMain.setBorder(new javax.swing.border.MatteBorder(null));
        panelMain.setFont(resourceMap.getFont("panelMain.font")); // NOI18N
        panelMain.setName("panelMain"); // NOI18N
        panelMain.setOpaque(false);

        javax.swing.GroupLayout panelMainLayout = new javax.swing.GroupLayout(panelMain);
        panelMain.setLayout(panelMainLayout);
        panelMainLayout.setHorizontalGroup(
                panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        panelMainLayout.setVerticalGroup(
                panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 451, Short.MAX_VALUE)
        );

        panelLock.setBorder(new javax.swing.border.MatteBorder(null));
        panelLock.setName("panelLock"); // NOI18N
        panelLock.setOpaque(false);

        labelLock.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelLock.setText(resourceMap.getString("labelLock.text")); // NOI18N
        labelLock.setName("labelLock"); // NOI18N

        javax.swing.GroupLayout panelLockLayout = new javax.swing.GroupLayout(panelLock);
        panelLock.setLayout(panelLockLayout);
        panelLockLayout.setHorizontalGroup(
                panelLockLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelLockLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(labelLock, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        panelLockLayout.setVerticalGroup(
                panelLockLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelLockLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(labelLock, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
                                .addContainerGap())
        );

        buttonInfo.setFont(resourceMap.getFont("buttonInfo.font")); // NOI18N
        buttonInfo.setText(resourceMap.getString("buttonInfo.text")); // NOI18N
        buttonInfo.setActionCommand(resourceMap.getString("buttonInfo.actionCommand")); // NOI18N
        buttonInfo.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED)));
        buttonInfo.setFocusPainted(false);
        buttonInfo.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonInfo.setName("buttonInfo"); // NOI18N
        buttonInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonInfoActionPerformed(evt);
            }
        });

        buttonResponse.setFont(resourceMap.getFont("buttonResponse.font")); // NOI18N
        buttonResponse.setText(resourceMap.getString("buttonResponse.text")); // NOI18N
        buttonResponse.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED)));
        buttonResponse.setFocusPainted(false);
        buttonResponse.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonResponse.setName("buttonResponse"); // NOI18N
        buttonResponse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResponseActionPerformed(evt);
            }
        });

        panelLngs.setBorder(new javax.swing.border.MatteBorder(null));
        panelLngs.setName("panelLngs"); // NOI18N
        panelLngs.setOpaque(false);

        javax.swing.GroupLayout panelLngsLayout = new javax.swing.GroupLayout(panelLngs);
        panelLngs.setLayout(panelLngsLayout);
        panelLngsLayout.setHorizontalGroup(
                panelLngsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        panelLngsLayout.setVerticalGroup(
                panelLngsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 36, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout panelCentreLayout = new javax.swing.GroupLayout(panelCentre);
        panelCentre.setLayout(panelCentreLayout);
        panelCentreLayout.setHorizontalGroup(
                panelCentreLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelCentreLayout.createSequentialGroup()
                                .addGroup(panelCentreLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelCentreLayout.createSequentialGroup()
                                                .addGap(20, 20, 20)
                                                .addComponent(panelMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addComponent(panelLngs, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(panelLock, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(18, 18, 18)
                                .addGroup(panelCentreLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(buttonResponse, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(buttonInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap())
        );
        panelCentreLayout.setVerticalGroup(
                panelCentreLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelCentreLayout.createSequentialGroup()
                                .addComponent(buttonInfo, javax.swing.GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addComponent(buttonResponse, javax.swing.GroupLayout.DEFAULT_SIZE, 267, Short.MAX_VALUE))
                        .addGroup(panelCentreLayout.createSequentialGroup()
                                .addComponent(panelLngs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(panelMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addComponent(panelLock, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelForPaging.setBorder(new javax.swing.border.MatteBorder(null));
        panelForPaging.setName("panelForPaging"); // NOI18N
        panelForPaging.setOpaque(false);

        buttonBackPage.setFont(resourceMap.getFont("buttonForwardPage.font")); // NOI18N
        buttonBackPage.setIcon(resourceMap.getIcon("buttonBackPage.icon")); // NOI18N
        buttonBackPage.setText(resourceMap.getString("buttonBackPage.text")); // NOI18N
        buttonBackPage.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED))));
        buttonBackPage.setFocusPainted(false);
        buttonBackPage.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        buttonBackPage.setName("buttonBackPage"); // NOI18N
        buttonBackPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonBackPageActionPerformed(evt);
            }
        });

        buttonForwardPage.setFont(resourceMap.getFont("buttonForwardPage.font")); // NOI18N
        buttonForwardPage.setIcon(resourceMap.getIcon("buttonForwardPage.icon")); // NOI18N
        buttonForwardPage.setText(resourceMap.getString("buttonForwardPage.text")); // NOI18N
        buttonForwardPage.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED))));
        buttonForwardPage.setFocusPainted(false);
        buttonForwardPage.setName("buttonForwardPage"); // NOI18N
        buttonForwardPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonForwardPageActionPerformed(evt);
            }
        });

        labelForwardPage.setFont(resourceMap.getFont("labelForwardPage.font")); // NOI18N
        labelForwardPage.setText(resourceMap.getString("labelForwardPage.text")); // NOI18N
        labelForwardPage.setName("labelForwardPage"); // NOI18N

        labelBackPage.setFont(resourceMap.getFont("labelBackPage.font")); // NOI18N
        labelBackPage.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        labelBackPage.setText(resourceMap.getString("labelBackPage.text")); // NOI18N
        labelBackPage.setName("labelBackPage"); // NOI18N

        javax.swing.GroupLayout panelForPagingLayout = new javax.swing.GroupLayout(panelForPaging);
        panelForPaging.setLayout(panelForPagingLayout);
        panelForPagingLayout.setHorizontalGroup(
                panelForPagingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelForPagingLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(labelBackPage)
                                .addGap(18, 18, 18)
                                .addComponent(buttonBackPage, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(buttonForwardPage, javax.swing.GroupLayout.PREFERRED_SIZE, 231, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(labelForwardPage)
                                .addContainerGap())
        );
        panelForPagingLayout.setVerticalGroup(
                panelForPagingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelForPagingLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelForPagingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(labelForwardPage, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 63, Short.MAX_VALUE)
                                        .addComponent(labelBackPage, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 63, Short.MAX_VALUE)
                                        .addComponent(buttonBackPage, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 63, Short.MAX_VALUE)
                                        .addComponent(buttonForwardPage, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 63, Short.MAX_VALUE))
                                .addContainerGap())
        );

        javax.swing.GroupLayout panelBackgroundLayout = new javax.swing.GroupLayout(panelBackground);
        panelBackground.setLayout(panelBackgroundLayout);
        panelBackgroundLayout.setHorizontalGroup(
                panelBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panelCaption, javax.swing.GroupLayout.DEFAULT_SIZE, 1140, Short.MAX_VALUE)
                        .addGroup(panelBackgroundLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(panelButtons, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
                        .addComponent(panelForPaging, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(panelCentre, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        panelBackgroundLayout.setVerticalGroup(
                panelBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelBackgroundLayout.createSequentialGroup()
                                .addComponent(panelCaption, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelCentre, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelForPaging, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panelBackground, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panelBackground, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void changeTextToLocale() {
        final org.jdesktop.application.ResourceMap resourceMap =
                org.jdesktop.application.Application.getInstance(ru.apertum.qsystem.QSystem.class).getContext().getResourceMap(FWelcome.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        labelCaption.setText(resourceMap.getString("labelCaption.text")); // NOI18N
        buttonAdvance.setText("<html><p align=center>" + resourceMap.getString(advanceRegim ? "lable.reg_calcel" : "lable.adv_reg")); // NOI18N
        buttonStandAdvance.setText(resourceMap.getString("buttonStandAdvance.text")); // NOI18N
        buttonToBegin.setText(resourceMap.getString("buttonToBegin.text")); // NOI18N
        buttonBack.setText(resourceMap.getString("buttonBack.text")); // NOI18N
        labelLock.setText(resourceMap.getString("labelLock.text")); // NOI18N
        buttonInfo.setText("".equals(WelcomeParams.getInstance().infoHtml) ? resourceMap.getString("buttonInfo.text") : WelcomeParams.getInstance().infoHtml); // NOI18N 
        buttonResponse.setText("".equals(WelcomeParams.getInstance().responseHtml) ? resourceMap.getString("buttonResponse.text") : WelcomeParams.getInstance().responseHtml);
        buttonBackPage.setText(resourceMap.getString("buttonBackPage.text")); // NOI18N
        buttonForwardPage.setText(resourceMap.getString("buttonForwardPage.text")); // NOI18N
        labelBackPage.setText(resourceMap.getString("labelBackPage.text")); // NOI18N
        labelForwardPage.setText(resourceMap.getString("labelForwardPage.text")); // NOI18N
    }

    private void buttonBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonBackActionPerformed
        if (!current.equals(root)) {
            showButtons(current.getParent(), panelMain);
        }
    }//GEN-LAST:event_buttonBackActionPerformed

    private void buttonToBeginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonToBeginActionPerformed
        if (!current.equals(root)) {
            showButtons(root, panelMain);
            current = root;
        }
    }//GEN-LAST:event_buttonToBeginActionPerformed

    private static boolean advanceRegim = false;

    public static boolean isAdvanceRegim() {
        return advanceRegim;
    }

    /**
     * Переключение режима постановки в очередь и предварительной записи.
     *
     * @param advanceRegim true - предварительная запись, false - встать в очередь
     */
    public void setAdvanceRegim(boolean advanceRegim) {
        FWelcome.advanceRegim = advanceRegim;
        // \|/ нарисуем кнопки первого экрана // раньше тут был buttonToBeginActionPerformed(null)
        showButtons(root, panelMain);
        current = root;
        // /|\ нарисуем кнопки первого экрана // раньше тут был buttonToBeginActionPerformed(null)
        if (advanceRegim) {
            labelCaption.setText(WelcomeParams.getInstance().patternPickAdvanceTitle
                    .replace("dialog_text.part1", msg("messages.select_adv_servece1"))
                    .replace("dialog_text.part2", msg("messages.select_adv_servece2")));

            buttonAdvance.setText("<html><p align=center>" + msg("lable.reg_calcel"));
            //возврат в начальное состояние из диалога предварительной записи.
            if (clockBack.isActive()) {
                clockBack.stop();
            }
            clockBack.start();
        } else {
            if (clockBack.isActive()) {
                clockBack.stop();
            }
            labelCaption.setText(root.getButtonText());
            buttonAdvance.setText("<html><p align=center>" + msg("lable.adv_reg"));
        }
        //кнопка регистрации пришедших которые записались давно видна только в стандартном режиме и вместе с кнопкой предварительной записи
        if (buttonAdvance.isVisible()) {
            buttonStandAdvance.setVisible(!advanceRegim);
        }
    }

    /**
     * Заставка на некоторый таймаут.
     *
     * @param text      текст на заставке
     * @param imagePath картинка на заставке
     * @return Таймер.
     */
    public ATalkingClock showDelayFormPrint(String text, String imagePath) {
        setVisibleButtons(false);
        ATalkingClock clock = new ATalkingClock(WelcomeParams.getInstance().delayPrint, 1) {

            @Override
            public void run() {
                setVisibleButtons(true);
                showButtons(root, panelMain);
                showMed();
            }
        };
        clock.start();
        clearPanel(panelMain);
        panelMain.setLayout(new GridLayout(1, 1, 50, 1));
        panelMain.add(labelInfo);
        labelInfo.setText(text);
        labelInfo.setHorizontalAlignment(JLabel.CENTER);
        labelInfo.setVerticalAlignment(JLabel.BOTTOM);
        labelInfo.setVerticalTextPosition(SwingConstants.TOP);
        labelInfo.setHorizontalTextPosition(SwingConstants.CENTER);
        labelInfo.setIconTextGap(45);

        labelInfo.setIcon(new File(imagePath).exists() ? new ImageIcon(imagePath) : new ImageIcon(getClass().getResource(imagePath)));

        panelMain.repaint();
        labelInfo.repaint();
        return clock;
    }

    private final JLabel labelInfo = new JLabel();

    private void buttonAdvanceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAdvanceActionPerformed
        setAdvanceRegim(!isAdvanceRegim());
        if (isMed && !isAdvanceRegim()) {
            showMed();
        }
        showButtons(root, panelMain);
    }//GEN-LAST:event_buttonAdvanceActionPerformed

    private void buttonStandAdvanceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStandAdvanceActionPerformed
        final RpcStandInService res = FStandAdvance.showAdvanceStandDialog(this, true, FWelcome.netProperty, true, WelcomeParams.getInstance().delayBack * 2);

        if (res != null) {

            if (res.getMethod() == null) {// костыль. тут приедет текст запрета если нельзя встать в очередь

                showDelayFormPrint("<HTML><b><p align=center><span style='font-size:50.0pt;color:green'>" + msg("ticket.get_caption") + "<br></span>"
                                + "<span style='font-size:60.0pt;color:blue'>" + msg("ticket.get_caption_number") + "<br></span>"
                                + "<span style='font-size:100.0pt;color:blue'>" + res.getResult().getFullNumber() + "</span></p>",
                        Uses.firstMonitor.getDefaultConfiguration().getBounds().height > 900 || this.getHeight() > 900
                                ? "/ru/apertum/qsystem/client/forms/resources/getTicket.png"
                                : "/ru/apertum/qsystem/client/forms/resources/getTicketSmall.png");

                QLog.l().logger().info("Печать этикетки.");

                new Thread(() -> {
                    FWelcome.caption = root.getTextToLocale(QService.Field.NAME);
                    FWelcome.printTicket(res.getResult());
                }).start();
            } else {
                showDelayFormPrint("<HTML><b><p align=center><span style='font-size:60.0pt;color:red'>" + res.getMethod(),
                        "/ru/apertum/qsystem/client/forms/resources/noActive.png");
            }
        }
    }//GEN-LAST:event_buttonStandAdvanceActionPerformed

    private void buttonResponseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResponseActionPerformed
        if (WelcomeParams.getInstance().responseURL != null) {
            FInfoDialogWeb.showInfoDialogWeb(this, true, true, WelcomeParams.getInstance().delayBack * 4, WelcomeParams.getInstance().responseURL);
        } else {
            final QRespItem res = FResponseDialog.showResponseDialog(this, getResponse(), true, true, WelcomeParams.getInstance().delayBack * 2);
            if (res != null) {
                NetCommander.setResponseAnswer(netProperty, res, null, null, null, "");
            }
        }
    }//GEN-LAST:event_buttonResponseActionPerformed

    private void buttonInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInfoActionPerformed
        if (WelcomeParams.getInstance().infoURL != null) {
            FInfoDialogWeb.showInfoDialogWeb(this, true, true, WelcomeParams.getInstance().delayBack * 4, WelcomeParams.getInstance().infoURL);
        } else {
            FInfoDialog.showInfoDialog(this, getInfoTree(), true, true, WelcomeParams.getInstance().delayBack * 3);
        }
    }//GEN-LAST:event_buttonInfoActionPerformed

    private void buttonBackPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonBackPageActionPerformed
        if (pageNumber > 0) {
            pageNumber--;
            showButtons(current, panelMain);
            buttonBackPage.setEnabled(pageNumber > 0);
        }
    }//GEN-LAST:event_buttonBackPageActionPerformed

    private void buttonForwardPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonForwardPageActionPerformed
        pageNumber++;
        showButtons(current, panelMain);
        buttonBackPage.setEnabled(pageNumber > 0);
    }//GEN-LAST:event_buttonForwardPageActionPerformed

    private final LinkedList<Long> clicks = new LinkedList<>();

    private void panelButtonsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panelButtonsMouseClicked
        if (clicks.isEmpty()) {
            clicks.add(new Date().getTime());
            return;
        }
        final long now = new Date().getTime();
        if (now - clicks.getLast() < 500) {
            clicks.add(now);
        } else {
            if (now - clicks.getLast() > 5000 && now - clicks.getLast() < 10000 && clicks.size() == 10) {
                final BackDoor bd = new BackDoor(this, false);
                bd.setVisible(true);
                clicks.clear();
            } else {

                clicks.clear();
                clicks.add(now);
            }
        }
    }//GEN-LAST:event_panelButtonsMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAdvance;
    private javax.swing.JButton buttonBack;
    private javax.swing.JButton buttonBackPage;
    private javax.swing.JButton buttonForwardPage;
    private javax.swing.JButton buttonInfo;
    private javax.swing.JButton buttonResponse;
    private javax.swing.JButton buttonStandAdvance;
    private javax.swing.JButton buttonToBegin;
    private javax.swing.JLabel labelBackPage;
    private javax.swing.JLabel labelCaption;
    private javax.swing.JLabel labelForwardPage;
    private javax.swing.JLabel labelLock;
    private javax.swing.JPanel panelBackground;
    private javax.swing.JPanel panelButtons;
    private javax.swing.JPanel panelCaption;
    private javax.swing.JPanel panelCentre;
    private javax.swing.JPanel panelForPaging;
    private javax.swing.JPanel panelLngs;
    private javax.swing.JPanel panelLock;
    private javax.swing.JPanel panelMain;
    // End of variables declaration//GEN-END:variables
}
