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

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import ru.apertum.qsystem.QSystem;
import ru.apertum.qsystem.client.model.QPanel;
import ru.apertum.qsystem.common.BrowserFX;
import ru.apertum.qsystem.common.QConfig;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.RunningLabel;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.exceptions.ServerException;
import ru.apertum.qsystem.common.model.ATalkingClock;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.MatteBorder;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.apertum.qsystem.common.QLog.log;

/**
 * Created on 22 Сентябрь 2008 г., 14:27.
 *
 * @author Evgeniy Egorov
 */
@SuppressWarnings({"squid:S1192", "squid:S1172", "squid:S1450", "squid:S1604", "squid:S1161", "squid:MaximumInheritanceDepth"})
public class FIndicatorBoard extends javax.swing.JFrame {

    /**
     * Пустая строка.
     */
    public static final String LINE_EMPTY = "<HTML><b><p align=center><span style='font-size:150.0pt;color:green'></span><p><b>";
    /**
     * Фоновая картинка.
     */
    private transient Element topElement;
    private transient Element bottomElement;
    private transient Element bottomElement2;
    private transient Element leftElement;
    private transient Element rightElement;
    private transient Element mainElement;
    private static FIndicatorBoard indicatorBoard = null;
    protected FCallDialog callDialog;
    private transient Element root = null;
    protected boolean fractal;
    /**
     * Режим. Главное табло или клиентское.
     */
    protected static boolean isMain = true;

    /**
     * Получить форму табло. Получаем главное табло. Если требуется получить с указанием типа то использовать public static FIndicatorBoard
     * getIndicatorBoard(Element rootParams, boolean isMain)
     *
     * @param rootParams параметры табло.
     * @return
     */
    public static FIndicatorBoard getIndicatorBoard(Element rootParams) {
        if (!"1".equals(rootParams.attributeValue(Uses.TAG_BOARD_VISIBLE_PANEL)) && !rootParams.attributeValue(Uses.TAG_BOARD_VISIBLE_PANEL).startsWith("$")) {
            return null;
        }
        if (indicatorBoard == null || rootParams != indicatorBoard.root) {
            indicatorBoard = new FIndicatorBoard(rootParams, QConfig.cfg().isDebug(), false);
            indicatorBoard.loadConfig();
            indicatorBoard.root = rootParams;
        }
        if (indicatorBoard.monitor < 1) {
            return null;
        }
        return indicatorBoard;
    }

    /**
     * Получить форму табло.
     *
     * @param rootParams параметры табло.
     * @param isMain     режим. Главное или клиентское
     * @return
     */
    public static FIndicatorBoard getIndicatorBoard(Element rootParams, boolean isMain) {
        if (indicatorBoard != null && FIndicatorBoard.isMain != isMain) {
            FIndicatorBoard.isMain = isMain;
            indicatorBoard = null;
        }
        FIndicatorBoard.isMain = isMain;
        return getIndicatorBoard(rootParams);
    }

    /**
     * Получить форму табло. Получаем главное табло. Вызов этого метода создает новый объект. не использовать при одиночном табло. сделано для зонального.
     *
     * @param rootParams параметры табло.
     * @param isDebug    отладка.
     * @return форма.
     */
    public static FIndicatorBoard getIndicatorBoardForZone(Element rootParams, boolean isDebug) {
        if (!"1".equals(rootParams.attributeValue(Uses.TAG_BOARD_VISIBLE_PANEL))) {
            return null;
        }
        final FIndicatorBoard iBoard = new FIndicatorBoard(rootParams, isDebug, false);
        iBoard.loadConfig();
        iBoard.root = rootParams;
        iBoard.zoneDebug = isDebug;
        return iBoard;
    }

    protected boolean zoneDebug = false;


    /**
     * Конструктор формы с указанием количества строк на табло.
     *
     * @param rootParams настройки в xml. Если NULL, то не выполнится ничего из того, что в конструкторе.
     */
    protected FIndicatorBoard(Element rootParams, boolean isDebug, boolean fractal) {
        if (rootParams == null) {
            return;
        }

        log().info("Создаем окно для информации.");

        init(rootParams, isDebug, fractal);
        initComponents();
        spDown2.setOpaque(false);
        panelDown2.setOpaque(false);
        panelCommon.setBackground(bgColor);
        if (fractal) {
            panelMain.setVisible(false);
        }
        if (mainElement != null && !QConfig.cfg().getModule().isClient() && !QConfig.cfg().getModule().isDesktop()) {
            callDialog = new FCallDialog(this, false, mainElement);
        } else {
            callDialog = null;
        }
        log().trace("Прочитали настройки для окна информации.");
    }

    private void init(Element rootParams, boolean isDebug, boolean fractal) {
        this.fractal = fractal;
        topElement = rootParams.element(Uses.TAG_BOARD_TOP);
        bottomElement = rootParams.element(Uses.TAG_BOARD_BOTTOM);
        bottomElement2 = rootParams.element(Uses.TAG_BOARD_BOTTOM_2);
        leftElement = rootParams.element(Uses.TAG_BOARD_LEFT);
        rightElement = rootParams.element(Uses.TAG_BOARD_RIGHT);
        mainElement = rootParams.element(Uses.TAG_BOARD_MAIN);
        if (topElement == null || bottomElement == null || bottomElement2 == null
                || leftElement == null || rightElement == null || mainElement == null) {
            throw new ServerException("mainboard.xml is broken.");
        }
        // Проствим кол-во строк и др. параметры
        final ArrayList<Element> lst = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_MONITOR);
        this.monitor = lst.isEmpty() ? 100 : Integer.parseInt(lst.get(0).attributeValue(Uses.TAG_BOARD_VALUE));
        this.linesCount = Integer.parseInt(Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_LINES_COUNT).get(0).attributeValue(Uses.TAG_BOARD_VALUE));
        final int ii = Integer.parseInt(Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_COLS_COUNT).get(0).attributeValue(Uses.TAG_BOARD_VALUE));
        this.colsCount = isMain ? (ii > 0 ? (ii > 5 ? 5 : ii) : 1) : 1;
        this.pause = Integer.parseInt(Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_DELAY_VISIBLE).get(0).attributeValue(Uses.TAG_BOARD_VALUE));
        // Определим цвет табло
        this.bgColor = Color.decode("#" + Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FON_COLOR).get(0).attributeValue(Uses.TAG_BOARD_VALUE));
        this.fgColorCaprion =
                Color.decode("#" + Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FONT_COLOR_CAPTION).get(0).attributeValue(Uses.TAG_BOARD_VALUE));

        String clrs = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FONT_COLOR_LEFT).get(0).attributeValue(Uses.TAG_BOARD_VALUE);
        String[] cls = clrs.split("(\\s*,\\s*|\\s*;\\s*|\\s+)");
        this.fgColorLeft = new Color[cls.length];
        for (int i = 0; i < cls.length; i++) {
            fgColorLeft[i] = Color.decode("#" + (cls[i].trim().isEmpty() ? "F0F0F0" : cls[i].trim()));
        }

        clrs = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FONT_COLOR_RIGHT).get(0).attributeValue(Uses.TAG_BOARD_VALUE);
        cls = clrs.split("(\\s*,\\s*|\\s*;\\s*|\\s+)");
        this.fgColorRight = new Color[cls.length];
        for (int i = 0; i < cls.length; i++) {
            fgColorRight[i] = Color.decode("#" + (cls[i].trim().isEmpty() ? "F0F0F0" : cls[i].trim()));
        }

        this.borderLine = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_LINE_BORDER).get(0).attributeValue(Uses.TAG_BOARD_VALUE);
        this.delimiter = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_LINE_DELIMITER).get(0).attributeValue(Uses.TAG_BOARD_VALUE);
        this.leftPic = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_LEFT_PIC).isEmpty() ? "" :
                Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_LEFT_PIC).get(0).attributeValue(Uses.TAG_BOARD_VALUE);
        this.rightPic = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_RIGHT_PIC).isEmpty() ? "" :
                Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_RIGHT_PIC).get(0).attributeValue(Uses.TAG_BOARD_VALUE);
        this.extPic = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_EXT_PIC).isEmpty() ? "" :
                Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_EXT_PIC).get(0).attributeValue(Uses.TAG_BOARD_VALUE);

        // этим сделаем зебру из табло.
        clrs = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FONT_COLOR_LINE).get(0).attributeValue(Uses.TAG_BOARD_VALUE);
        if (clrs == null || clrs.isEmpty()) {
            this.colorFonLine = new Color[0];
        } else {
            cls = clrs.split("(\\s*,\\s*|\\s*;\\s*|\\s+)");
            this.colorFonLine = new Color[cls.length];
            for (int i = 0; i < cls.length; i++) {
                colorFonLine[i] = Color.decode("#" + (cls[i].trim().isEmpty() ? "F0F0F0" : cls[i].trim()));
            }
        }

        clrs = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_LINE_COLOR).get(0).attributeValue(Uses.TAG_BOARD_VALUE);
        cls = clrs.split("(\\s*,\\s*|\\s*;\\s*|\\s+)");
        this.colorRow = new Color[cls.length];
        for (int i = 0; i < cls.length; i++) {
            colorRow[i] = Color.decode("#" + (cls[i].trim().isEmpty() ? "F0F0F0" : cls[i].trim()));
        }

        final String rowCap = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_LINE_CAPTION).get(0).attributeValue(Uses.TAG_BOARD_VALUE);
        this.rowCaption = !"".equals(rowCap) ? rowCap : getLocaleMessage("line_caption");
        this.leftColCaption = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_LEFT_CAPTION).get(0).attributeValue(Uses.TAG_BOARD_VALUE);
        ArrayList<Element> li = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_RIGHT_CAPTION);
        this.rightColCaption = li.isEmpty() ? "" : li.get(0).attributeValue(Uses.TAG_BOARD_VALUE);
        li = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_EXT_CAPTION);
        final String adCap = li.isEmpty() ? "" : li.get(0).attributeValue(Uses.TAG_BOARD_VALUE);
        this.extColCaption = !"".equals(adCap) ? adCap : getLocaleMessage("additional_column_cap");
        li = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_EXT_POSITION);
        int extCollPos = Integer.parseInt(li.isEmpty() ? "0" : li.get(0).attributeValue(Uses.TAG_BOARD_VALUE));
        extCollPos = extCollPos < 0 ? 0 : extCollPos;
        extCollPos = extCollPos > 3 ? 3 : extCollPos;
        this.extColPosition = extCollPos;
        final String cap = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_GRID_NEXT_CAPTION).get(0).attributeValue(Uses.TAG_BOARD_VALUE);
        this.nextGridCaption = (!"".equals(cap) ? cap : getLocaleMessage("board.next_caption"));

        String[] borderLines = (borderLine.isEmpty() ? "0,0,0,0" : borderLine).split("\\s*;\\s*");
        this.border = new MatteBorder[100];
        String[] ss1 = borderLines[0].replaceAll("[\\D&&[^,;\\s]]", "").split("(\\s*,\\s*|\\s*;\\s*|\\s+)");
        border[0] = ss1.length < 4
                ? new MatteBorder(0, 0, 0, 0, colorRow[0])
                : new MatteBorder(Integer.parseInt(ss1[0]), Integer.parseInt(ss1[1]), Integer.parseInt(ss1[2]), Integer.parseInt(ss1[3]), colorRow[0]);
        for (int i = 0; i < 99; i++) {
            Color color = colorRow.length == 1 ? colorRow[0] : (colorRow.length == 2 ? colorRow[1] : colorRow[i % (colorRow.length - 1) + 1]);
            String[] ss = borderLines[borderLines.length == 1 ? 0 : (borderLines.length == 2 ? 1 : (i % (borderLines.length - 1) + 1))]
                    .replaceAll("[\\D&&[^,;\\s]]", "").split("(\\s*,\\s*|\\s*;\\s*|\\s+)");
            if (ss.length < 4) {
                log().warn("Bad parameters for Matte Border: \"" + Arrays.toString(ss) + "\"");
            }
            border[i + 1] = ss.length < 4
                    ? new MatteBorder(0, 0, 0, 0, color)
                    : new MatteBorder(Integer.parseInt(ss[0].trim()), Integer.parseInt(ss[1].trim()), Integer.parseInt(ss[2].trim()), Integer.parseInt(ss[3].trim()), color);
        }


        if (!isDebug) {
            setUndecorated(true);
            setType(Type.UTILITY);
        }
    }

    /**
     * Определить на монитор.
     *
     * @param isDebug режим отладки.
     * @param x       координата монитора.
     * @param y       координата монитора.
     */
    public void toPosition(boolean isDebug, int x, int y) {
        // Определим форму на монитор
        final Rectangle bounds = Uses.DISPLAYS.get(monitor);
        if (bounds != null) {
            x = bounds.x + 1;
            y = bounds.y + 1;
        }

        setLocation(x, y);
        setAlwaysOnTop(!isDebug);
        // Отрехтуем форму в зависимости от режима.
        if (!isDebug) {

            setAlwaysOnTop(true);
            // спрячем курсор мыши
            if (QConfig.cfg().isHideCursor()) {
                int[] pixels = new int[16 * 16];
                Image image = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16, 16, pixels, 0, 16));
                Cursor transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0, 0), "invisibleCursor");
                setCursor(transparentCursor);
            }
            setBounds(x, y, 200, 300);
            addWindowListener(new WindowAdapter() {

                @Override
                public void windowOpened(WindowEvent e) {
                    setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
            });
        } else {
            setSize(1280, 720);
        }
    }

    /**
     * Загрузка размеров областей табло.
     */
    protected void loadDividerLocation() {
        double down = 1;
        if ("1".equals(topElement.attributeValue(Uses.TAG_BOARD_VISIBLE_PANEL))) {
            double up = Double.parseDouble(topElement.attributeValue(Uses.TAG_BOARD_PANEL_SIZE));
            spUp.setDividerLocation(up);
            panelUp.refreshVideoSize();
        } else {
            panelUp.setVisible(false);
        }

        if ("0".equals(bottomElement2.attributeValue(Uses.TAG_BOARD_VISIBLE_PANEL)) && "0".equals(bottomElement.attributeValue(Uses.TAG_BOARD_VISIBLE_PANEL))) {
            panelDown.setVisible(false);
            panelDown2.setVisible(false);
            spDown.setDividerLocation(down);
        } else {
            if ("1".equals(bottomElement2.attributeValue(Uses.TAG_BOARD_VISIBLE_PANEL)) && "1".equals(bottomElement.attributeValue(Uses.TAG_BOARD_VISIBLE_PANEL))) {
                if ("1".equals(bottomElement.attributeValue(Uses.TAG_BOARD_VISIBLE_PANEL))) {
                    down = Double.parseDouble(bottomElement.attributeValue(Uses.TAG_BOARD_PANEL_SIZE));
                    spDown.setDividerLocation(down);
                    panelDown.refreshVideoSize();
                } else {
                    panelDown.setVisible(false);
                }
                if ("1".equals(bottomElement2.attributeValue(Uses.TAG_BOARD_VISIBLE_PANEL))) {
                    double down2 = Double.parseDouble(bottomElement2.attributeValue(Uses.TAG_BOARD_PANEL_SIZE));
                    spDown2.setDividerLocation(down2);
                    panelDown2.refreshVideoSize();
                } else {
                    panelDown2.setVisible(false);
                }
            } else {
                if ("1".equals(bottomElement.attributeValue(Uses.TAG_BOARD_VISIBLE_PANEL))) {
                    down = Double.parseDouble(bottomElement.attributeValue(Uses.TAG_BOARD_PANEL_SIZE))
                            + (1 - Double.parseDouble(bottomElement.attributeValue(Uses.TAG_BOARD_PANEL_SIZE)))
                            - (1 - Double.parseDouble(bottomElement.attributeValue(Uses.TAG_BOARD_PANEL_SIZE)))
                            * Double.parseDouble(bottomElement2.attributeValue(Uses.TAG_BOARD_PANEL_SIZE));
                    spDown.setDividerLocation(down);
                    spDown2.setDividerLocation(1);
                    panelDown2.setVisible(false);
                    panelDown.refreshVideoSize();
                } else {
                    down = Double.parseDouble(bottomElement.attributeValue(Uses.TAG_BOARD_PANEL_SIZE))
                            + (1 - Double.parseDouble(bottomElement.attributeValue(Uses.TAG_BOARD_PANEL_SIZE)))
                            - (1 - Double.parseDouble(bottomElement.attributeValue(Uses.TAG_BOARD_PANEL_SIZE)))
                            * (1 - Double.parseDouble(bottomElement2.attributeValue(Uses.TAG_BOARD_PANEL_SIZE)));
                    spDown.setDividerLocation(down);
                    spDown2.setDividerLocation(0);
                    panelDown.setVisible(false);
                    panelDown2.refreshVideoSize();
                }
            }
        }
        if ("1".equals(leftElement.attributeValue(Uses.TAG_BOARD_VISIBLE_PANEL))) {
            double left = Double.parseDouble(leftElement.attributeValue(Uses.TAG_BOARD_PANEL_SIZE));
            spLeft.setDividerLocation(left);
            panelLeft.refreshVideoSize();
        } else {
            panelLeft.setVisible(false);
        }
        if ("1".equals(rightElement.attributeValue(Uses.TAG_BOARD_VISIBLE_PANEL))) {
            double right = Double.parseDouble(rightElement.attributeValue(Uses.TAG_BOARD_PANEL_SIZE));
            spRight.setDividerLocation(right);
            panelRight.refreshVideoSize();
        } else {
            panelRight.setVisible(false);
        }
    }

    /**
     * Загрузка содержимого областей табло.
     */
    private void loadConfig() {
        //загрузим фоновый рисунок
        String filePath = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FON_IMG).get(0).attributeValue(Uses.TAG_BOARD_VALUE);
        File f = new File(filePath);
        if (f.exists()) {
            panelCommon.setBackgroundImgage(filePath);
        }

        nexts.clear();
        elNexts.clear();
        loadPanel(topElement, rlTop, panelUp);
        loadPanel(bottomElement, rlDown, panelDown);
        loadPanel(bottomElement2, rlDown2, panelDown2);
        loadPanel(leftElement, rlLeft, panelLeft);
        loadPanel(rightElement, rlRight, panelRight);
        if (!fractal) {
            showLines();
        }
    }

    private void loadPanel(Element params, RunningLabel label, QPanel panel) {
        if (!"1".equals(params.attributeValue(Uses.TAG_BOARD_VISIBLE_PANEL))) {
            return;
        }
        // цвет панельки
        label.setBackground(bgColor);
        //загрузим размер и цвет шрифта
        final Font font = new Font(getFontName(), label.getFont().getStyle(),
                Integer.parseInt(Uses.elementsByAttr(params, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FONT_SIZE).get(0).attributeValue(Uses.TAG_BOARD_VALUE)));
        label.setForeground(Color.decode("#" + (Uses.elementsByAttr(params, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FONT_COLOR).get(0).attributeValue(Uses.TAG_BOARD_VALUE))));
        label.setFont(font);

        //загрузим фоновый рисунок
        final String filePath = Uses.elementsByAttr(params, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FON_IMG).get(0).attributeValue(Uses.TAG_BOARD_VALUE);
        File fp = new File(filePath);
        if (fp.exists()) {
            label.setBackgroundImage(filePath);
        }

        //загрузим фрактал
        String fileFractalXml = "";
        if (!Uses.elementsByAttr(params, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FRACTAL).isEmpty()) {
            fileFractalXml = Uses.elementsByAttr(params, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FRACTAL).get(0).attributeValue(Uses.TAG_BOARD_VALUE);
        }
        final File frX = new File(fileFractalXml);
        if (frX.exists()) {
            panel.removeAll();
            panel.setLayout(new GridLayout(1, 1));
            final FIndicatorBoard fi;
            try {
                fi = new FIndicatorBoard(new SAXReader(false).read(frX).getRootElement(), true, true);
                fi.loadConfig();
                panel.add(fi.panelCommon);
            } catch (DocumentException ex) {
                QLog.l().logger().error(ex);
            }
        } else {

            //загрузим видео
            final String filePathVid = Uses.elementsByAttr(params, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_VIDEO_FILE).get(0).attributeValue(Uses.TAG_BOARD_VALUE);
            File fv = new File(filePathVid);
            if (fv.exists()) {
                label.setVisible(false);
                panel.setVideoFileName(filePathVid);
                panel.startVideo();
            } else { // если не видео, то простая дата или таблица ближайших
                if ("1".equals(Uses.elementsByAttr(params, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_SIMPLE_DATE).get(0).attributeValue(Uses.TAG_BOARD_VALUE))) {
                    label.setRunningText("");
                    label.setText("");
                    label.setShowTime(true);
                } else { // загрузим текст
                    if ("1".equals(Uses.elementsByAttr(params, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_GRID_NEXT).get(0).attributeValue(Uses.TAG_BOARD_VALUE))) {
                        // таблица ближайших
                        label.setVerticalAlignment(1);
                        label.setRunningText("");
                        label.setText("<HTML>"
                                + "<table  cellpadding='5' align='center' border='"
                                + Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_GRID_NEXT_FRAME_BORDER).get(0).attributeValue(Uses.TAG_BOARD_VALUE)
                                + "' bordercolor='0'>"
                                + "<tr><td>"
                                + "<p align=center>"
                                + "<span style='font-size:"
                                + Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FONT_SIZE_CAPTION).get(0).attributeValue(Uses.TAG_BOARD_VALUE)
                                + ".0pt;color:"
                                + Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FONT_COLOR_CAPTION).get(0).attributeValue(Uses.TAG_BOARD_VALUE) + ";'>"
                                + nextGridCaption
                                + "</span></p>"
                                + "</td></tr>"
                                + "<tr>"
                                + "</table>");
                        nexts.add(label);
                        elNexts.put(label, params);
                    } else {
                        final String rt = Uses.elementsByAttr(params, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_RUNNING_TEXT).get(0).attributeValue(Uses.TAG_BOARD_VALUE).trim();
                        if (!"".equals(rt)) {
                            label.setRunningText(rt);
                            label.setText("");
                            label.setSpeedRunningText(Integer.parseInt(Uses.elementsByAttr(params, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_SPEED_TEXT).get(0)
                                    .attributeValue(Uses.TAG_BOARD_VALUE)));
                            label.start();
                        } else {
                            // просто хтмл-текст или URL
                            final String txt = params.getTextTrim();
                            Pattern replace = Pattern.compile(PATTERN);
                            Matcher matcher = replace.matcher(txt);
                            if (new File(txt).exists() || matcher.matches() || txt.contains("localhost") || txt.contains("127.0.0.1")) {
                                panel.removeAll();
                                GridLayout gl = new GridLayout(1, 1);
                                panel.setLayout(gl);
                                BrowserFX bfx = new BrowserFX();
                                panel.add(bfx, BorderLayout.CENTER);
                                bfx.load(Uses.prepareAbsolutPathForImg(txt));
                            } else {
                                label.setText(Uses.prepareAbsolutPathForImg(txt));
                                label.setRunningText("");
                            }
                        }//бегущий
                    }//ближайшие//время//видео
                }
            }
        }// фрактал
    }

    protected static final String PATTERN = "(file|http|ftp|https):\\/\\/\\/*[\\w\\-_:\\/]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?";

    /**
     * это лейблы, в которых будут таблицы ближайших клиентосов.
     */
    protected final ArrayList<RunningLabel> nexts = new ArrayList<>();
    private final HashMap<RunningLabel, Element> elNexts = new HashMap<>();

    public boolean needNext() {
        return !nexts.isEmpty();
    }

    public boolean hideByStartWork() {
        return "1".equals(Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_HIDE_INVITE_BY_WORK).get(0).attributeValue(Uses.TAG_BOARD_VALUE));
    }

    /**
     * Показать ближайших.
     *
     * @param list список ближайших.
     */
    public void showNext(List<String> list) {
        nexts.forEach(rl -> {
            String grid = "<HTML>"
                    + "<table cellpadding='5' align='center' border='"
                    + Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_GRID_NEXT_FRAME_BORDER).get(0).attributeValue(Uses.TAG_BOARD_VALUE)
                    + "'>"
                    + "<tr><td colspan='" + Uses.elementsByAttr(elNexts.get(rl), Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_GRID_NEXT_COLS).get(0).attributeValue(Uses.TAG_BOARD_VALUE)
                    + "'>"
                    + "<p align=center>"
                    + "<span style='font-size:" + Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FONT_SIZE_CAPTION).get(0)
                    .attributeValue(Uses.TAG_BOARD_VALUE) + ".0pt;color:" + Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FONT_COLOR_CAPTION).get(0)
                    .attributeValue(Uses.TAG_BOARD_VALUE) + ";'>"
                    + nextGridCaption
                    + "</span></p>"
                    + "</td></tr>";
            int pos = 0;
            for (int i = 0; i < Integer.parseInt(Uses.elementsByAttr(elNexts.get(rl), Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_GRID_NEXT_ROWS).get(0)
                    .attributeValue(Uses.TAG_BOARD_VALUE)); i++) {
                grid = grid + "<tr>";
                for (int j = 0; j < Integer.parseInt(Uses.elementsByAttr(elNexts.get(rl), Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_GRID_NEXT_COLS).get(0)
                        .attributeValue(Uses.TAG_BOARD_VALUE)); j++) {
                    grid = grid + "<td>"
                            + "<p align=center><span style='font-size:" + Uses.elementsByAttr(elNexts.get(rl), Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FONT_SIZE).get(0)
                            .attributeValue(Uses.TAG_BOARD_VALUE) + ".0pt;color:" + Uses.elementsByAttr(elNexts.get(rl), Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FONT_COLOR).get(0)
                            .attributeValue(Uses.TAG_BOARD_VALUE) + ";'>"
                            + (pos < list.size() ? list.get(pos) : "")
                            + "</span></p>"
                            + "</td>";
                    pos++;
                }
                grid = grid + "</tr>";
            }
            grid = grid + "</table>";
            rl.setText(grid);
        });
    }

    private static ResourceMap localeMap = null;

    protected static String getLocaleMessage(String key) {
        if (localeMap == null) {
            localeMap = Application.getInstance(QSystem.class).getContext().getResourceMap(FIndicatorBoard.class);
        }
        return localeMap.getString(key);
    }

    private MatteBorder[] border;

    public interface ILine {
        /**
         * Опредилить строку вызова на табло.
         */
        public void setLineData(String number, String point, String extData);

        /**
         * Выставить количество миганий.
         *
         * @param count скоклько раз мигнуть.
         */
        public void setBlinkCount(int count);

        /**
         * Начать мигать.
         */
        public void startBlink();
    }

    /**
     * Эот строка на табло со всеми на ней элементами вызова.
     */
    private final class Line extends JPanel implements ILine {

        private final JLabel left;
        private final JLabel right;
        private JLabel ext;

        /**
         * Строка N-ная.
         *
         * @param n номер строки
         */
        public Line(int n) {
            super();
            if (colorFonLine.length > 1) {
                setOpaque(true);
                setBackground(colorFonLine[colorFonLine.length == 2 ? 1 : (n % (colorFonLine.length - 1) + 1)]);
            } else {
                setOpaque(false);
            }
            setBorder(border[(n > border.length - 3) ? 1 : n + 1]);
            left = new JLabel();
            final Font font = new Font(getFontName(), left.getFont().getStyle(),
                    Integer.parseInt(Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FONT_SIZE_LINE).get(0).attributeValue(Uses.TAG_BOARD_VALUE)));

            if (isMain && extColPosition > 0) {
                ext = new JLabel();
                ext.setFont(font);
                ext.setBackground(bgColor);
                ext.setForeground(fgColorRight[0]);
                ext.setHorizontalAlignment(JLabel.CENTER);
                ext.setVerticalAlignment(JLabel.CENTER);
                ext.setBounds(0, 0, 100, 100);
                ext.setText("");
                ext.setVisible(true);
            }

            setLayout(new GridLayout(1, isMain ? (extColPosition > 0 ? 3 : 2) : 1, 0, 0));
            setBounds(0, 0, 100, 100);

            if (isMain && extColPosition == 1) {
                add(ext);
            }

            left.setFont(font);
            left.setBackground(bgColor);
            left.setForeground(fgColorLeft[fgColorLeft.length == 1 ? 0 : (n % fgColorLeft.length)]);
            left.setHorizontalAlignment(JLabel.CENTER);
            left.setVerticalAlignment(JLabel.CENTER);
            add(left);
            left.setBounds(0, 0, 100, 100);
            left.setText("");
            left.setVisible(true);

            if (isMain) {
                if (extColPosition == 2) {
                    add(ext);
                }
                right = new JLabel();
                right.setFont(font);
                right.setBackground(bgColor);
                right.setForeground(fgColorRight[fgColorRight.length == 1 ? 0 : (n % fgColorRight.length)]);
                right.setHorizontalAlignment(JLabel.CENTER);
                right.setVerticalAlignment(JLabel.CENTER);
                add(right);
                right.setBounds(0, 0, 100, 100);
                right.setText("");
                right.setVisible(true);
                if (extColPosition == 3) {
                    add(ext);
                }
            } else {
                right = null;
            }

            final String delPic = Uses.prepareAbsolutPathForImg(delimiter);
            boolean delEx2;
            try {
                delEx2 = new File(new URI(delPic)).exists();
            } catch (URISyntaxException | IllegalArgumentException ex) {
                log().warn("Parameter of main tablo \"" + Uses.TAG_BOARD_LINE_DELIMITER + "\" is uncorrect URI or file not exists: \"" + delimiter + "\"(" + delPic + ")");
                delEx2 = false;
            }
            ImageIcon arrow2;
            if (delEx2) {
                try {
                    arrow2 = new javax.swing.ImageIcon(new URL(delPic));
                } catch (MalformedURLException ex) {
                    delEx2 = false;
                    arrow2 = null;
                }
            } else {
                arrow2 = null;
            }
            arrow = arrow2;
            delEx = delEx2;
        }

        private final boolean delEx;
        private final ImageIcon arrow;

        /**
         * Опредилить строку вызова на табло.
         */
        @Override
        public void setLineData(String number, String point, String extData) {
            if (isMain) {
                if (number == null || number.isEmpty()) {
                    left.setText("");
                } else {
                    left.setText(number);
                }
                if (point == null || point.isEmpty()) {
                    right.setText("");
                    right.setIcon(null);
                } else {
                    if (delimiter == null || delimiter.isEmpty()) {
                        right.setHorizontalAlignment(JLabel.CENTER);
                        right.setText(point);
                    } else {
                        right.setHorizontalAlignment(JLabel.LEFT);

                        if (delEx) {
                            right.setIcon(arrow);
                            right.setText(point);
                        } else {
                            right.setText(delimiter + " " + point);
                        }
                    }
                }
                if (ext != null) {
                    ext.setText(extData);
                }
            } else {
                left.setText(number);
            }
        }

        /**
         * blinkCount 0 - постоянное мигание, -1 не мигает. число - количество миганий.
         */
        int blinkCount = -1;

        /**
         * Выставить количество миганий.
         *
         * @param count скоклько раз мигнуть.
         */
        public void setBlinkCount(int count) {
            blinkCount = count;
            if (clock != null && clock.isActive()) {
                clock.stop();
                clock = null;
                left.setVisible(true);
                if (right != null) {
                    right.setVisible(true);
                }
                if (ext != null) {
                    ext.setVisible(true);
                }
            }
        }

        boolean vis = true;

        /**
         * Начать мигать.
         */
        public void startBlink() {
            if (clock != null && clock.isActive()) {
                clock.stop();
                clock = null;
            }
            if (blinkCount == -1) {
                left.setVisible(true);
                if (right != null) {
                    right.setVisible(true);
                }
                if (ext != null) {
                    ext.setVisible(true);
                }
                return;
            }
            vis = true;
            clock = new ATalkingClock(100, blinkCount) {

                private int iteration = 0;
                private static final int DARK = 3; //длительность погашеного состояния в циклах
                private static final int LIGHT = 8; //длительность горящего состояния в циклах
                private static final int RET = DARK + LIGHT;

                @Override
                public void run() {
                    if (iteration == DARK || iteration == RET) {
                        left.setVisible(vis);
                        if (right != null) {
                            right.setVisible(vis);
                        }
                        if (ext != null) {
                            ext.setVisible(vis);
                        }
                        vis = !vis;
                    }
                    if (iteration == RET) {
                        iteration = -1;
                    }
                    iteration++;
                }
            };
            clock.start();
        }

        private transient ATalkingClock clock = null;
    }

    /**
     * Массив контролов для вывода инфы.
     */
    protected final ArrayList<ILine> labels = new ArrayList<>();
    /**
     * Номер дополнительного монитора для табло.
     */
    protected int monitor = 100;
    /**
     * Количество выводимых строк.
     */
    private int linesCount;
    /**
     * Количество выводимых столбцов.
     */
    private int colsCount;
    /**
     * Цвет фона табло.
     */
    private Color bgColor;
    /**
     * Цвет шрифта заголовка.
     */
    private Color fgColorCaprion;
    /**
     * Цвет шрифта левого столбца.
     */
    private Color[] fgColorLeft;
    /**
     * Цвет шрифта правого столбца.
     */
    private Color[] fgColorRight;
    /**
     * Окантовка Строк.
     */
    private String borderLine;
    /**
     * Чем разделяются столбци клиента и пункта вызова на главном табло.
     */
    private String delimiter;
    /**
     * Иконка у заголовка левого столбца.
     */
    private String leftPic;
    /**
     * Иконка у заголовка правого столбца.
     */
    private String rightPic;
    /**
     * Иконка у заголовка доп. столбца.
     */
    private String extPic;
    /**
     * Цвет рамки строки табло.
     */
    private Color[] colorRow;
    /**
     * Заголовок строки табло.
     */
    private String rowCaption;
    /**
     * Заголовок левого столбца.
     */
    private String leftColCaption;
    /**
     * Заголовок правого столбца.
     */
    private String rightColCaption;
    private String nextGridCaption;

    private String extColCaption;
    private int extColPosition;
    /**
     * Цвет надписи строки табло.
     */
    private Color[] colorFonLine;

    public int getLinesCount() {
        return linesCount * colsCount;
    }

    /**
     * Минимальное время индикации на табло.
     */
    private int pause;

    public int getPause() {
        return pause;
    }

    private String fontName = null;

    private String getFontName() {
        if (fontName == null) {
            ArrayList<Element> are = Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FONT_NAME);
            fontName = are.isEmpty() ? null : are.get(0).attributeValue(Uses.TAG_BOARD_VALUE).trim();
            if (fontName == null || fontName.isEmpty()) {
                fontName = new JLabel().getFont().getName();
            }
        }
        return fontName;
    }

    /**
     * Создаем и расставляем контролы для строк по форме.
     */
    protected void showLines() {
        log().info("Показываем набор строк.");
        GridLayout la = new GridLayout(linesCount + (isMain ? 1 : 0), (isMain ? colsCount : 1), 10, 0);
        panelMain.setLayout(la);
        final ArrayList<JPanel> caps = new ArrayList<>();
        for (int i = 0; i < colsCount; i++) {
            if (isMain) {
                final JPanel panelCap = new JPanel();
                caps.add(panelCap);
                panelCap.setBorder(border[0]);
                if (colorFonLine.length > 0) {
                    panelCap.setOpaque(true);
                    panelCap.setBackground(colorFonLine[0]);
                } else {
                    panelCap.setOpaque(false);
                }

                panelCap.setLayout(new GridLayout(1, extColPosition > 0 ? 3 : 2, 0, 0));
                panelCap.setBounds(0, 0, 100, 100);
                JLabel labCapLeft = new JLabel();
                try {
                    labCapLeft.setIcon(new javax.swing.ImageIcon(new URL(Uses.prepareAbsolutPathForImg(leftPic))));
                } catch (MalformedURLException ex) {
                    QLog.l().logger().error(ex);
                }
                final Font fontCap = new Font(getFontName(), 0,
                        Integer.parseInt(Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_FONT_SIZE_CAPTION).get(0).attributeValue(Uses.TAG_BOARD_VALUE)));

                JLabel labCapExt = new JLabel();
                try {
                    labCapExt.setIcon(new javax.swing.ImageIcon(new URL(Uses.prepareAbsolutPathForImg(extPic))));
                } catch (MalformedURLException ex) {
                    QLog.l().logger().error(ex);
                }
                if (extColPosition > 0) {
                    labCapExt = new JLabel();
                    labCapExt.setFont(fontCap);
                    labCapExt.setBackground(bgColor);
                    labCapExt.setForeground(fgColorCaprion);
                    labCapExt.setHorizontalAlignment(JLabel.CENTER);
                    labCapExt.setVerticalAlignment(JLabel.CENTER);
                    labCapExt.setBounds(0, 0, 100, 100);
                    labCapExt.setText(!"".equals(extColCaption) ? extColCaption : getLocaleMessage("board.point.ext"));
                }
                if (extColPosition == 1) {
                    panelCap.add(labCapExt);
                }

                labCapLeft.setFont(fontCap);
                labCapLeft.setBackground(bgColor);
                labCapLeft.setForeground(fgColorCaprion);
                labCapLeft.setHorizontalAlignment(JLabel.CENTER);
                labCapLeft.setVerticalAlignment(JLabel.CENTER);
                panelCap.add(labCapLeft);
                labCapLeft.setBounds(0, 0, 100, 100);
                labCapLeft.setText(!"".equals(leftColCaption) ? leftColCaption : getLocaleMessage("board.client"));

                if (extColPosition == 2) {
                    panelCap.add(labCapExt);
                }

                labCapLeft = new JLabel();
                try {
                    labCapLeft.setIcon(new javax.swing.ImageIcon(new URL(Uses.prepareAbsolutPathForImg(rightPic))));
                } catch (MalformedURLException ex) {
                    QLog.l().logger().error(ex);
                }
                labCapLeft.setFont(fontCap);
                labCapLeft.setBackground(bgColor);
                labCapLeft.setForeground(fgColorCaprion);
                labCapLeft.setHorizontalAlignment(JLabel.CENTER);
                labCapLeft.setVerticalAlignment(JLabel.CENTER);
                panelCap.add(labCapLeft);
                labCapLeft.setBounds(0, 0, 100, 100);
                labCapLeft.setText(!"".equals(rightColCaption) ? rightColCaption : getLocaleMessage("board.point"));

                if (extColPosition == 3) {
                    panelCap.add(labCapExt);
                }
            }
        }
        if (!caps.isEmpty()) {
            caps.stream().forEach(cap -> panelMain.add(cap));
        }
        final Line[][] cels = new Line[linesCount][colsCount];
        for (int j = 0; j < colsCount; j++) {
            for (int i = 0; i < linesCount; i++) {
                final Line panel = new Line(i);
                labels.add(panel);
                cels[i][j] = panel;
            }
        }
        for (Line[] lines : cels) {
            for (Line line : lines) {
                panelMain.add(line);
            }
        }
        repaint();
    }

    /**
     * Метод вывода инфы на табло.
     *
     * @param index      номер строки.
     * @param prefix     номер клиента - часть выводимого текста
     * @param num        номер клиента - часть выводимого текста
     * @param point      пункт куда позвали клиента - часть выводимого текста
     * @param extData    Третья колонка
     * @param blinkCount 0 - постоянное мигание, -1 не мигает. число - количество миганий
     */
    public void printRecord(int index, String prefix, Integer num, String point, String extData, int blinkCount) {
        if (index < getLinesCount()) {
            String nn = (num == null || num < 1 ? "" : num.toString());
            String number = prefix + (nn.isEmpty() ? "" : QConfig.cfg().getNumDivider(prefix)) + nn;
            labels.get(index).setLineData(number, point, extData);
            labels.get(index).setBlinkCount(blinkCount == -1 ? -1 : blinkCount * 2);
            if (blinkCount != -1) {
                labels.get(index).startBlink();
            }
        }
    }

    /**
     * Показать паннель вызванного.
     *
     * @param number номер вызванного.
     * @param point  куда вызвали.
     */
    public void showCallPanel(String number, String point) {
        showCallPanel(number, point, "", "", "");
    }

    /**
     * Показать паннель вызванного.
     *
     * @param number     номер вызванного.
     * @param point      куда вызвали.
     * @param userTxt    наименование юзера.
     * @param serviceTxt наименование услуги.
     * @param inputed    что ввел вызванный.
     */
    public void showCallPanel(String number, String point, String userTxt, String serviceTxt, String inputed) {
        if (isMain && "1".equals(Uses.elementsByAttr(mainElement, Uses.TAG_BOARD_NAME, Uses.TAG_BOARD_CALL_PANEL).get(0).attributeValue(Uses.TAG_BOARD_VALUE))) {
            log().info("Демонстрация номера вызванного \"" + number + " - " + point + "\" для показа в диалоге на главном табло.");
            if (callDialog != null) {
                callDialog.show(number, point, userTxt, serviceTxt, inputed);
            }
        }
    }

    /**
     * Включение/выключение звука видеородиков на табло.
     *
     * @param mute наличие звука в роликах
     */
    public void setMute(boolean mute) {
        panelUp.setMute(mute);
        panelLeft.setMute(mute);
        panelRight.setMute(mute);
        panelDown.setMute(mute);
    }

    /**
     * Выключение видеородиков на табло.
     */
    public void closeVideo() {
        panelUp.closeVideo();
        panelLeft.closeVideo();
        panelRight.closeVideo();
        panelDown.closeVideo();
    }

    protected void initComponentsOverride() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelCommon = new ru.apertum.qsystem.client.model.QPanel();
        spUp = new javax.swing.JSplitPane();
        panelUp = new ru.apertum.qsystem.client.model.QPanel();
        rlTop = new ru.apertum.qsystem.common.RunningLabel();
        spDown = new javax.swing.JSplitPane();
        spLeft = new javax.swing.JSplitPane();
        panelLeft = new ru.apertum.qsystem.client.model.QPanel();
        rlLeft = new ru.apertum.qsystem.common.RunningLabel();
        spRight = new javax.swing.JSplitPane();
        panelRight = new ru.apertum.qsystem.client.model.QPanel();
        rlRight = new ru.apertum.qsystem.common.RunningLabel();
        panelMain = new ru.apertum.qsystem.client.model.QPanel();
        spDown2 = new javax.swing.JSplitPane();
        panelDown = new ru.apertum.qsystem.client.model.QPanel();
        rlDown = new ru.apertum.qsystem.common.RunningLabel();
        panelDown2 = new ru.apertum.qsystem.client.model.QPanel();
        rlDown2 = new ru.apertum.qsystem.common.RunningLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance().getContext().getResourceMap(FIndicatorBoard.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setAlwaysOnTop(true);
        setName("Form"); // NOI18N
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        panelCommon.setBorder(new javax.swing.border.MatteBorder(null));
        panelCommon.setName("panelCommon"); // NOI18N
        panelCommon.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                panelCommonComponentResized(evt);
            }
        });

        spUp.setBorder(new javax.swing.border.MatteBorder(null));
        spUp.setDividerLocation(100);
        spUp.setDividerSize(0);
        spUp.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        spUp.setName("spUp"); // NOI18N
        spUp.setOpaque(false);

        panelUp.setBorder(new javax.swing.border.MatteBorder(null));
        panelUp.setName("panelUp"); // NOI18N
        panelUp.setNativePosition(java.lang.Boolean.FALSE);
        panelUp.setOpaque(false);
        panelUp.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                panelUpComponentResized(evt);
            }
        });

        rlTop.setBorder(new javax.swing.border.MatteBorder(null));
        rlTop.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        rlTop.setText(resourceMap.getString("rlTop.text")); // NOI18N
        rlTop.setFont(resourceMap.getFont("rlTop.font")); // NOI18N
        rlTop.setName("rlTop"); // NOI18N
        rlTop.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                FIndicatorBoard.this.mouseMoved(evt);
            }
        });
        rlTop.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                FIndicatorBoard.this.mouseEntered(evt);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                FIndicatorBoard.this.mouseExited(evt);
            }
        });

        javax.swing.GroupLayout panelUpLayout = new javax.swing.GroupLayout(panelUp);
        panelUp.setLayout(panelUpLayout);
        panelUpLayout.setHorizontalGroup(
                panelUpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(rlTop, javax.swing.GroupLayout.DEFAULT_SIZE, 911, Short.MAX_VALUE)
        );
        panelUpLayout.setVerticalGroup(
                panelUpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(rlTop, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE)
        );

        spUp.setTopComponent(panelUp);

        spDown.setBorder(new javax.swing.border.MatteBorder(null));
        spDown.setDividerLocation(250);
        spDown.setDividerSize(0);
        spDown.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        spDown.setName("spDown"); // NOI18N
        spDown.setOpaque(false);

        spLeft.setBorder(new javax.swing.border.MatteBorder(null));
        spLeft.setDividerLocation(150);
        spLeft.setDividerSize(0);
        spLeft.setName("spLeft"); // NOI18N
        spLeft.setOpaque(false);

        panelLeft.setBorder(new javax.swing.border.MatteBorder(null));
        panelLeft.setName("panelLeft"); // NOI18N
        panelLeft.setNativePosition(java.lang.Boolean.FALSE);
        panelLeft.setOpaque(false);
        panelLeft.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                FIndicatorBoard.this.mouseMoved(evt);
            }
        });
        panelLeft.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                FIndicatorBoard.this.mouseEntered(evt);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                FIndicatorBoard.this.mouseExited(evt);
            }
        });
        panelLeft.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                panelLeftComponentResized(evt);
            }
        });

        rlLeft.setBorder(new javax.swing.border.MatteBorder(null));
        rlLeft.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        rlLeft.setText(resourceMap.getString("rlLeft.text")); // NOI18N
        rlLeft.setFont(resourceMap.getFont("rlRight.font")); // NOI18N
        rlLeft.setName("rlLeft"); // NOI18N
        rlLeft.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                FIndicatorBoard.this.mouseMoved(evt);
            }
        });
        rlLeft.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                FIndicatorBoard.this.mouseEntered(evt);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                FIndicatorBoard.this.mouseExited(evt);
            }
        });

        javax.swing.GroupLayout panelLeftLayout = new javax.swing.GroupLayout(panelLeft);
        panelLeft.setLayout(panelLeftLayout);
        panelLeftLayout.setHorizontalGroup(
                panelLeftLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelLeftLayout.createSequentialGroup()
                                .addGap(0, 0, 0)
                                .addComponent(rlLeft, javax.swing.GroupLayout.DEFAULT_SIZE, 147, Short.MAX_VALUE)
                                .addGap(0, 0, 0))
        );
        panelLeftLayout.setVerticalGroup(
                panelLeftLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(rlLeft, javax.swing.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE)
        );

        spLeft.setLeftComponent(panelLeft);

        spRight.setBorder(new javax.swing.border.MatteBorder(null));
        spRight.setDividerLocation(250);
        spRight.setDividerSize(0);
        spRight.setName("spRight"); // NOI18N
        spRight.setOpaque(false);

        panelRight.setBorder(new javax.swing.border.MatteBorder(null));
        panelRight.setName("panelRight"); // NOI18N
        panelRight.setNativePosition(java.lang.Boolean.FALSE);
        panelRight.setOpaque(false);
        panelRight.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                panelRightComponentResized(evt);
            }
        });

        rlRight.setBorder(new javax.swing.border.MatteBorder(null));
        rlRight.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        rlRight.setText(resourceMap.getString("rlRight.text")); // NOI18N
        rlRight.setFont(resourceMap.getFont("rlRight.font")); // NOI18N
        rlRight.setName("rlRight"); // NOI18N
        rlRight.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                FIndicatorBoard.this.mouseMoved(evt);
            }
        });
        rlRight.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                FIndicatorBoard.this.mouseEntered(evt);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                FIndicatorBoard.this.mouseExited(evt);
            }
        });

        javax.swing.GroupLayout panelRightLayout = new javax.swing.GroupLayout(panelRight);
        panelRight.setLayout(panelRightLayout);
        panelRightLayout.setHorizontalGroup(
                panelRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(rlRight, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 507, Short.MAX_VALUE)
        );
        panelRightLayout.setVerticalGroup(
                panelRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(rlRight, javax.swing.GroupLayout.DEFAULT_SIZE, 243, Short.MAX_VALUE)
        );

        spRight.setRightComponent(panelRight);

        panelMain.setBorder(new javax.swing.border.MatteBorder(null));
        panelMain.setFont(resourceMap.getFont("rlRight.font")); // NOI18N
        panelMain.setName("panelMain"); // NOI18N
        panelMain.setOpaque(false);
        panelMain.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                FIndicatorBoard.this.mouseMoved(evt);
            }
        });
        panelMain.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                panelMainComponentResized(evt);
            }
        });

        javax.swing.GroupLayout panelMainLayout = new javax.swing.GroupLayout(panelMain);
        panelMain.setLayout(panelMainLayout);
        panelMainLayout.setHorizontalGroup(
                panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 247, Short.MAX_VALUE)
        );
        panelMainLayout.setVerticalGroup(
                panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 243, Short.MAX_VALUE)
        );

        spRight.setLeftComponent(panelMain);

        spLeft.setRightComponent(spRight);

        spDown.setLeftComponent(spLeft);

        spDown2.setBorder(new javax.swing.border.MatteBorder(null));
        spDown2.setDividerSize(0);
        spDown2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        spDown2.setOpaque(false);
        spDown2.setName("spDown2"); // NOI18N

        panelDown.setBorder(new javax.swing.border.MatteBorder(null));
        panelDown.setName("panelDown"); // NOI18N
        panelDown.setNativePosition(java.lang.Boolean.FALSE);
        panelDown.setOpaque(false);
        panelDown.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                panelDownComponentResized(evt);
            }
        });

        rlDown.setBorder(new javax.swing.border.MatteBorder(null));
        rlDown.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        rlDown.setText(resourceMap.getString("rlDown.text")); // NOI18N
        rlDown.setFont(resourceMap.getFont("rlRight.font")); // NOI18N
        rlDown.setName("rlDown"); // NOI18N
        rlDown.setRunningText(resourceMap.getString("rlDown.runningText")); // NOI18N
        rlDown.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                FIndicatorBoard.this.mouseMoved(evt);
            }
        });
        rlDown.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                FIndicatorBoard.this.mouseEntered(evt);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                FIndicatorBoard.this.mouseExited(evt);
            }
        });

        javax.swing.GroupLayout panelDownLayout = new javax.swing.GroupLayout(panelDown);
        panelDown.setLayout(panelDownLayout);
        panelDownLayout.setHorizontalGroup(
                panelDownLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(rlDown, javax.swing.GroupLayout.DEFAULT_SIZE, 907, Short.MAX_VALUE)
        );
        panelDownLayout.setVerticalGroup(
                panelDownLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(rlDown, javax.swing.GroupLayout.DEFAULT_SIZE, 118, Short.MAX_VALUE)
        );

        spDown2.setTopComponent(panelDown);

        panelDown2.setBorder(new javax.swing.border.MatteBorder(null));
        panelDown2.setName("panelDown2"); // NOI18N
        panelDown2.setOpaque(false);

        rlDown2.setBorder(new javax.swing.border.MatteBorder(null));
        rlDown2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        rlDown2.setText(resourceMap.getString("rlDown2.text")); // NOI18N
        rlDown2.setFont(resourceMap.getFont("rlRight.font")); // NOI18N
        rlDown2.setName("rlDown2"); // NOI18N
        rlDown2.setRunningText(resourceMap.getString("rlDown2.runningText")); // NOI18N

        javax.swing.GroupLayout panelDown2Layout = new javax.swing.GroupLayout(panelDown2);
        panelDown2.setLayout(panelDown2Layout);
        panelDown2Layout.setHorizontalGroup(
                panelDown2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(rlDown2, javax.swing.GroupLayout.DEFAULT_SIZE, 907, Short.MAX_VALUE)
        );
        panelDown2Layout.setVerticalGroup(
                panelDown2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(rlDown2, javax.swing.GroupLayout.DEFAULT_SIZE, 131, Short.MAX_VALUE)
        );

        spDown2.setRightComponent(panelDown2);

        spDown.setRightComponent(spDown2);

        spUp.setRightComponent(spDown);

        javax.swing.GroupLayout panelCommonLayout = new javax.swing.GroupLayout(panelCommon);
        panelCommon.setLayout(panelCommonLayout);
        panelCommonLayout.setHorizontalGroup(
                panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(spUp)
        );
        panelCommonLayout.setVerticalGroup(
                panelCommonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(spUp)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panelCommon, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panelCommon, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        loadDividerLocation();
    }//GEN-LAST:event_formComponentResized

    private void panelCommonComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_panelCommonComponentResized

        loadDividerLocation();
    }//GEN-LAST:event_panelCommonComponentResized

    private void panelMainComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_panelMainComponentResized

        loadDividerLocation();
    }//GEN-LAST:event_panelMainComponentResized

    private void panelDownComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_panelDownComponentResized

        loadDividerLocation();
    }//GEN-LAST:event_panelDownComponentResized

    private void panelUpComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_panelUpComponentResized

        loadDividerLocation();
    }//GEN-LAST:event_panelUpComponentResized

    private void panelLeftComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_panelLeftComponentResized

        loadDividerLocation();
    }//GEN-LAST:event_panelLeftComponentResized

    private void panelRightComponentResized(java.awt.event.ComponentEvent evt) {

        loadDividerLocation();
    }

    private Point point = null;

    private void mouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mouseMoved
        if (point != null && !QConfig.cfg().isDebug() && !zoneDebug) {
            try {
                Robot rob = new Robot();
                rob.mouseMove(point.x, point.y);
            } catch (AWTException ex) {
                QLog.l().logger().error("Can't move mouse to center, error in DrawingWindow.java:195");
            }
        }
    }//GEN-LAST:event_mouseMoved

    private void mouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mouseExited
        // not in use.
    }//GEN-LAST:event_mouseExited

    private void mouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mouseEntered
        point = evt.getLocationOnScreen();
        if (evt.getLocationOnScreen().x > Uses.firstMonitor.getDefaultConfiguration().getBounds().width - 5) {
            point = new Point(Uses.firstMonitor.getDefaultConfiguration().getBounds().width - 5, point.y);
        }
        if (evt.getLocationOnScreen().x < 0) {
            point = new Point(2, point.y);
        }
        if (evt.getLocationOnScreen().y > Uses.firstMonitor.getDefaultConfiguration().getBounds().height - 5) {
            point = new Point(point.x, Uses.firstMonitor.getDefaultConfiguration().getBounds().height - 15);
        }
        if (evt.getLocationOnScreen().y < 0) {
            point = new Point(point.x, 15);
        }
    }//GEN-LAST:event_mouseEntered

    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected ru.apertum.qsystem.client.model.QPanel panelCommon;
    protected ru.apertum.qsystem.client.model.QPanel panelDown;
    protected ru.apertum.qsystem.client.model.QPanel panelDown2;
    protected ru.apertum.qsystem.client.model.QPanel panelLeft;
    protected ru.apertum.qsystem.client.model.QPanel panelMain;
    protected ru.apertum.qsystem.client.model.QPanel panelRight;
    protected ru.apertum.qsystem.client.model.QPanel panelUp;
    protected ru.apertum.qsystem.common.RunningLabel rlDown;
    protected ru.apertum.qsystem.common.RunningLabel rlDown2;
    protected ru.apertum.qsystem.common.RunningLabel rlLeft;
    protected ru.apertum.qsystem.common.RunningLabel rlRight;
    protected ru.apertum.qsystem.common.RunningLabel rlTop;
    protected javax.swing.JSplitPane spDown;
    protected javax.swing.JSplitPane spDown2;
    protected javax.swing.JSplitPane spLeft;
    protected javax.swing.JSplitPane spRight;
    protected javax.swing.JSplitPane spUp;
    // End of variables declaration//GEN-END:variables

    public RunningLabel getTopRunningLabel() {
        return rlTop;
    }

    public RunningLabel getLeftRunningLabel() {
        return rlLeft;
    }

    public RunningLabel getRightRunningLabel() {
        return rlRight;
    }

    public RunningLabel getBottomRunningLabel() {
        return rlDown;
    }

    public RunningLabel getBottom2RunningLabel() {
        return rlDown2;
    }

    public QPanel getPanelDown() {
        return panelDown;
    }

    public QPanel getPanelDown2() {
        return panelDown2;
    }

    public QPanel getPanelLeft() {
        return panelLeft;
    }

    public QPanel getPanelRight() {
        return panelRight;
    }

    public QPanel getPanelUp() {
        return panelUp;
    }
}
