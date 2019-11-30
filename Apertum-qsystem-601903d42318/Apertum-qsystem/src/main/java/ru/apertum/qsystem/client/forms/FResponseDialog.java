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

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import ru.apertum.qsystem.QSystem;
import ru.apertum.qsystem.client.common.WelcomeParams;
import ru.apertum.qsystem.client.model.QButton;
import ru.apertum.qsystem.client.model.QPanel;
import ru.apertum.qsystem.common.BrowserFX;
import ru.apertum.qsystem.common.QConfig;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.model.ATalkingClock;
import ru.apertum.qsystem.server.model.response.QRespItem;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Диалог оставления отзыва.
 *
 * @author Evgeniy Egorov
 */
@SuppressWarnings({"squid:S3776", "squid:S1192", "squid:S1172", "squid:S1450", "squid:S1604", "squid:S1161", "squid:MaximumInheritanceDepth"})
public class FResponseDialog extends javax.swing.JDialog {

    private static FResponseDialog respDialog;
    private final java.awt.Frame parentFrame;

    /**
     * Creates new form FStandAdvance.
     */
    private FResponseDialog(java.awt.Frame parentFrame, boolean modal) {
        super(parentFrame, modal);
        initComponents();
        this.parentFrame = parentFrame;
        if (WelcomeParams.getInstance().btnFont != null) {
            buttonBack.setFont(WelcomeParams.getInstance().btnFont);
            buttonInRoot.setFont(WelcomeParams.getInstance().btnFont);
            jButton2.setFont(WelcomeParams.getInstance().btnFont);
        }

        //На верхней панели пункта регистрации, там где заголовок и картинка в углу, можно вывести вэб-контент по URL. Оставьте пустым если не требуется
        if (!WelcomeParams.getInstance().topURL.isEmpty()) {
            panelUp.removeAll();
            final BrowserFX bro = new BrowserFX();
            final GridLayout gl = new GridLayout(1, 1);
            panelUp.setLayout(gl);
            panelUp.add(bro);
            bro.load(Uses.prepareAbsolutPathForImg(WelcomeParams.getInstance().topURL));
        }
        // Раздвинем шапку так же как на главном экране.
        if (WelcomeParams.getInstance().topSize >= 0) {
            panelUp.setPreferredSize(new Dimension(panelUp.getWidth(), WelcomeParams.getInstance().topSize));
        }
    }

    private QRespItem result = null;
    private static int delay = 10000;

    public void setRoot(QRespItem rootEl) {
        root = rootEl;
        preLevel = rootEl;
    }

    /**
     * Корень справочной системы.
     */
    private QRespItem root;
    /**
     * Предыдущий уровень кнопок.
     */
    private QRespItem preLevel;
    /**
     * Текущий уровень кнопок.
     */
    private QRespItem level;
    private static ResourceMap localeMap = null;

    private static String getLocaleMessage(String key) {
        if (localeMap == null) {
            localeMap = Application.getInstance(QSystem.class).getContext().getResourceMap(FResponseDialog.class);
        }
        return localeMap.getString(key);
    }

    /**
     * Статический метод который показывает модально диалог выбора времени для предварительной записи клиентов.
     *
     * @param parent     фрейм относительно которого будет модальность
     * @param respList   XML-список возможных отзывов
     * @param modal      модальный диалог или нет
     * @param fullscreen растягивать форму на весь экран и прятать мышку или нет
     * @param delay      задержка перед скрытием диалога. если 0, то нет автозакрытия диалога
     * @return XML-описание результата предварительной записи, по сути это номерок. если null, то отказались от предварительной записи
     */
    public static QRespItem showResponseDialog(Frame parent, QRespItem respList, boolean modal, boolean fullscreen, int delay) {
        FResponseDialog.delay = delay;
        QLog.l().logger().info("Выбор отзыва");
        if (respDialog == null) {
            respDialog = new FResponseDialog(parent, modal);
            respDialog.setTitle(getLocaleMessage("dialog.title"));
        }
        respDialog.changeTextToLocale(respList);
        respDialog.setRoot(respList);
        respDialog.result = null;
        Uses.setLocation(respDialog);
        respDialog.changeTextToLocale(respList);
        if (!(QConfig.cfg().isDebug() || QConfig.cfg().isDemo() && !fullscreen)) {
            Uses.setFullSize(respDialog);
            if (QConfig.cfg().isHideCursor()) {
                int[] pixels = new int[16 * 16];
                Image image = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16, 16, pixels, 0, 16));
                Cursor transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0, 0), "invisibleCursor");
                respDialog.setCursor(transparentCursor);
            }
        } else {
            respDialog.setSize(FWelcome.getScreenHorizontal(), FWelcome.getScreenVertical());
            Uses.setLocation(respDialog, parent);
        }
        respDialog.showLevel(respDialog.root);
        if (respDialog.clockBack.isActive()) {
            respDialog.clockBack.stop();
        }
        if (respDialog.clockBack.getInterval() > 1000) {
            respDialog.clockBack.start();
        }
        respDialog.setVisible(true);
        return respDialog.result;
    }

    private void showLevel(QRespItem level) {
        panelMain.removeAll();
        panelMain.repaint();
        if (level.getParent() == null && level != root) {
            level.setParent(level);
        }
        this.level = level;
        if (level.isLeaf() || level.getChildCount() == 1) {
            final JLabel label = new JLabel(Uses.prepareAbsolutPathForImg(level.isLeaf() ? level.getHtmlText() : level.getChildren().getFirst().getHtmlText()));
            label.setBackground(Color.WHITE);
            label.setOpaque(true);
            label.setBorder(new LineBorder(Color.GRAY, 10));
            label.setHorizontalAlignment(JLabel.CENTER);
            final GridLayout gl = new GridLayout();
            panelMain.setLayout(gl);
            panelMain.add(label);
            label.setBounds(-1, -1, panelMain.getWidth() + 2, panelMain.getHeight() + 2);
        } else {
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
            int cols = 1;
            int rows = level.getChildCount() + (level.getInputCaption().trim().isEmpty() ? 0 : 1);

            panelMain.setLayout(new GridLayout(rows < 5 ? 5 : rows, cols, delta, delta / 2));
            if (!level.getInputCaption().trim().isEmpty()) {
                final FResponseDialog.RespButton button = new FResponseDialog.RespButton(parentFrame, level, null);
                button.addActionListener(null);
                button.setOpaque(false);
                button.setContentAreaFilled(false);
                button.setBorderPainted(false);
                button.setFocusable(false);
                panelMain.add(button);
            }
            level.getChildren().stream().map(item -> new FResponseDialog.RespButton(parentFrame, item, WelcomeParams.getInstance().buttonType)).forEachOrdered(button ->
                panelMain.add(button)
            );
            if (level != root) {
                preLevel = level.getParent();
            } else {
                preLevel = root;
            }
            setSize(getWidth() + getSwitch(), getHeight());
        }
    }

    private static int switcher = 1;

    private static int getSwitch() {
        switcher = (-1) * switcher;
        return switcher;
    }

    /**
     * Эта ботва для кнпки. Картинка на кнопке рисуемая.
     */
    private static Image backgroundImage;
    private static final HashMap<String, Image> IMGS = new HashMap<>();

    private final class RespButton extends JButton {

        final Long id;
        final QRespItem el;

        public RespButton(Frame parent, QRespItem item, String resourceName) {
            this.el = item;
            id = item.getId();
            setFocusPainted(false);
            setText(!item.isLeaf() && resourceName == null ? item.getInputCaption() : item.getHtmlText());
            setBorder(new CompoundBorder(new BevelBorder(BevelBorder.RAISED), new BevelBorder(BevelBorder.RAISED)));
            addActionListener((ActionEvent e) -> {
                if (el.isLeaf()) {
                    //Если услуга требует ввода данных пользователем, то нужно получить эти данные из диалога ввода, т.к. потом при постановки в очередь предварительных
                    // нет ввода данных, только номера регистрации.
                    String inputData = null;
                    if (el.getInputRequired()) {
                        inputData = FInputDialog.showInputDialog(parent, true, FWelcome.getNetProperty(), false, WelcomeParams.getInstance().delayBack, el.getInputCaption());
                    }
                    FTimedDialog.showTimedDialog(parent, true, el.getHtmlText(), 1000);
                    el.data = inputData;
                    result = el;
                    respDialog.setVisible(false);
                } else {
                    respDialog.showLevel(el);
                }
            });

            // Нарисуем картинку на кнопке если надо. Загрузить можно из файла или ресурса
            if (resourceName == null || "".equals(resourceName)) {
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
                            QLog.l().logger().error(ex);
                        }
                    } else {
                        byte[] b = null;
                        try (final DataInputStream inStream = new DataInputStream(getClass().getResourceAsStream(resourceName))) {
                            b = new byte[inStream.available()];
                            inStream.readFully(b);
                        } catch (IOException ex) {
                            backgroundImage = null;
                            QLog.l().logger().error(ex);
                        }
                        backgroundImage = new ImageIcon(b).getImage();
                        IMGS.put(resourceName, backgroundImage);
                    }
                }
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
            if (!item.isRoot() && resourceName != null && WelcomeParams.getInstance().responseImg) {
                if (item.isLeaf()) {
                    setIcon(new ImageIcon(getClass().getResource("/ru/apertum/qsystem/client/forms/resources/feedback.png")));
                } else {
                    setIcon(new ImageIcon(getClass().getResource("/ru/apertum/qsystem/client/forms/resources/feedback_group.png")));
                }
            }
        }

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
            final BufferedImage resizedImage = new BufferedImage(biggerWidth, biggerHeight, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D graphics = resizedImage.createGraphics();

            graphics.setComposite(AlphaComposite.Src);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            graphics.drawImage(originalImage, 0, 0, biggerWidth, biggerHeight, this);
            graphics.dispose();

            return resizedImage;
        }
    }

    /**
     * Таймер, по которому будем выходить в корень меню.
     */
    private transient ATalkingClock clockBack = new ATalkingClock(delay, 1) {

        @Override
        public void run() {
            setVisible(false);
        }
    };

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelAll = new QPanel(WelcomeParams.getInstance().getBackgroundImg("feedback"));
        panelUp = new QPanel(WelcomeParams.getInstance().topImgSecondary);
        labelCaption = new javax.swing.JLabel();
        panelBottom = new ru.apertum.qsystem.client.model.QPanel();
        jButton2 = new QButton(WelcomeParams.getInstance().servButtonType);
        buttonBack = new QButton(WelcomeParams.getInstance().servButtonType);
        buttonInRoot = new QButton(WelcomeParams.getInstance().servButtonType);
        panelMain = new ru.apertum.qsystem.client.model.QPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setUndecorated(true);

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance().getContext().getResourceMap(FResponseDialog.class);
        panelAll.setBackground(resourceMap.getColor("panelAll.background")); // NOI18N
        panelAll.setName("panelAll"); // NOI18N

        panelUp.setBorder(new javax.swing.border.MatteBorder(null));
        panelUp.setCycle(java.lang.Boolean.FALSE);
        panelUp.setEndColor(resourceMap.getColor("panelUpRespDlg.endColor")); // NOI18N
        panelUp.setEndPoint(new java.awt.Point(0, 70));
        panelUp.setName("panelUpRespDlg"); // NOI18N
        panelUp.setOpaque(false);
        panelUp.setPreferredSize(new java.awt.Dimension(969, 150));
        panelUp.setStartColor(resourceMap.getColor("panelUpRespDlg.startColor")); // NOI18N
        panelUp.setStartPoint(new java.awt.Point(0, -50));

        labelCaption.setFont(resourceMap.getFont("labelCaption.font")); // NOI18N
        labelCaption.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelCaption.setText(resourceMap.getString("labelCaption.text")); // NOI18N
        labelCaption.setName("labelCaption"); // NOI18N

        javax.swing.GroupLayout panelUpLayout = new javax.swing.GroupLayout(panelUp);
        panelUp.setLayout(panelUpLayout);
        panelUpLayout.setHorizontalGroup(
            panelUpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(labelCaption, javax.swing.GroupLayout.DEFAULT_SIZE, 1071, Short.MAX_VALUE)
        );
        panelUpLayout.setVerticalGroup(
            panelUpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(labelCaption, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
        );

        panelBottom.setBorder(new javax.swing.border.MatteBorder(null));
        panelBottom.setEndPoint(new java.awt.Point(0, 100));
        panelBottom.setName("panelBottom"); // NOI18N
        panelBottom.setOpaque(false);
        panelBottom.setStartColor(resourceMap.getColor("panelBottom.startColor")); // NOI18N

        jButton2.setFont(resourceMap.getFont("jButton2.font")); // NOI18N
        jButton2.setIcon(resourceMap.getIcon("jButton2.icon")); // NOI18N
        jButton2.setText(resourceMap.getString("jButton2.text")); // NOI18N
        jButton2.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED)));
        jButton2.setFocusPainted(false);
        jButton2.setName("jButton2"); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        buttonBack.setFont(resourceMap.getFont("buttonBack.font")); // NOI18N
        buttonBack.setIcon(resourceMap.getIcon("buttonBack.icon")); // NOI18N
        buttonBack.setText(resourceMap.getString("buttonBack.text")); // NOI18N
        buttonBack.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED)));
        buttonBack.setFocusPainted(false);
        buttonBack.setName("buttonBack"); // NOI18N
        buttonBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonBackActionPerformed(evt);
            }
        });

        buttonInRoot.setFont(resourceMap.getFont("buttonInRoot.font")); // NOI18N
        buttonInRoot.setIcon(resourceMap.getIcon("buttonInRoot.icon")); // NOI18N
        buttonInRoot.setText(resourceMap.getString("buttonInRoot.text")); // NOI18N
        buttonInRoot.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED)));
        buttonInRoot.setFocusPainted(false);
        buttonInRoot.setName("buttonInRoot"); // NOI18N
        buttonInRoot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonInRootActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelBottomLayout = new javax.swing.GroupLayout(panelBottom);
        panelBottom.setLayout(panelBottomLayout);
        panelBottomLayout.setHorizontalGroup(
            panelBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBottomLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonInRoot, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(buttonBack, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        panelBottomLayout.setVerticalGroup(
            panelBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBottomLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 78, Short.MAX_VALUE)
                    .addComponent(buttonBack, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonInRoot, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        panelMain.setBackground(resourceMap.getColor("panelMain.background")); // NOI18N
        panelMain.setBorder(new javax.swing.border.MatteBorder(null));
        panelMain.setName("panelMain"); // NOI18N
        panelMain.setOpaque(false);

        javax.swing.GroupLayout panelMainLayout = new javax.swing.GroupLayout(panelMain);
        panelMain.setLayout(panelMainLayout);
        panelMainLayout.setHorizontalGroup(
            panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 571, Short.MAX_VALUE)
        );
        panelMainLayout.setVerticalGroup(
            panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 462, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout panelAllLayout = new javax.swing.GroupLayout(panelAll);
        panelAll.setLayout(panelAllLayout);
        panelAllLayout.setHorizontalGroup(
            panelAllLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelUp, javax.swing.GroupLayout.DEFAULT_SIZE, 1073, Short.MAX_VALUE)
            .addComponent(panelBottom, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(panelAllLayout.createSequentialGroup()
                .addGap(250, 250, 250)
                .addComponent(panelMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(250, 250, 250))
        );
        panelAllLayout.setVerticalGroup(
            panelAllLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAllLayout.createSequentialGroup()
                .addComponent(panelUp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(panelMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(panelBottom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelAll, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelAll, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void changeTextToLocale(QRespItem respList) {
        final org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(ru.apertum.qsystem.QSystem.class).getContext().getResourceMap(FResponseDialog.class);
        labelCaption.setText(respList.getHtmlText() == null || respList.getHtmlText().isEmpty() ? resourceMap.getString("LabelCaption.text") : respList.getHtmlText()); // NOI18N
        jButton2.setText(resourceMap.getString("jButton2.text")); // NOI18N

        buttonInRoot.setText(resourceMap.getString("buttonInRoot.text")); // NOI18N
        buttonBack.setText(resourceMap.getString("buttonBack.text")); // NOI18N
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        result = null;
        if (clockBack.isActive()) {
            clockBack.stop();
        }
        setVisible(false);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void buttonBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonBackActionPerformed
        if (level.isLeaf() || level.getChildCount() == 1) {
            if (level.getParent() != null) {
                showLevel(level.getParent());
            }
        } else {
            showLevel(preLevel);
        }
        if (respDialog.clockBack.isActive()) {
            respDialog.clockBack.stop();
        }
        respDialog.clockBack.start();
    }//GEN-LAST:event_buttonBackActionPerformed

    private void buttonInRootActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInRootActionPerformed
        showLevel(root);
        if (respDialog.clockBack.isActive()) {
            respDialog.clockBack.stop();
        }
        respDialog.clockBack.start();
    }//GEN-LAST:event_buttonInRootActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonBack;
    private javax.swing.JButton buttonInRoot;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel labelCaption;
    private ru.apertum.qsystem.client.model.QPanel panelAll;
    private ru.apertum.qsystem.client.model.QPanel panelBottom;
    private ru.apertum.qsystem.client.model.QPanel panelMain;
    private ru.apertum.qsystem.client.model.QPanel panelUp;
    // End of variables declaration//GEN-END:variables
}
