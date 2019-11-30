/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.qsystem.client.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import ru.apertum.qsystem.client.Locales;
import ru.apertum.qsystem.client.QProperties;
import ru.apertum.qsystem.client.common.ClientNetProperty;
import ru.apertum.qsystem.client.forms.FIndicatorBoard;
import ru.apertum.qsystem.common.AUdpServer;
import ru.apertum.qsystem.common.NetCommander;
import ru.apertum.qsystem.common.QConfig;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.QModule;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.cmd.RpcGetSelfSituation;
import ru.apertum.qsystem.common.exceptions.ClientException;
import ru.apertum.qsystem.common.exceptions.QException;
import ru.apertum.qsystem.common.exceptions.ServerException;
import ru.apertum.qsystem.common.model.IClientNetProperty;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.server.model.QUser;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import java.awt.HeadlessException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static ru.apertum.qsystem.client.forms.FWelcome.getScreenHorizontal;
import static ru.apertum.qsystem.client.forms.FWelcome.getScreenVertical;

/**
 * Приложение рабочего стола.
 *
 * @author Evgeniy Egorov
 */
public class Desktop extends Application {

    private DesktopController desktopController;

    /**
     * Описание того, кто залогинелся.
     */
    private QUser user;

    private Desktop.UDPServer udpServer;

    /**
     * Информация для взаимодействия по сети. Формируется по данным из командной строки.
     */
    private static IClientNetProperty netProperty;

    public static IClientNetProperty getNetProperty() {
        return netProperty;
    }

    /**
     * Приложение рабочего стола.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        QLog.initial(args, QModule.desktop);
        Locale.setDefault(Locales.getInstance().getLangCurrent());

        // Загрузка плагинов из папки plugins
        if (QConfig.cfg().isPlaginable()) {
            Uses.loadPlugins("./plugins/");
        }

        netProperty = new ClientNetProperty(args);
        //Загрузим серверные параметры
        boolean flag = false;
        try {
            QProperties.get().load(netProperty);
        } catch (QException e) {
            flag = true;
            throw new ClientException(e);
        } finally {
            if (flag) {
                System.exit(1);
            }
        }

        if (!QConfig.cfg().isTerminal()) {// в терминальном режиме запускаем много копий
            // Отсечем вторую копию.
            try {
                final DatagramSocket socket = new DatagramSocket(netProperty.getClientPort());
                socket.close();
            } catch (SocketException ex) {
                QLog.l().logger().error("Сервер UDP не запустился, вторая копия не позволяется.");
                JOptionPane.showMessageDialog(null, "Second start app is forbidden.", "Second start app", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            }
        }
        try {
            //Определим, надо ли выводить кастомера на второй экран.
            // Для этого должны быть переданы две координаты для определения этого монитора
            // -posx x -posy y
            if (new File(QConfig.cfg().getBoardCfgFile()).exists()) {
                initIndicatorBoard(QConfig.cfg().getBoardCfgFile());
            }

            launch(Desktop.class, args);
        } catch (HeadlessException ex) {
            QLog.l().logger().error("Ошибка при старте: ", ex);
            System.exit(0);
        }
    }

    /**
     * Табло вывода кастомера.
     */
    private static FIndicatorBoard indicatorBoard = null;

    /**
     * Создадим форму, спозиционируем, сконфигурируем и покажем.
     *
     * @param cfgFile файл конфигурации табло, приезжает из параметров консоли
     */
    private static void initIndicatorBoard(final String cfgFile) {
        File f = new File(cfgFile);
        if (indicatorBoard == null && f.exists()) {

            java.awt.EventQueue.invokeLater(() -> {
                Element root = null;
                try {
                    root = new SAXReader(false).read(cfgFile).getRootElement();
                } catch (DocumentException ex) {
                    throw new ServerException("Не создали клиентское табло.", ex);
                }
                indicatorBoard = FIndicatorBoard.getIndicatorBoard(root, false);
                if (indicatorBoard != null) {
                    try {
                        indicatorBoard.setIconImage(ImageIO.read(FIndicatorBoard.class.getResource("/ru/apertum/qsystem/client/forms/resources/client.png")));
                    } catch (IOException ex) {
                        throw new ServerException(ex);
                    }
                    indicatorBoard.toPosition(QConfig.cfg().isDebug(), Integer.parseInt(root.attributeValue("x", "0")), Integer.parseInt(root.attributeValue("y", "0")));
                    indicatorBoard.setVisible(true);
                }
            });
        }
    }

    @Override
    public void stop() {
        udpServer.stop();
    }

    @Override
    public void start(Stage stageDesktop) {
        stageDesktop.setTitle("Desktop");
        final LinkedList<QUser> users;
        try {
            users = NetCommander.getUsers(netProperty);
        } catch (QException e) {
            QLog.l().logger().error("Error desktop start.", e);
            JOptionPane.showMessageDialog(null,
                    "Init was failed. " + e,
                    "Init",
                    JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(e);
        }

        /*
        ****
         Логинимся.
        ****
         */
        final Stage loginStage = new Stage(StageStyle.UNDECORATED);
        Group rootGroup = new Group();
        Scene loginScene = new Scene(rootGroup);
        loginStage.setScene(loginScene);
        loginStage.setTitle("Login");
        loginStage.centerOnScreen();
        loginStage.setAlwaysOnTop(true);

        final ControllerAndView<LoginController, Parent> loginCav = new ControllerAndView<>("Login.fxml", loginStage, loginScene);
        rootGroup.getChildren().add(loginCav.getView());
        final LoginController loginController = loginCav.getController();
        loginController.init(netProperty, users);
        loginStage.setAlwaysOnTop(true);
        loginStage.showAndWait();
        user = loginController.getUser();
        if (user == null) {
            return;
        }
        /*
        ****
         Залогинились.
        ****
         *

         *
        ****
         Показываем рабочий стол.
        ****
         */
        stageDesktop.getIcons().add(new Image(getClass().getResourceAsStream("/ru/apertum/qsystem/client/forms/resources/client.png")));

        final ControllerAndView<DesktopController, Parent> desktopCav = new ControllerAndView<>("Desktop.fxml", stageDesktop, null);
        desktopController = desktopCav.getController();
        desktopController.setUser(user);
        desktopController.init(netProperty, desktopController);
        desktopController.refreshSituation(true);

        if (QConfig.cfg().isDebug()) {
            stageDesktop.setWidth(getScreenHorizontal());
            stageDesktop.setHeight(getScreenVertical() / 2d);
        } else {
            stageDesktop.setFullScreen(true);
            if (!QConfig.cfg().isDemo()) {
                stageDesktop.setAlwaysOnTop(true);
                // спрячем курсор мыши
                stageDesktop.getScene().setCursor(javafx.scene.Cursor.NONE);
            }
        }
        stageDesktop.centerOnScreen();
        stageDesktop.show();

        // стартуем UDP сервер для обнаружения изменения состояния очередей
        udpServer = new Desktop.UDPServer(getNetProperty().getClientPort());
        udpServer.start2();
    }

    /**
     * UDP Сервер. Обнаруживает изменение состояния очередей.
     */
    protected final class UDPServer extends AUdpServer {

        public UDPServer(int port) {
            super(port);
        }

        private Timer timer;
        private String pref = "";

        public void start2() {

            Thread thread = new Thread(this::start);
            thread.setDaemon(true);
            thread.start();

            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            if (!isActivate() && QConfig.cfg().isTerminal()) { // порт не занялся, т.к. в терминальном режиме, то нужно подсасывать мессаги из файла
                QLog.l().logger().trace("Старт PIPE.");
                if (!new File(Uses.TEMP_FOLDER + File.separator + "pipe").exists()) {
                    new File(Uses.TEMP_FOLDER + File.separator).mkdir();
                    try (PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(Uses.TEMP_FOLDER + File.separator + "pipe")), true)) {
                        w.println("" + new Date().getTime() + "^");
                    } catch (IOException ex) {
                        throw new ServerException(ex);
                    }
                }

                if (timer != null) {
                    timer.stop();
                }
                final AtomicInteger integer = new AtomicInteger();
                timer = new Timer(1000, (java.awt.event.ActionEvent e) -> {
                    try (final RandomAccessFile raf2 = new RandomAccessFile(Uses.TEMP_FOLDER + File.separator + "pipe", "r")) {
                        raf2.seek(0);
                        final String s = raf2.readLine();
                        final String[] ss = s.split("\\^");
                        if (ss.length == 2 && !pref.equals(ss[0])) {
                            pref = ss[0];
                            getData2(ss[1]);
                        }
                    } catch (IOException ex) {
                        QLog.l().logger().error("Не прочитался pipe. ", ex);
                    }

                    if (integer.get() > 180) {
                        integer.set(0);
                        checkPort();
                    } else {
                        integer.incrementAndGet();
                    }
                });
                timer.start();
            }
        }

        private void checkPort() {
            if (timer != null) {
                timer.stop();
            }
            start2();
        }

        @Override
        protected synchronized void getData(String data, InetAddress clientAddress, int clientPort) {
            if (QConfig.cfg().isTerminal()) {
                try {
                    if (!new File(Uses.TEMP_FOLDER + File.separator + "pipe").exists()) {
                        new File(Uses.TEMP_FOLDER + File.separator).mkdir();
                        try (final PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(Uses.TEMP_FOLDER + File.separator + "pipe")), true)) {
                            w.println("" + new Date().getTime() + "^");
                        }
                    }
                    try (final PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(Uses.TEMP_FOLDER + File.separator + "pipe")), true)) {
                        w.println("" + new Date().getTime() + "^" + data);
                    }
                } catch (IOException ex) {
                    QLog.l().logger().error("Не записался пакет UDP в pipe. ", ex);
                }
            }
            getData2(data);
        }

        protected void getData2(String data) {
            //Определяем, по нашей ли услуге пришел кастомер
            boolean my = false;
            for (RpcGetSelfSituation.SelfService srv : desktopController.getUserPlan().getSelfservices()) {
                if (String.valueOf(srv.getId()).equals(data)) {
                    my = true;
                }
            }
            //Если кастомер встал в очередь, обрабатываемую этим юзером, то апдейтим состояние очередей.
            if (my || user.getId().toString().equals(data)) {
                //Получаем состояние очередей для юзера
                Platform.runLater(() -> desktopController.refreshSituation());
                return;
            }
            if (Uses.TASK_REFRESH_POSTPONED_POOL.equals(data)) {
                //Получаем состояние пула отложенных
                LinkedList<QCustomer> custs = NetCommander.getPostponedPoolInfo(netProperty);
                LinkedList<QCustomer> rem = new LinkedList<>();
                custs.stream().filter(cust -> (cust.getIsMine() != null && !cust.getIsMine().equals(user.getId()))).forEach(rem::add);
                custs.removeAll(rem);
                //  todo  listPostponed.setModel(QPostponedList.getInstance().loadPostponedList(custs))...
            }
        }
    }
}
