package ru.apertum.qsystem.client.forms;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import ru.apertum.qsystem.client.model.QPanel;
import ru.apertum.qsystem.common.BrowserFX;
import ru.apertum.qsystem.common.QConfig;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.RunningLabel;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.model.ATalkingClock;
import ru.apertum.qsystem.server.board.Board;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.apertum.qsystem.common.QLog.log;

public class FIndicatorBoargJaxb extends FIndicatorBoard {

    private static FIndicatorBoargJaxb indicatorBoard = null;
    private transient Board board;

    /**
     * Конструктор формы с указанием количества строк на табло.
     */
    private FIndicatorBoargJaxb(Element rootParams, boolean isDebug, boolean fractal) {
        super(rootParams, isDebug, fractal);
        board = null;
    }

    /**
     * Конструктор формы с указанием количества строк на табло.
     */
    private FIndicatorBoargJaxb(Board board, boolean isDebug, boolean fractal) {
        super(null, isDebug, fractal); // не выполнится ничего.
        log().info("Создаем окно для информации JAXB.");
        this.board = board;
        init(board, isDebug, fractal);
        initComponentsOverride();
        spDown2.setOpaque(false);
        panelDown2.setOpaque(false);
        panelCommon.setBackground(board.parameters.backgroundColor);
        if (fractal) {
            panelMain.setVisible(false);
        }
        if (board != null && !QConfig.cfg().getModule().isClient() && !QConfig.cfg().getModule().isDesktop()) {
            callDialog = new FCallDialog(this, false, board);
        } else {
            callDialog = null;
        }
        log().trace("Прочитали настройки для окна информации JAXB.");
    }

    private void init(Board board, boolean isDebug, boolean fractal) {
        this.fractal = fractal;
        this.monitor = board.monitor;

        if (!isDebug) {
            setUndecorated(true);
            setType(Type.UTILITY);
        }
    }

    @Override
    public int getLinesCount() {
        return board.parameters.rowCount * board.parameters.columnCount;
    }

    @Override
    public int getPause() {
        return board.parameters.invitationMinTime;
    }

    @Override
    public boolean hideByStartWork() {
        return board.parameters.hideInvitationByStart;
    }

    /**
     * Получить форму табло. Получаем главное табло. Если требуется получить с указанием типа то использовать public static FIndicatorBoard
     * getIndicatorBoard(Board board, boolean isMain)
     *
     * @param board параметры табло.
     */
    public static FIndicatorBoargJaxb getIndicatorBoard(Board board) {
        if (!board.isVisible() || board.monitor < 1) {
            return null;
        }
        if (indicatorBoard == null || board != indicatorBoard.board) {
            indicatorBoard = new FIndicatorBoargJaxb(board, QConfig.cfg().isDebug(), false);
            indicatorBoard.loadConfigJaxb();
        }
        return indicatorBoard;
    }

    /**
     * Получить форму табло.
     *
     * @param board  параметры табло.
     * @param isMain режим. Главное или клиентское
     * @return
     */
    public static FIndicatorBoard getIndicatorBoard(Board board, boolean isMain) {
        if (indicatorBoard != null && FIndicatorBoard.isMain != isMain) {
            FIndicatorBoard.isMain = isMain;
            indicatorBoard = null;
        }
        FIndicatorBoard.isMain = isMain;
        return getIndicatorBoard(board);
    }

    /**
     * Получить форму табло. Получаем главное табло. Вызов этого метода создает новый объект. не использовать при одиночном табло. сделано для зонального.
     *
     * @param board   параметры табло.
     * @param isDebug отладка.
     * @return форма.
     */
    public static FIndicatorBoard getIndicatorBoardForZone(Board board, boolean isDebug) {
        if (!board.isVisible() || board.monitor < 1) {
            return null;
        }
        if (indicatorBoard == null || board != indicatorBoard.board) {
            indicatorBoard = new FIndicatorBoargJaxb(board, QConfig.cfg().isDebug(), false);
            indicatorBoard.loadConfigJaxb();
            indicatorBoard.zoneDebug = isDebug;
        }
        return indicatorBoard;
    }

    /**
     * Загрузка содержимого областей табло.
     */
    private void loadConfigJaxb() {
        //загрузим фоновый рисунок
        String filePath = board.parameters.backgroundImg;
        File f = new File(filePath);
        if (f.exists()) {
            panelCommon.setBackgroundImgage(filePath);
        }

        nexts.clear();
        elNexts.clear();
        loadPanel(board.top, rlTop, panelUp);
        loadPanel(board.bottom, rlDown, panelDown);
        loadPanel(board.bottom2, rlDown2, panelDown2);
        loadPanel(board.left, rlLeft, panelLeft);
        loadPanel(board.right, rlRight, panelRight);
        if (!fractal) {
            showLines();
        }
    }

    private final HashMap<RunningLabel, Board.Segment> elNexts = new HashMap<>();

    private void loadPanel(Board.Segment segment, RunningLabel label, QPanel panel) {
        if (!segment.visible) {
            return;
        }
        // цвет панельки
        label.setBackground(board.parameters.backgroundColor);
        // Проставим скорость, может пригодится если экстренно выводят текст в сегмент из админки.
        label.setSpeedRunningText(segment.speedRunningText);
        //загрузим размер и цвет шрифта
        final Font font = segment.font;
        label.setForeground(segment.fontColor);
        label.setFont(font);

        //загрузим фоновый рисунок
        final String filePath = segment.backgroundImg;
        File fp = new File(filePath);
        if (fp.exists()) {
            label.setBackgroundImage(filePath);
        }

        //загрузим фрактал
        String fileFractalXml = segment.fractal;
        final File frX = new File(fileFractalXml);
        if (frX.exists()) {
            panel.removeAll();
            panel.setLayout(new GridLayout(1, 1));
            final FIndicatorBoargJaxb fi;
            try {
                fi = new FIndicatorBoargJaxb(new SAXReader(false).read(frX).getRootElement(), true, true);
                fi.loadConfigJaxb();
                panel.add(fi.panelCommon);
            } catch (DocumentException ex) {
                QLog.l().logger().error(ex);
            }
        } else {

            //загрузим видео
            final String filePathVid = segment.videoFile;
            File fv = new File(filePathVid);
            if (fv.exists()) {
                label.setVisible(false);
                panel.setVideoFileName(filePathVid);
                panel.startVideo();
            } else { // если не видео, то простая дата или таблица ближайших
                if (segment.simpleDate) {
                    label.setRunningText("");
                    label.setText("");
                    label.setShowTime(true);
                } else { // загрузим текст
                    if (segment.nextTable.visible) {
                        // таблица ближайших
                        label.setVerticalAlignment(1);
                        label.setRunningText("");
                        label.setText("<HTML>"
                                + "<table  cellpadding='5' align='center' border='"
                                + segment.nextTable.border
                                + "' bordercolor='0'>"
                                + "<tr><td>"
                                + "<p align=center>"
                                + "<span style='font-size:"
                                + board.parameters.header.font.getSize() + ".0pt;color:"
                                + Integer.toHexString(board.parameters.header.fontColor.getRGB()).substring(2) + ";'>"
                                + (segment.nextTable.caption.isEmpty() ? getLocaleMessage("board.next_caption") : segment.nextTable.caption)
                                + "</span></p>"
                                + "</td></tr>"
                                + "<tr>"
                                + "</table>");
                        nexts.add(label);
                        elNexts.put(label, segment);
                    } else {
                        final String rt = segment.runningText;
                        if (!"".equals(rt)) {
                            label.setRunningText(rt);
                            label.setText("");
                            label.setSpeedRunningText(segment.speedRunningText);
                            label.start();
                        } else {
                            // просто хтмл-текст или URL
                            final String txt = segment.content.trim();
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

    /**
     * Загрузка размеров областей табло.
     */
    @Override
    protected void loadDividerLocation() {
        double down = 1;
        if (board.top.visible) {
            double up = board.top.size;
            spUp.setDividerLocation(up);
            panelUp.refreshVideoSize();
        } else {
            panelUp.setVisible(false);
        }

        if (!board.bottom2.visible && !board.bottom.visible) {
            panelDown.setVisible(false);
            panelDown2.setVisible(false);
            spDown.setDividerLocation(down);
        } else {
            if (board.bottom2.visible && board.bottom.visible) {
                if (board.bottom.visible) {
                    down = board.bottom.size;
                    spDown.setDividerLocation(down);
                    panelDown.refreshVideoSize();
                } else {
                    panelDown.setVisible(false);
                }
                if (board.bottom2.visible) {
                    double down2 = board.bottom2.size;
                    spDown2.setDividerLocation(down2);
                    panelDown2.refreshVideoSize();
                } else {
                    panelDown2.setVisible(false);
                }
            } else {
                if (board.bottom.visible) {
                    down = board.bottom.size
                            + (1 - board.bottom.size)
                            - (1 - board.bottom.size)
                            * board.bottom2.size;
                    spDown.setDividerLocation(down);
                    spDown2.setDividerLocation(1);
                    panelDown2.setVisible(false);
                    panelDown.refreshVideoSize();
                } else {
                    down = board.bottom.size
                            + (1 - board.bottom.size)
                            - (1 - board.bottom.size)
                            * (1 - board.bottom2.size);
                    spDown.setDividerLocation(down);
                    spDown2.setDividerLocation(0);
                    panelDown.setVisible(false);
                    panelDown2.refreshVideoSize();
                }
            }
        }
        if (board.left.visible) {
            double left = board.left.size;
            spLeft.setDividerLocation(left);
            panelLeft.refreshVideoSize();
        } else {
            panelLeft.setVisible(false);
        }
        if (board.right.visible) {
            double right = board.right.size;
            spRight.setDividerLocation(right);
            panelRight.refreshVideoSize();
        } else {
            panelRight.setVisible(false);
        }
    }

    /**
     * Показать ближайших.
     *
     * @param list список ближайших.
     */
    @Override
    public void showNext(List<String> list) {
        nexts.forEach(rl -> {
            String grid = "<HTML>"
                    + "<table cellpadding='5' align='center' border='"
                    + elNexts.get(rl).nextTable.border
                    + "'>"
                    + "<tr><td colspan='" + elNexts.get(rl).nextTable.columnCount + "'>"
                    + "<p align=center>"
                    + "<span style='font-size:" + board.parameters.header.font.getSize() + ".0pt;color:"
                    + Integer.toHexString(board.parameters.header.fontColor.getRGB()).substring(2) + ";'>"
                    + (elNexts.get(rl).nextTable.caption.isEmpty() ? getLocaleMessage("board.next_caption") : elNexts.get(rl).nextTable.caption)
                    + "</span></p>"
                    + "</td></tr>";
            int pos = 0;
            for (int i = 0; i < elNexts.get(rl).nextTable.rowCount; i++) {
                grid = grid + "<tr>";
                for (int j = 0; j < elNexts.get(rl).nextTable.columnCount; j++) {
                    grid = grid + "<td>"
                            + "<p align=center><span style='font-size:" + elNexts.get(rl).nextTable.font.getSize() + ".0pt;color:"
                            + Integer.toHexString(elNexts.get(rl).nextTable.fontColor.getRGB()).substring(2) + ";'>"
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

    /**
     * Показать паннель вызванного.
     *
     * @param number     номер вызванного.
     * @param point      куда вызвали.
     * @param userTxt    наименование юзера.
     * @param serviceTxt наименование услуги.
     * @param inputed    что ввел вызванный.
     */
    @Override
    public void showCallPanel(String number, String point, String userTxt, String serviceTxt, String inputed) {
        if (isMain && board.invitePanel.show) {
            log().info("Демонстрация номера вызванного \"" + number + " - " + point + "\" для показа в диалоге на главном табло.");
            if (callDialog != null) {
                callDialog.show(number, point, userTxt, serviceTxt, inputed);
            }
        }
    }

    /**
     * Создаем и расставляем контролы для строк по форме.
     */
    @Override
    protected void showLines() {
        log().info("Показываем набор строк.");
        final GridLayout la = new GridLayout(board.parameters.rowCount + (isMain ? 1 : 0), (isMain ? board.parameters.columnCount : 1), 10, 0);
        la.setHgap(0);
        la.setVgap(0);
        panelMain.setLayout(la);
        final ArrayList<JPanel> caps = new ArrayList<>();
        for (int i = 0; i < board.parameters.columnCount; i++) {
            if (isMain) {
                final JPanel panelCap = new JPanel();
                caps.add(panelCap);
                panelCap.setBorder(board.parameters.header.border);
                if (board.parameters.header.backgroundColor == null) {
                    panelCap.setOpaque(true);
                } else {
                    panelCap.setOpaque(false);
                    panelCap.setBackground(board.parameters.header.backgroundColor);
                }

                panelCap.setLayout(new GridLayout(1, board.parameters.cols(), 0, 0));
                panelCap.setBounds(0, 0, 100, 100);

                final Font fontCap = board.parameters.header.font;

                JLabel labCapExt = null;
                if (board.parameters.extColumn.visible) {
                    labCapExt = new JLabel();
                    try {
                        labCapExt.setIcon(new javax.swing.ImageIcon(new URL(Uses.prepareAbsolutPathForImg(board.parameters.extColumn.icon))));
                    } catch (MalformedURLException ex) {
                        QLog.l().logger().error(ex);
                    }
                    if (board.parameters.extColumn.order > 0) {
                        labCapExt = new JLabel();
                        labCapExt.setFont(fontCap);
                        labCapExt.setBackground(board.parameters.backgroundColor);
                        labCapExt.setForeground(board.parameters.header.fontColor);
                        labCapExt.setHorizontalAlignment(JLabel.CENTER);
                        labCapExt.setVerticalAlignment(JLabel.CENTER);
                        labCapExt.setBounds(0, 0, 100, 100);
                        labCapExt.setText(board.parameters.extColumn.caption.isEmpty() ? getLocaleMessage("board.point.ext") : board.parameters.extColumn.caption);
                    }
                }
                if (labCapExt != null && board.parameters.extColumn.order == 1) {
                    panelCap.add(labCapExt);
                }

                if (board.parameters.leftColumn.visible) {
                    JLabel labCapLeft = new JLabel();
                    try {
                        labCapLeft.setIcon(new javax.swing.ImageIcon(new URL(Uses.prepareAbsolutPathForImg(board.parameters.leftColumn.icon))));
                    } catch (MalformedURLException ex) {
                        QLog.l().logger().error(ex);
                    }
                    labCapLeft.setFont(fontCap);
                    labCapLeft.setBackground(board.parameters.backgroundColor);
                    labCapLeft.setForeground(board.parameters.header.fontColor);
                    labCapLeft.setHorizontalAlignment(JLabel.CENTER);
                    labCapLeft.setVerticalAlignment(JLabel.CENTER);
                    panelCap.add(labCapLeft);
                    labCapLeft.setBounds(0, 0, 100, 100);
                    labCapLeft.setText(board.parameters.leftColumn.caption.isEmpty() ? getLocaleMessage("board.client") : board.parameters.leftColumn.caption);
                }

                if (labCapExt != null && board.parameters.extColumn.order == 2) {
                    panelCap.add(labCapExt);
                }

                if (board.parameters.rightColumn.visible) {
                    JLabel labCapRight = new JLabel();
                    try {
                        labCapRight.setIcon(new javax.swing.ImageIcon(new URL(Uses.prepareAbsolutPathForImg(board.parameters.rightColumn.icon))));
                    } catch (MalformedURLException ex) {
                        QLog.l().logger().error(ex);
                    }
                    labCapRight.setFont(fontCap);
                    labCapRight.setBackground(board.parameters.backgroundColor);
                    labCapRight.setForeground(board.parameters.header.fontColor);
                    labCapRight.setHorizontalAlignment(JLabel.CENTER);
                    labCapRight.setVerticalAlignment(JLabel.CENTER);
                    panelCap.add(labCapRight);
                    labCapRight.setBounds(0, 0, 100, 100);
                    labCapRight.setText(board.parameters.rightColumn.caption.isEmpty() ? getLocaleMessage("board.point") : board.parameters.rightColumn.caption);
                }

                if (labCapExt != null && board.parameters.extColumn.order == 3) {
                    panelCap.add(labCapExt);
                }
            }
        }
        if (!caps.isEmpty()) {
            caps.stream().forEach(cap -> panelMain.add(cap));
        }
        final LineJaxb[][] cels = new LineJaxb[board.parameters.rowCount][board.parameters.columnCount];
        for (int j = 0; j < board.parameters.columnCount; j++) {
            for (int i = 0; i < board.parameters.rowCount; i++) {
                final LineJaxb panel = new LineJaxb(i);
                labels.add(panel);
                cels[i][j] = panel;
            }
        }
        for (LineJaxb[] lines : cels) {
            for (LineJaxb line : lines) {
                panelMain.add(line);
            }
        }
        repaint();
    }


    /**
     * Эот строка на табло со всеми на ней элементами вызова.
     */
    private final class LineJaxb extends JPanel implements ILine {

        private final JLabel left;
        private final JLabel right;
        private JLabel ext;

        /**
         * Строка N-ная.
         *
         * @param n номер строки
         */
        public LineJaxb(int n) {
            super();
            if (board.parameters.row.lineColors.color.size() > 1) {
                setOpaque(true);
                setBackground(board.parameters.row.lineColors.get(n));
            } else {
                setOpaque(false);
            }
            final MatteBorder matteBorder = board.parameters.row.lineBorders.get(n);
            if (matteBorder != null) {
                setBorder(matteBorder);
            }

            if (isMain && board.parameters.extColumn.order > 0 && board.parameters.extColumn.visible) {
                ext = new JLabel();
                ext.setFont(board.parameters.extColumn.font);
                ext.setBackground(board.parameters.backgroundColor);
                ext.setForeground(board.parameters.extColumn.fontColors.get(n));
                ext.setHorizontalAlignment(JLabel.CENTER);
                ext.setVerticalAlignment(JLabel.CENTER);
                ext.setBounds(0, 0, 100, 100);
                ext.setText("");
                ext.setVisible(true);
            } else {
                ext = null;
            }

            setLayout(new GridLayout(1, isMain ? (board.parameters.cols()) : 1, 0, 0));
            setBounds(0, 0, 100, 100);

            if (isMain && ext != null && board.parameters.extColumn.order == 1) {
                add(ext);
            }

            if (board.parameters.leftColumn.visible) {
                left = new JLabel();
                left.setFont(board.parameters.leftColumn.font);
                left.setBackground(board.parameters.backgroundColor);
                left.setForeground(board.parameters.leftColumn.fontColors.get(n));
                left.setHorizontalAlignment(JLabel.CENTER);
                left.setVerticalAlignment(JLabel.CENTER);
                add(left);
                left.setBounds(0, 0, 100, 100);
                left.setText("");
                left.setVisible(true);
            } else {
                left = null;
            }

            if (isMain && board.parameters.extColumn.order == 2 && ext != null) {
                add(ext);
            }

            if (isMain && board.parameters.rightColumn.visible) {
                right = new JLabel();
                right.setFont(board.parameters.rightColumn.font);
                right.setBackground(board.parameters.backgroundColor);
                right.setForeground(board.parameters.rightColumn.fontColors.get(n));
                right.setHorizontalAlignment(JLabel.CENTER);
                right.setVerticalAlignment(JLabel.CENTER);
                add(right);
                right.setBounds(0, 0, 100, 100);
                right.setText("");
                right.setVisible(true);
            } else {
                right = null;
            }

            if (isMain && board.parameters.extColumn.order == 3 && ext != null) {
                add(ext);
            }

            final String delPic = Uses.prepareAbsolutPathForImg(board.parameters.columnSeparator);
            boolean delEx2;
            try {
                delEx2 = new File(new URI(delPic)).exists();
            } catch (URISyntaxException | IllegalArgumentException ex) {
                log().warn("Parameter of main tablo \"" + Uses.TAG_BOARD_LINE_DELIMITER
                        + "\" is uncorrect URI or file not exists: \"" + board.parameters.columnSeparator + "\"(" + delPic + ")");
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
                if (left != null) {
                    if (number == null || number.isEmpty()) {
                        left.setText("");
                    } else {
                        left.setText(number);
                    }
                }
                if (right != null) {
                    if (point == null || point.isEmpty()) {
                        right.setText("");
                        right.setIcon(null);
                    } else {
                        if (board.parameters.columnSeparator == null || board.parameters.columnSeparator.isEmpty()) {
                            right.setHorizontalAlignment(JLabel.CENTER);
                            right.setText(point);
                        } else {
                            right.setHorizontalAlignment(JLabel.LEFT);

                            if (delEx) {
                                right.setIcon(arrow);
                                right.setText(point);
                            } else {
                                right.setText(board.parameters.columnSeparator + " " + point);
                            }
                        }
                    }
                }
                if (ext != null) {
                    ext.setText(extData);
                }
            } else {
                if (left != null) {
                    left.setText(number);
                }
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
                if (left != null) {
                    left.setVisible(true);
                }
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
                if (left != null) {
                    left.setVisible(true);
                }
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
                        if (left != null) {
                            left.setVisible(vis);
                        }
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
}
