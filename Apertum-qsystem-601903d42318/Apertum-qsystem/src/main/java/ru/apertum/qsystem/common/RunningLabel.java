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

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import ru.apertum.qsystem.client.Locales;
import ru.apertum.qsystem.common.model.ATalkingClock;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.apertum.qsystem.common.QLog.log;

/**
 * Компонент расширенной метки JLabel. Класс, расширяющий JLabel. Добавлены свойства создания бегущего текста, статического текста без привязки к лайаутам и
 * т.д. Умеет мигать.
 *
 * @author Evgeniy Egorov
 */
public class RunningLabel extends JLabel {

    public RunningLabel() {
        init();
    }

    protected final void init() {
        addComponentListener(new java.awt.event.ComponentAdapter() {

            @Override
            public void componentResized(java.awt.event.ComponentEvent evt) {
                onResize();
            }
        });

        phasesLight = speedBlink / getSpeedBlinkTimer();
        phasesDark = speedBlinkDark / getSpeedBlinkTimer();
    }

    /**
     * Событие ресайза метки. Если что-то нужно повесиь на ресайз, то перекрыть этод метод, незабыв вызвать предка.
     */
    protected void onResize() {
        needRepaint();
    }

    /**
     * Событие перерисовки 25 кадров в секунду.
     */
    private final transient ActionListener actionListener = (ActionEvent e) -> {
        need = true;
        repaint();
    };

    private final PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);

    @Override
    @SuppressWarnings("squid:S2589")
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
        if (listener == null || propertySupport == null) {
            return;
        }
        propertySupport.addPropertyChangeListener(listener);
    }

    @Override
    @SuppressWarnings("squid:S2589")
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        super.removePropertyChangeListener(listener);
        if (listener == null || propertySupport == null) {
            return;
        }
        propertySupport.removePropertyChangeListener(listener);
    }

    @Override
    public void setVerticalAlignment(int alignment) {
        super.setVerticalAlignment(alignment);
        needRepaint();
    }

    @Override
    public void setHorizontalAlignment(int alignment) {
        super.setHorizontalAlignment(alignment);
        needRepaint();
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        delta = 100 + 1 * font.getSize();
        needRepaint();
    }

    private int delta = 100;
    private int position = 0;
    private transient Image foregImg;
    private transient Graphics graphImg = null;
    private Dimension dmImg = null;
    public static final String PROP_SPEED = "speedRunningText";
    /**
     * Скорость движения текста. На сколько пикселей сместится кадр относительно предыдущего при 24 кадра в секкунду. По умолчанию 10 пикселей, т.е. 240
     * пикселей с секунку будет скорость движения текста.
     */
    private int speedRunningText = 10;

    public int getSpeedRunningText() {
        return speedRunningText;
    }

    /**
     * Установить скорость бегущего текста.
     *
     * @param speedRunningText в пикселах.
     */
    public void setSpeedRunningText(int speedRunningText) {
        final int oldValue = speedRunningText;
        this.speedRunningText = speedRunningText;
        propertySupport.firePropertyChange(PROP_SPEED, oldValue, speedRunningText);
    }

    public static final String PROP_RUNNING_TEXT = "running_text";

    private String dateFormat = null;

    public String getDateFormat() {
        return dateFormat;
    }

    /**
     * Формат выводимой даты.
     */
    public void setDateFormat(String dateFormat) {
        final String oldValue = dateFormat;
        this.dateFormat = dateFormat;

        final DateFormatSymbols symbols = new DateFormatSymbols(Locales.getInstance().getLangCurrent());
        switch (Locales.getInstance().getLangCurrent().toString().toLowerCase(Locale.US)) {
            case "ru_ru":
                symbols.setMonths(Locales.getRussianMonat());
                break;
            case "uk_ua":
                symbols.setMonths(Locales.getUkrainianMonat());
                break;
            case "az_az":
                symbols.setMonths(Locales.getAzerbaijanMonat());
                break;
            case "hy_am":
                symbols.setMonths(Locales.getArmenianMonat());
                break;
            default:
                log().info(Locales.getInstance().getLangCurrent().toString().toLowerCase(Locale.US));
        }
        theDateFormat = new SimpleDateFormat(dateFormat, symbols);

        propertySupport.firePropertyChange("dateFormat", oldValue, dateFormat);
    }

    private SimpleDateFormat theDateFormat = null;

    /**
     * Бегущий текст, это не тот же что статический.
     */
    private String runningText = "runningText";
    private int[] sizes = new int[0];
    private int totalSize = 0;

    private void setSizes(String text) {
        // подсчитаем длины
        sizes = new int[text.length()];
        totalSize = getFontMetrics(getFont()).stringWidth(text);
        for (int i = 0; i < text.length(); i++) {
            sizes[i] = getFontMetrics(getFont()).stringWidth(text.substring(i, i + 1));
            //System.out.print(" " + sizes[i])
        }
        //System.out.println()
        //System.out.println(getFont().getTitle() + " " + totalSize + " " + text)
    }

    /**
     * Этот метод установит новое значение бегущего текста, выведет его на конву и отцентрирует его в зависимости от установленных выравниваний.
     *
     * @param text устанавливаемый бегущий текст
     */
    public void setRunningText(String text) {
        if (new File(text).exists()) {
            prepareStrings(text);
            currentLine = 0;
        } else {
            currentLine = -1;
            lines.clear();
        }
        final String oldValue = runningText;
        this.runningText = text;
        propertySupport.firePropertyChange(PROP_RUNNING_TEXT, oldValue, runningText);
        needRepaint();
    }

    private String oldTxt = "";

    /**
     * Текст который бежит.
     */
    public String getRunningText() {
        final String txt;
        if (isShowTime()) {
            txt = getDate();
        } else {
            txt = currentLine >= 0 && !lines.isEmpty() ? getNextStringFromLines() : runningText;
        }
        if (sizes == null || oldTxt.length() != txt.length()) {
            setSizes(txt);
        }
        oldTxt = txt;
        return txt;
    }

    private final LinkedList<String> lines = new LinkedList<>();
    private String fileWithStrings;
    private boolean needNextLine = false;
    private int currentLine = -1;//то что надо выводить

    @SuppressWarnings("squid:S3776")
    private void prepareStrings(String filePath) {
        fileWithStrings = filePath;
        try {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), Charset.forName("utf8")))) {
                String line;
                lines.clear();
                while ((line = br.readLine()) != null) {
                    if (!line.startsWith("#")) {
                        lines.add(line.trim());
                    }
                }
            }
        } catch (IOException ex) {
            log().error("Cant read running strings from file " + filePath, ex);
        }
        // XSLT
        if (lines.size() == 3 && lines.getFirst().toLowerCase().startsWith("xml")) {
            String url = lines.get(1);

            final Pattern pattern = Pattern.compile("##.+?##");
            final Matcher matcher = pattern.matcher(url);
            // check all occurance
            while (matcher.find()) {
                final SimpleDateFormat sdf2 = new SimpleDateFormat(matcher.group().substring(2, (matcher.group().length() - 2)));
                url = url.replace(matcher.group(), sdf2.format(new Date()));
            }
            try {
                final URL xmlIn = new URL(url);
                @SuppressWarnings("squid:S2755") // можно и так и больше никак
                final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                final DocumentBuilder builder = factory.newDocumentBuilder();
                final Document document = builder.parse(xmlIn.openStream());

                // Use a Transformer for output
                final URL xslIn = new URL(lines.get(2));
                final StreamSource stylesource = new StreamSource(xslIn.openStream());

                final TransformerFactory tFactory = TransformerFactory.newInstance();
                tFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                final Transformer transformer = tFactory.newTransformer(stylesource);
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");

                final DOMSource source = new DOMSource(document);
                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                final StreamResult result = new StreamResult(os);
                transformer.transform(source, result);

                try (BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(os.toByteArray()), Charset.forName("utf8")))) {
                    String line;
                    lines.clear();
                    while ((line = br.readLine()) != null) {
                        if (!line.startsWith("#")) {
                            lines.add(line.trim());
                        }
                    }
                }
            } catch (IOException ex) {
                log().error("Cant read running strings from XSLT 1 " + filePath, ex);
            } catch (ParserConfigurationException | SAXException ex) {
                log().error("Cant read running strings from XSLT 2 " + filePath, ex);
            } catch (TransformerConfigurationException ex) {
                log().error("Cant read running strings from XSLT 3 " + filePath, ex);
            } catch (TransformerException ex) {
                log().error("Cant read running strings from XSLT 4 " + filePath, ex);
            }
        }
    }

    private String getNextStringFromLines() {
        final String res;
        if (needNextLine) {
            prepareStrings(fileWithStrings);
            if (lines.isEmpty()) {
                res = runningText;// файл пустой подсунули
            } else {
                if (currentLine < lines.size()) {
                    res = lines.get(currentLine);
                    currentLine = currentLine == lines.size() - 1 ? 0 : currentLine + 1;
                } else {
                    res = lines.get(0);
                    currentLine = 1;
                }
            }
            sizes = null;// переразбить длины
        } else {
            currentLine = currentLine > lines.size() - 1 ? 0 : currentLine;
            res = lines.get(currentLine);
        }
        needNextLine = false;
        return res;
    }

    private SimpleDateFormat sdf;

    private String getDate() {
        if (Locales.getInstance().isRuss()) {
            if (sdf == null) {
                DateFormatSymbols russSymbol = new DateFormatSymbols(Locales.getInstance().getLangCurrent());
                russSymbol.setMonths(Locales.getRussianMonat());
                sdf = new SimpleDateFormat("dd MMMM  HH.mm:ss", russSymbol);
            }
            return sdf.format(new Date());
        } else {
            if (Locales.getInstance().isEng()) {
                return Locales.getInstance().formatForLabel2.format(new Date());
            } else {
                return theDateFormat != null ? theDateFormat.format(new Date()) : Locales.getInstance().formatForLabel2Short.format(new Date());
            }
        }
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        needRepaint();
    }

    @Override
    public void setForeground(Color fg) {
        super.setForeground(fg);
        needRepaint();
    }

    private void needRepaint() {
        needRepaint = true;
    }

    @Override
    public void paint(Graphics g) {
        if (backgroundImg != null || !"".equals(getRunningText()) || isShowTime()) {
            // Тут условия на изменение картинки с текстом
            if (((run || isBlink()) && need) || ((!run && !isBlink()) && need) || needRepaint) {

                updateImg();
                need = false;
                needRepaint = false;
            }
            if (foregImg != null) {
                g.drawImage(foregImg, 0, 0, null);
            } else {
                if (backgroundImg != null) {
                    g.drawImage(backgroundImg, 0, 0, null);
                }
            }
        }
        super.paint(g);
    }

    @Override
    public void update(Graphics g) {
        // не знаю зачем, попытка улучшить прорисовку, но не проверено как повлияло  paint (getGraphics ())
    }

    /**
     * Формирование картинки сдвинутой для эффекта смещения.
     */
    @SuppressWarnings("squid:S3776")
    private void updateImg() {

        Dimension dm = getSize();
        int wndWidth = dm.width;
        int wndHeight = dm.height;

        if ((dmImg == null)
            || (dmImg.width != wndWidth)
            || (dmImg.height != wndHeight)) {
            dmImg = new Dimension(wndWidth, wndHeight);
            foregImg = createImage(wndWidth, wndHeight);
            graphImg = foregImg.getGraphics();
        }

        Color bg = getBackground();
        graphImg.setColor(bg);

        //Теперь мы закрашиваем изображение:
        graphImg.fillRect(0, 0, dmImg.width, dmImg.height);
        if (getBackgroundImage() != null) {
            graphImg.drawImage(backgroundImg, 0, 0, null);
        }
        Color fg = getForeground();
        graphImg.setColor(fg);

        // отцентрируем по вертикале
        final int y;
        switch (getVerticalAlignment()) {
            case SwingConstants.CENTER:
                y = (getHeight() - (int) (getFont().getSize() * 0.95)) / 2;
                break;
            case SwingConstants.BOTTOM:
                y = getHeight() - (int) (getFont().getSize() * 0.95);
                break;
            default:
                y = 0;
        }
        /*
         CENTER  = 0
         TOP     = 1
         LEFT    = 2
         BOTTOM  = 3
         RIGHT   = 4
         */
        // отцентрируем по горизонтале
        final int len = sizes.length == 0 ? 0 : totalSize;
        if (!run) {
            switch (getHorizontalAlignment()) {
                case SwingConstants.CENTER:
                    position = (getWidth() - len) / 2;
                    break;
                case SwingConstants.RIGHT:
                    position = getWidth() - len;
                    break;
                default:
                    position = 0;
            }
        }
        //Затем рисуем строку в контексте изображения:
        graphImg.setFont(getFont());

        String forDrow = null;
        position = position - (run ? getSpeedRunningText() : 0);
        if (position < -len) { // проверка, если уехало целиков за левый край, то перекинем все за правый край
            needNextLine = true; // повторный пробег
            forDrow = getRunningText(); // подсосалась и обработалась следующая строка
            position = getSize().width;
        }
        if (forDrow == null) {// уже подучили в предыдущем IF
            forDrow = getRunningText();
        }
        if (isVisibleRunningText && !forDrow.isEmpty()) {
            int lenHead = 0;
            int pos = -1;// позиция последней отрубаемой буквы
            while (-position - delta > lenHead) {
                lenHead = lenHead + sizes[++pos];
            }

            int posLast = pos == -1 ? 0 : pos;
            int lenTail = 0;
            while (++posLast < forDrow.length() && getWidth() + delta > lenTail) {
                lenTail = lenTail + sizes[posLast];
            }
            forDrow = forDrow.substring(pos == -1 ? 0 : pos + 1, posLast);
            graphImg.drawString(forDrow,
                position + lenHead,
                y + (int) (getFont().getSize() * 0.75));
        }
    }

    public static final String PROP_BACKGROUND_IMG = "backgroundImgage";
    /**
     * Фоновая картинка.
     */
    private String backgroundImage = "";
    private transient Image backgroundImg = null;

    public String getBackgroundImage() {
        return backgroundImage;
    }

    /**
     * Установить фоновую картинку.
     *
     * @param resourceName картинка.
     */
    public void setBackgroundImage(String resourceName) {
        backgroundImage = resourceName;
        final String oldValue = resourceName;
        this.backgroundImg = Uses.loadImage(this, resourceName, "");
        propertySupport.firePropertyChange(PROP_BACKGROUND_IMG, oldValue, resourceName);
        needRepaint();
    }

    public static final String PROP_IS_RUN = "isRunText";
    /**
     * бежит ли строка.
     */
    private Boolean run = false;

    public Boolean isRun() {
        return run;
    }

    /**
     * Бежать тексту.
     *
     * @param run да или нет.
     */
    public void setRun(Boolean run) {
        final Boolean oldValue = run;
        this.run = run;
        propertySupport.firePropertyChange(PROP_IS_RUN, oldValue, run);
        if (run) {
            start();
        } else {
            stop();
        }
    }

    /**
     * Возникло ли событие смещение надписи.
     */
    private boolean need = false;
    private boolean needRepaint = false;
    /**
     * Поток генерации событий смещения текста.
     */
    //private Thread timerThread = null
    private final Timer timerThread = new Timer(40, actionListener);

    /**
     * Запустить бегущий текст.
     */
    public void start() {
        run = true;
        setRateMainTimer();
        /**
         * Создать поток для инициализации перересовки. Он выступает так же и в качестве таймера для перересовки.
         */
        position = getWidth();
        if (!timerThread.isRunning()) {
            timerThread.start();
        }
    }

    /**
     * Остановить бегущий текст.
     */
    public void stop() {
        run = false;
        setRateMainTimer();
        if (!isBlink() && !showTime) {
            timerThread.stop();
        }
    }

    public static final String PROP_BLINK_COUNT = "blinkCount";
    /**
     * Количество миганий текста.
     */
    private int blinkCount = 0;

    public int getBlinkCount() {
        return blinkCount;
    }

    /**
     * Установить количество мигания.
     *
     * @param blinkCount столько раз мигнет.
     */
    public void setBlinkCount(int blinkCount) {
        final int oldValue = blinkCount;
        this.blinkCount = blinkCount;
        propertySupport.firePropertyChange(PROP_BLINK_COUNT, oldValue, blinkCount);
        blinkTimer.setCount(blinkCount * 2);
    }

    public static final String PROP_SPEED_BLINK = "speed_blink";
    public static final String PROP_SPEED_BLINK_DARK = "speed_blink_dark";

    private int phasesLight = 1;
    private int phasesDark = 1;

    private int getSpeedBlinkTimer() {
        return (speedBlink + speedBlinkDark) / 10;
    }

    /**
     * Время невидимой фазы текста.
     */
    private int speedBlinkDark = 500;

    public int getSpeedBlinkDark() {
        return speedBlinkDark;
    }

    /**
     * Установить фазу негорения.
     *
     * @param speedBlinkDark милисек.
     */
    public void setSpeedBlinkDark(int speedBlinkDark) {
        final int oldValue = speedBlinkDark;
        this.speedBlinkDark = speedBlinkDark;
        propertySupport.firePropertyChange(PROP_SPEED_BLINK_DARK, oldValue, speedBlinkDark);
        final int period = getSpeedBlinkTimer();
        blinkTimer.setInterval(period);
        phasesLight = speedBlink / period;
        phasesDark = speedBlinkDark / period;
    }

    /**
     * Время фазы горения текста.
     */
    private int speedBlink = 500;

    public int getSpeedBlink() {
        return speedBlink;
    }

    /**
     * Установить скорость мигания.
     *
     * @param speedBlink милиск.
     */
    public void setSpeedBlink(int speedBlink) {
        final int oldValue = blinkCount;
        this.speedBlink = speedBlink;
        propertySupport.firePropertyChange(PROP_SPEED_BLINK, oldValue, speedBlink);
        final int period = getSpeedBlinkTimer();
        blinkTimer.setInterval(period);
        phasesLight = speedBlink / period;
        phasesDark = speedBlinkDark / period;
    }

    /**
     * Таймер мигания надписи.
     */
    private final transient ATalkingClock blinkTimer = new ATalkingClock(getSpeedBlinkTimer(), getBlinkCount()) {

        private int phases = 1;

        @Override
        public void run() {
            setRateMainTimer();
            if ((isVisibleRunningText && phasesLight == phases) || (!isVisibleRunningText && phasesDark == phases)) {
                isVisibleRunningText = !isVisibleRunningText;
                phases = 1;
            } else {
                phases++;
            }
        }

        @Override
        public void stop() {
            super.stop();
            stopBlink();
        }
    };

    /**
     * Запустить мигание текста.
     */
    public void startBlink() {
        if (!blinkTimer.isActive()) {
            blinkTimer.start();
        }

        /**
         * Создать поток для инициализации перересовки. Он выступает так же и в качестве таймера для перересовки.
         */
        if (!timerThread.isRunning()) {
            timerThread.start();
        }
    }

    /**
     * Остановить мигание текста.
     */
    public void stopBlink() {
        if (blinkTimer.isActive()) {
            blinkTimer.stop();
        }
        isVisibleRunningText = true;
        if (!run && !showTime) {
            need = true;
            repaint();
        }
        setRateMainTimer();
    }

    /**
     * Нужно для мигания.
     */
    private boolean isVisibleRunningText = true;

    /**
     * мигает ли строка.
     */
    private boolean isBlink() {
        return blinkTimer.isActive();
    }

    public static final String PROP_SHOW_TIME = "showTime";
    /**
     * Показывать или нет время вместо текста на бегущей строке.
     */
    private Boolean showTime = false;

    public Boolean isShowTime() {
        return showTime;
    }

    /**
     * Метка показывает время.
     *
     * @param showTime да или нет.
     */
    public void setShowTime(Boolean showTime) {
        final Boolean oldValue = showTime;
        this.showTime = showTime;
        propertySupport.firePropertyChange(PROP_SHOW_TIME, oldValue, showTime);
        setRateMainTimer();
        /**
         * Создать поток для инициализации перересовки. Он выступает так же и в качестве таймера для перересовки.
         */
        if (showTime) {
            setSizes(getDate());
            timerThread.start();
        } else {
            setRunningText(runningText);
            needRepaint();
        }
    }

    private void setRateMainTimer() {
        if (run) {
            timerThread.setDelay(40);
        } else {
            timerThread.setDelay(isBlink() ? getSpeedBlinkTimer() : 1000);
        }
        if (timerThread.isRunning()) {
            timerThread.restart();
        }
    }
}
