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
package ru.apertum.qsystem.client.model;

import ru.apertum.qsystem.client.Locales;
import ru.apertum.qsystem.client.common.WelcomeParams;
import ru.apertum.qsystem.client.forms.FAdvanceCalendar;
import ru.apertum.qsystem.client.forms.FConfirmationStart2;
import ru.apertum.qsystem.client.forms.FInfoDialogWeb;
import ru.apertum.qsystem.client.forms.FInputDialog;
import ru.apertum.qsystem.client.forms.FPreInfoDialog;
import ru.apertum.qsystem.client.forms.FTimedDialog;
import ru.apertum.qsystem.client.forms.FWelcome;
import ru.apertum.qsystem.common.NetCommander;
import ru.apertum.qsystem.common.QConfig;
import ru.apertum.qsystem.common.SoundPlayer;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.cmd.RpcGetServiceState.ServiceState;
import ru.apertum.qsystem.common.model.ATalkingClock;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.extra.IWelcome;
import ru.apertum.qsystem.server.model.QAdvanceCustomer;
import ru.apertum.qsystem.server.model.QAuthorizationCustomer;
import ru.apertum.qsystem.server.model.QService;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.apertum.qsystem.client.forms.FWelcome.msg;
import static ru.apertum.qsystem.common.QLog.log;

/**
 * Сдесь реализован класс кнопки пользователя при выборе услуги. Класс кнопки пользователя при выборе услуги. Кнопка умеет слать задание на сервер для
 * постановки в очередь.
 *
 * @author Evgeniy Egorov
 */
public class QButton extends JButton {

    /**
     * Услуга, висящая на кнопке.
     */
    private final QService service;
    /**
     * Маркировка сайта, который соотверствует услуге, которая висит на этой кнопке.
     */
    private final FWelcome form;
    private final JPanel parentPanel;

    public FWelcome getForm() {
        return form;
    }

    public JPanel getParentPanel() {
        return parentPanel;
    }

    /**
     * Состояния кнопок.
     */
    private final boolean isActive;

    public boolean isIsActive() {
        return isActive;
    }

    private final boolean isVisible;

    public boolean isIsVisible() {
        return isVisible;
    }

    private final boolean isForPrereg;
    private final boolean isNotForPrereg;
    private boolean isDummy = false;

    public boolean isIsForPrereg() {
        return isForPrereg;
    }

    /**
     * Состояние услуги. 1 - доступна, 0 - недоступна, -1 - невидима, 2 - только для предвариловки, 3 - заглушка, 4 - не для предвариловки, 5 - рулон.
     */
    private static final int FOR_DUMMY = 3;
    private static final int FOR_PREREG = 2;
    private static final int NOT_FOR_PREREG = 4;
    private static final int NO_ACTIVE = 0;
    private static final int NO_VISIBLE = -1;
    private static final HashMap<String, Image> IMGS = new HashMap<>();

    /**
     * Сдесь реализован класс кнопки пользователя при выборе услуги.
     */
    public QButton() {
        service = null;
        form = null;
        parentPanel = null;
        isActive = true;
        isVisible = true;
        isForPrereg = false;
        isNotForPrereg = false;
    }

    /**
     * Сдесь реализован класс кнопки пользователя при выборе услуги.
     *
     * @param resourceName картинка подложки.
     */
    public QButton(String resourceName) {
        service = null;
        form = null;
        parentPanel = null;
        isActive = true;
        isVisible = true;
        isForPrereg = false;
        isNotForPrereg = false;

        init(resourceName);
    }

    /**
     * займемся внешним видом.
     */
    private void lookAndFeel() {
        // либо просто стандартная кнопка, либо картинка на кнопке если она есть
        if (backgroundImage == null) {
            setBorder(new CompoundBorder(new BevelBorder(BevelBorder.RAISED), new BevelBorder(BevelBorder.RAISED)));
        } else {
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
        }
    }

    /**
     * Здесь реализован класс кнопки пользователя при выборе услуги.
     *
     * @param service         висит услуга.
     * @param frm             на киоске.
     * @param prt             лежит на панели.
     * @param bkgResourceName картинка подложки.
     */
    public QButton(final QService service, FWelcome frm, JPanel prt, String bkgResourceName) {
        super();
        this.form = frm;
        this.service = service;
        this.parentPanel = prt;

        // посмотрим доступна ли данная услуга или группа услуг
        isVisible = NO_VISIBLE != service.getStatus();
        isActive = NO_ACTIVE != service.getStatus() && isVisible;
        isForPrereg = FOR_PREREG == service.getStatus() && isActive;
        isNotForPrereg = NOT_FOR_PREREG == service.getStatus() && isActive;

        init(service, bkgResourceName, null);
    }

    /**
     * Здесь реализован класс кнопки пользователя при выборе услуги.
     *
     * @param service          висит услуга.
     * @param frm              на киоске.
     * @param prt              лежит на панели.
     * @param bkgResourceName  картинка подложки.
     * @param iconResourceName картинка иконки на кнопке.
     */
    public QButton(final QService service, FWelcome frm, JPanel prt, String bkgResourceName, String iconResourceName) {
        super();
        this.service = service;
        this.form = frm;
        this.parentPanel = prt;

        // посмотрим доступна ли данная услуга или группа услуг
        isVisible = NO_VISIBLE != service.getStatus();
        isActive = NO_ACTIVE != service.getStatus() && isVisible;
        isForPrereg = FOR_PREREG == service.getStatus() && isActive;
        isNotForPrereg = NOT_FOR_PREREG == service.getStatus() && isActive;

        init(service, bkgResourceName, iconResourceName);
    }

    /**
     * Небольшой и необязательный пока костылик. Если что, то его можно использовать как перекладсик или хранилище.
     */
    public final HashMap<String, Object> exchanger = new HashMap<>();

    private void init(String resourceName) {
        setFocusPainted(false);
        // Нарисуем картинку на кнопке если надо. Загрузить можно из файла или ресурса
        if ("".equals(resourceName)) {
            backgroundImage = null;
        } else {
            backgroundImage = IMGS.get(resourceName);
            if (backgroundImage == null) {
                File file = new File(resourceName);
                if (file.exists()) {
                    try {
                        backgroundImage = ImageIO.read(file);
                        IMGS.put(resourceName, backgroundImage);
                    } catch (IOException ex) {
                        backgroundImage = null;
                        log().error(ex);
                    }
                } else {
                    byte[] b = null;
                    try (final DataInputStream inStream = new DataInputStream(getClass().getResourceAsStream(resourceName))) {
                        b = new byte[inStream.available()];
                        inStream.readFully(b);
                    } catch (IOException ex) {
                        backgroundImage = null;
                        log().error(ex);
                    }
                    backgroundImage = new ImageIcon(b).getImage();
                    IMGS.put(resourceName, backgroundImage);
                }
            }
        }

        //займемся внешним видом
        lookAndFeel();
    }

    private void init(final QService service, String bkgResourceName, String iconResourceName) {

        setFocusPainted(false);

        isDummy = FOR_DUMMY == service.getStatus() && isActive;
        log().trace("Create button for \"" + service.getName() + "\" ID=" + service.getId() + " states:"
                + (isVisible ? " Visible" : " Hide") + (isActive ? " Active" : " Pasive") + (isForPrereg ? " ForPrereg" : " ForAll") + (isDummy ? " Dummy" : " Real"));
        if (!isVisible) {
            setVisible(false);
            return;
        }

        // Нарисуем картинку на кнопке если надо. Загрузить можно из файла или ресурса
        if (isDummy || "".equals(bkgResourceName)) {
            backgroundImage = null;
        } else {
            backgroundImage = IMGS.get(bkgResourceName);
            if (backgroundImage == null) {
                File file = new File(bkgResourceName);
                if (file.exists()) {
                    try {
                        backgroundImage = ImageIO.read(file);
                        IMGS.put(bkgResourceName, backgroundImage);
                    } catch (IOException ex) {
                        backgroundImage = null;
                        log().error(ex);
                    }
                } else {
                    byte[] b = null;
                    try (final DataInputStream inStream = new DataInputStream(getClass().getResourceAsStream(bkgResourceName))) {
                        b = new byte[inStream.available()];
                        inStream.readFully(b);
                    } catch (IOException ex) {
                        backgroundImage = null;
                        log().error(ex);
                    }
                    backgroundImage = new ImageIcon(b).getImage();
                    IMGS.put(bkgResourceName, backgroundImage);
                }
            }
        }

        refreshText();
        setSize(1, 1);
        if (WelcomeParams.getInstance().buttonImg) {
            if (iconResourceName != null && Files.exists(Paths.get(iconResourceName))) {
                setIcon(new ImageIcon(iconResourceName));
            } else {
                if (service.isLeaf()) {
                    setIcon(new ImageIcon(getClass().getResource("/ru/apertum/qsystem/client/forms/resources/serv_btn.png")));
                } else {
                    setIcon(new ImageIcon(getClass().getResource("/ru/apertum/qsystem/client/forms/resources/folder.png")));
                }
            }
        }

        // заглушка. не надо ей ничего отрисовывать кроме надписи на кнопки. и нажимать ее не надо
        if (isDummy) {
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            return;
        }

        //займемся внешним видом
        // либо просто стандартная кнопка, либо картинка на кнопке если она есть
        if (backgroundImage == null) {
            setBorder(new CompoundBorder(new BevelBorder(BevelBorder.RAISED), new BevelBorder(BevelBorder.RAISED)));
        } else {
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
        }

        addActionListener(new WelcomeButtonEvent());//addActionListener
    }

    private transient Image backgroundImage;

    @Override
    public void paintComponent(Graphics g) {
        if (backgroundImage != null) {
            //Image scaledImage = backgroundImage.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH); // это медленный вариант
            final Image scaledImage = resizeToBig(backgroundImage, getWidth(), getHeight());
            final Graphics2D g2 = (Graphics2D) g;
            g2.drawImage(scaledImage, 0, 0, null, null);
            super.paintComponent(g);
        } else {
            super.paintComponent(g);
        }
    }

    private Image resizeToBig(Image originalImage, int biggerWidth, int biggerHeight) {
        final BufferedImage bufferedImage = new BufferedImage(biggerWidth, biggerHeight, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = bufferedImage.createGraphics();

        graphics.setComposite(AlphaComposite.Src);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.drawImage(originalImage, 0, 0, biggerWidth, biggerHeight, this);
        graphics.dispose();

        return bufferedImage;
    }

    public final void refreshText() {
        setText(Uses.prepareAbsolutPathForImg(service.getTextToLocale(QService.Field.BUTTON_TEXT)));
    }


    /**
     * Все что происходит по нажатию кнопки.
     */
    private class WelcomeButtonEvent implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            log().info("Pressed button \"" + service.getName() + "\" ID=" + service.getId());
            for (final IWelcome event : ServiceLoader.load(IWelcome.class)) {
                log().info("Вызов SPI расширения. Описание: " + event.getDescription());
                try {
                    final boolean buttonPressed = event.buttonPressed(QButton.this, service);
                    if (!buttonPressed) {
                        log().info("Вызов SPI расширения. Отмена продолжения обработки нажатия: " + event.getDescription());
                        return;
                    }
                } catch (Exception tr) {
                    log().error("Вызов SPI расширения завершился ошибкой. Описание: " + tr);
                }
            }
            try {
                // "Услуги" и "Группа" это одно и тоже.
                if (!service.isLeaf()) {
                    if (form.getClockBack().isActive()) {
                        form.getClockBack().stop();
                    }
                    if (form.getClockBack().getInterval() > 999) {
                        form.getClockBack().start();
                    }
                    // группа может быть не активна
                    if (isActive) {
                        form.showButtons(service, (JPanel) getParent());
                    } else {
                        final String msg = WelcomeParams.getInstance().patternInfoDialog.replace("dialog.message", msg("qbutton.right_naw_can_not"));
                        FTimedDialog.showTimedDialog(form, true, msg, 3000);
                        return;
                    }
                }
                if (service.isLeaf()) {// отсюда и до конца :)

                    // просто если в тексте предварительного чтива URL, то надо показать этот УРЛ и не ставить в очередь. Пусть почитает и далее сам решит что и зачем без чтива.
                    final String pattern = "(file|http|ftp|https):\\/\\/\\/*[\\w\\-_:\\/]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?";
                    final String txt = Uses.prepareAbsolutPathForImg(service.getPreInfoHtml().trim());
                    Pattern replace = Pattern.compile(pattern);
                    Matcher matcher = replace.matcher(txt);
                    if (isActive && txt != null && !txt.isEmpty()
                            && (matcher.matches() || txt.contains("localhost") || txt.contains("127.0.0.1"))) {
                        FInfoDialogWeb.showInfoDialogWeb(QButton.this.form, true, true, WelcomeParams.getInstance().delayBack * 4, txt);
                        return;
                    }

                    //  в зависимости от активности формируем сообщение и шлем запрос на сервер об статистике
                    if (isActive) {
                        // Услуга активна. Посмотрим не предварительная ли это запись.
                        // Если Предварительная запись, то пытаемся предватительно встать и выходим из обработке кнопки.
                        if (FWelcome.isAdvanceRegim()) {

                            /*
                             * Если предвариловка запрещена, только возможна обычное оказание услуги, то просто заканчиваем с сообщением этого файта
                             */
                            if (isNotForPrereg) {
                                form.lockWelcome(WelcomeParams.getInstance().patternInfoDialog.replace("dialog.message", msg("messages.not_for_prereg")));
                                form.getClockUnlockBack().start();
                                return;
                            }

                            form.setAdvanceRegim(false);

                            //Если услуга требует ввода данных пользователем, то нужно получить эти данные из диалога ввода, т.к. потом при постановки в очередь предварительных
                            // нет ввода данных, только номера регистрации.
                            String inputData = null;
                            if (service.getInputRequired()) {
                                inputData = FInputDialog.showInputDialog(form, true, FWelcome.getNetProperty(), false, WelcomeParams.getInstance().delayBack,
                                        service.getTextToLocale(QService.Field.INPUT_CAPTION));
                                if (inputData == null) {
                                    return;
                                }
                            }

                            final QAdvanceCustomer res = FAdvanceCalendar.showCalendar(form, true, FWelcome.getNetProperty(), service, true,
                                    WelcomeParams.getInstance().delayBack * 2, form.getAdvancedCustomer(), inputData, "");
                            //Если res == null значит отказались от выбора
                            if (res == null) {
                                form.showMed();
                                return;
                            }

                            // приложим введенное клиентом чтобы потом напечатать.
                            if (service.getInputRequired()) {
                                res.setAuthorizationCustomer(new QAuthorizationCustomer(inputData));
                            }

                            for (final IWelcome event : ServiceLoader.load(IWelcome.class)) {
                                log().info("Вызов SPI расширения. Описание: " + event.getDescription());
                                try {
                                    event.readyNewAdvCustomer(QButton.this, res, service);
                                } catch (Exception tr) {
                                    log().error("Вызов SPI расширения завершился ошибкой. Описание: " + tr);
                                }
                            }

                            //вешаем заставку
                            final GregorianCalendar gcTime = new GregorianCalendar();
                            gcTime.setTime(res.getAdvanceTime());
                            int hours = gcTime.get(GregorianCalendar.HOUR_OF_DAY);
                            String minutes = ("" + gcTime.get(GregorianCalendar.MINUTE) + "0000").substring(0, 2);
                            if (hours == 0) {
                                hours = 24;
                                gcTime.add(GregorianCalendar.HOUR_OF_DAY, -1);
                            }
                            String dateStr = (Locales.getInstance().isRuss()
                                    ? Uses.getRusDate(gcTime.getTime(), Locales.DATE_FORMAT_FULL)
                                    : Locales.getInstance().formatForPrintShort.format(gcTime.getTime()));
                            form.showDelayFormPrint(WelcomeParams.getInstance().patternDelayFormPrintTitle
                                            .replace("take_adv_ticket", msg("qbutton.take_adv_ticket"))
                                            .replace("format_dd_MM_yyyy_time", dateStr)
                                            .replace("adv_ticket_come_to", msg("qbutton.take_adv_ticket_come_to"))
                                            .replace("HOUR_OF_DAY_MINUTE", " " + hours + ":" + minutes + " "),
                                    WelcomeParams.getInstance().getTicketImg);
                            // печатаем результат
                            new Thread(() -> {
                                log().info("Печать этикетки бронирования.");
                                FWelcome.printTicketAdvance(res);
                            }).start();
                            // выходим, т.к. вся логика предварительной записи в форме предварительного календаря
                            return;
                        }

                        /*
                         * Если только возможна предвариловка, то просто заканчиваем с сообщением этого файта
                         */
                        if (isForPrereg) {
                            form.lockWelcome(WelcomeParams.getInstance().patternInfoDialog.replace("dialog.message", msg("messages.only_for_prereg")));
                            form.getClockUnlockBack().start();
                            return;
                        }

                        // Отсюда действие по нажатия кнопки чтоб просто встать в очередь
                        // Узнать, есть ли информация для прочнения в этой услуге.
                        // Если текст информации не пустой, то показать диалог сэтим текстом
                        // У диалога должны быть кнопки "Встать в очередь", "Печать", "Отказаться".
                        // если есть текст, то показываем диалог
                        if (service.getPreInfoHtml() != null && !service.getPreInfoHtml().isEmpty()) {

                            // поддержка расширяемости плагинами
                            // покажим преинфо из плагинов
                            boolean flag = true;
                            for (final IWelcome event : ServiceLoader.load(IWelcome.class)) {
                                log().info("Вызов SPI расширения. Описание: " + event.getDescription());
                                boolean f = false;
                                try {
                                    f = event.showPreInfoDialog(QButton.this, form, FWelcome.getNetProperty(), service.getTextToLocale(QService.Field.PRE_INFO_HTML),
                                            service.getTextToLocale(QService.Field.PRE_INFO_PRINT_TEXT), true, true, WelcomeParams.getInstance().delayBack * 2);
                                } catch (Exception tr) {
                                    log().error("Вызов SPI расширения завершился ошибкой. Описание: " + tr);
                                }
                                flag = flag && f;
                            }

                            if (!flag || !FPreInfoDialog.showPreInfoDialog(form, service.getTextToLocale(QService.Field.PRE_INFO_HTML),
                                    service.getTextToLocale(QService.Field.PRE_INFO_PRINT_TEXT), true, true, WelcomeParams.getInstance().delayBack * 2)) {
                                // выходим т.к. кастомер отказался продолжать
                                return;
                            }
                        }

                        // Если режим инфокиоска, то сразу уходим, т.к. вставать в очередь нет нужды
                        // Показали информацию и все
                        if (FWelcome.isInfo() || QConfig.cfg().isWelcomeInfo()) {
                            return;
                        }

                        // узнать статистику по предлагаемой услуги и спросить потенциального кастомера
                        // будет ли он стоять или нет
                        final ServiceState servState;
                        try {
                            servState = NetCommander.aboutService(FWelcome.getNetProperty(), service.getId());
                        } catch (Exception ex) {
                            // гасим жестоко, пользователю незачем видеть ошибки. выставим блокировку
                            log().error("Гасим жестоко. Невозможно отправить команду на сервер. ", ex);
                            form.lockWelcome(FWelcome.LOCK_MESSAGE);
                            return;
                        }
                        // Если приехал текст причины, то покажем ее и не дадим встать в очередь
                        if (servState.getCode() == Uses.NOT_POSSIBLE_IN_LINE && servState.getMessage() != null && !"".equals(servState.getMessage())) {
                            form.lockWelcome(WelcomeParams.getInstance().patternInfoDialog.replace("dialog.message", servState.getMessage()));
                            form.getClockUnlockBack().start();
                            return;
                        }
                        // Если услуга не обрабатывается ни одним пользователем то в count вернется Uses.LOCK_INT
                        // вот трех еще потерплю, а больше низачто!
                        if (servState.getCode() == Uses.LOCK_INT) {
                            form.lockWelcome(WelcomeParams.getInstance().patternInfoDialog.replace("dialog.message", msg("qbutton.service_not_available")));
                            form.getClockUnlockBack().start();
                            return;
                        }
                        if (servState.getCode() == Uses.LOCK_FREE_INT) {
                            form.lockWelcome(WelcomeParams.getInstance().patternInfoDialog.replace("dialog.message", msg("qbutton.service_not_available_by_schedule")));
                            form.getClockUnlockBack().start();
                            return;
                        }
                        if (servState.getCode() == Uses.LOCK_PER_DAY_INT) {
                            form.lockWelcome(WelcomeParams.getInstance().patternInfoDialog.replace("dialog.message", msg("qbutton.clients_enough")));
                            form.getClockUnlockBack().start();
                            return;
                        }
                        if (WelcomeParams.getInstance().askLimit < 1 || servState.getLenghtLine() >= WelcomeParams.getInstance().askLimit) {
                            // Выведем диалог о том будет чел сотять или пошлет нахер всю контору.
                            if (!FConfirmationStart2.getMayContinue(form, servState.getLenghtLine())) {
                                return;
                            }
                        }
                        // Если приехал текст позсказки, то покажем ее и дадим встать в очередь
                        if (servState.getCode() == Uses.NORMAL_RESPONCE && (servState.getMessage() != null || !"".equals(servState.getMessage()))) {
                            FPreInfoDialog.showPreInfoDialog(form, servState.getMessage(), null, true, true, WelcomeParams.getInstance().delayBack / 2, false);
                        }
                    }
                    // ну если неактивно, т.е. надо показать отказ, или продолжить вставать в очередь
                    if (form.getClockBack().isActive()) {
                        form.getClockBack().stop();//т.к. есть какой-то логический конец, то не надо в корень автоматом.
                    }

                    String inputData = null;
                    ATalkingClock clock;
                    if (!isActive) {
                        clock = form.showDelayFormPrint(WelcomeParams.getInstance().patternInfoDialog
                                .replace("dialog.message", msg("qbutton.right_naw_can_not")) + "</span>", "/ru/apertum/qsystem/client/forms/resources/noActive.png");
                    } else {
                        //Если услуга требует ввода данных пользователем, то нужно получить эти данные из диалога ввода
                        if (service.getInputRequired()) {

                            // поддержка расширяемости плагинами
                            // запросим ввода данных
                            String flag = null;
                            for (final IWelcome event : ServiceLoader.load(IWelcome.class)) {
                                log().info("Вызов SPI расширения. Описание: " + event.getDescription());
                                final String f;
                                try {
                                    f = event.showInputDialog(QButton.this, form, true, FWelcome.getNetProperty(), false, WelcomeParams.getInstance().delayBack,
                                            service.getTextToLocale(QService.Field.INPUT_CAPTION), service);
                                } catch (Exception tr) {
                                    log().error("Вызов SPI расширения завершился ошибкой. Описание: " + tr);
                                    return;
                                }
                                flag = (f == null ? flag : (flag == null ? f : (flag + " " + f)));
                            }
                            inputData = (flag == null ? FInputDialog.showInputDialog(form, true, FWelcome.getNetProperty(), false,
                                    WelcomeParams.getInstance().delayBack, service.getTextToLocale(QService.Field.INPUT_CAPTION))
                                    : flag);
                            if (inputData == null) {
                                return;
                            }
                            // если ввели, то нужно спросить у сервера есть ли возможность встать в очередь с такими введенными данными

                            //@return 1 - превышен, 0 - можно встать. 2 - забанен
                            int limitPersonOver;
                            try {
                                limitPersonOver = NetCommander.aboutServicePersonLimitOver(FWelcome.getNetProperty(), service.getId(), inputData);
                            } catch (Exception ex) {
                                // гасим жестоко, пользователю незачем видеть ошибки. выставим блокировку
                                log().error("Гасим жестоко опрос превышения лимита по введенным данным, но не лочим киоск. Невозможно отправить команду на сервер.", ex);
                                return;
                            }
                            if (limitPersonOver != 0) {
                                form.lockWelcome(limitPersonOver == 1
                                        ? WelcomeParams.getInstance().patternInfoDialog.replace("dialog.message", msg("qbutton.ticket_with_nom_finished"))
                                        : "<HTML><p align=center><b><span style='font-size:60.0pt;color:red'>" + msg("qbutton.denail_by_lost") + "</span></b></p>");
                                form.getClockUnlockBack().start();
                                return;
                            }
                        }
                        clock = form.showDelayFormPrint(WelcomeParams.getInstance().patternInfoDialog.replace("dialog.message", msg("qbutton.take_ticket")),
                                WelcomeParams.getInstance().getTicketImg);
                    }

                    //выполним задание если услуга активна
                    if (isActive) {
                        IWelcome.StandInParameters parameters = new IWelcome.StandInParameters(FWelcome.getNetProperty(), service.getId(), "1", 1, inputData);
                        // тут сделать обработчик параметров для постановки в очередь
                        for (final IWelcome event : ServiceLoader.load(IWelcome.class)) {
                            log().info("Вызов SPI расширения handleStandInParams. Описание: " + event.getDescription());
                            try {
                                parameters = event.handleStandInParams(QButton.this, parameters);
                            } catch (Exception tr) {
                                log().error("Вызов SPI расширения handleStandInParams завершился ошибкой. Описание: " + tr);
                            }
                        }
                        final QCustomer customer;
                        try {
                            customer = NetCommander.standInService(parameters.getNetProperty(), parameters.getServiceId(), parameters.getPassword(),
                                    parameters.getPriority(), parameters.getInputData());
                        } catch (Exception ex) {
                            // гасим жестоко, пользователю незачем видеть ошибки. выставим блокировку
                            log().error("Невозможно отправить команду на сервер. ", ex);
                            form.lockWelcome(FWelcome.LOCK_MESSAGE);
                            clock.stop();
                            return;
                        }
                        for (final IWelcome event : ServiceLoader.load(IWelcome.class)) {
                            log().info("Вызов SPI расширения. Описание: " + event.getDescription());
                            try {
                                event.readyNewCustomer(QButton.this, customer, service);
                            } catch (Exception tr) {
                                log().error("Вызов SPI расширения завершился ошибкой. Описание: " + tr);
                            }
                        }
                        clock.stop();
                        form.showDelayFormPrint(WelcomeParams.getInstance().patternGetTicket.replace("dialogue_text.take_ticket", msg("qbutton.take_ticket"))
                                        .replace("dialogue_text.your_nom", msg("qbutton.your_nom"))
                                        .replace("dialogue_text.number", customer.getFullNumber()),
                                WelcomeParams.getInstance().getTicketImg);

                        if (WelcomeParams.getInstance().isVoiceForTicketNumber) {
                            log().info("Проговорим номер голосом.");
                            final LinkedList<String> phrases = new LinkedList<>();
                            // путь к звуковым файлам
                            String path = SoundPlayer.SAMPLES_PACKAGE;
                            phrases.add(path + "ticket.wav");
                            phrases.addAll(SoundPlayer.toSoundSimple2(path, customer.getPrefix() + (customer.getNumber() < 1 ? "" : customer.getNumber())));
                            new Thread(() -> SoundPlayer.play(phrases)).start();
                        }

                        new Thread(() -> {
                            FWelcome.setCaption(((QService) service.getRoot()).getTextToLocale(QService.Field.NAME));
                            FWelcome.printTicket(customer);
                        }).start();
                    }
                }
            } catch (Exception ex) {
                log().error("Ошибка при попытки обработать нажатие кнопки постановки в ачередь. " + ex.toString());
            }
        }
    }
}
