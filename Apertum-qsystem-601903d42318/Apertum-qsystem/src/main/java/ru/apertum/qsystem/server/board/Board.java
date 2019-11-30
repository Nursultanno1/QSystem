package ru.apertum.qsystem.server.board;

import org.dom4j.Element;
import org.dom4j.jaxb.JAXBWriter;
import org.reflections.ReflectionUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.exceptions.ServerException;
import ru.apertum.qsystem.server.controller.QIndicatorBoardMonitor;

import javax.annotation.PostConstruct;
import javax.swing.JLabel;
import javax.swing.border.MatteBorder;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.Color;
import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static ru.apertum.qsystem.common.Uses.TAG_BOARD_CALL_PANEL;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_CALL_PANEL_BACKGROUND;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_CALL_PANEL_DELAY;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_CALL_PANEL_HEIGHT;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_CALL_PANEL_TEMPLATE;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_CALL_PANEL_WIDTH;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_CALL_PANEL_X;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_CALL_PANEL_Y;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_COLS_COUNT;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_DELAY_VISIBLE;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_FONT_COLOR;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_FONT_COLOR_CAPTION;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_FONT_SIZE;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_FONT_SIZE_CAPTION;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_FONT_SIZE_LINE;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_FON_COLOR;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_FON_IMG;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_FRACTAL;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_HIDE_INVITE_BY_WORK;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_LINES_COUNT;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_MONITOR;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_NAME;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_PANEL_SIZE;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_RUNNING_TEXT;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_SIMPLE_DATE;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_SPEED_TEXT;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_VALUE;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_VIDEO_FILE;
import static ru.apertum.qsystem.common.Uses.TAG_BOARD_VISIBLE_PANEL;

@XmlRootElement(name = "board")
public class Board {

    /**
     * Отображать табло или не использовать вообще.
     */
    @XmlAttribute(name = "visible")
    public String visible = "true";

    public boolean isVisible() {
        return "1".equals(visible) || "true".equalsIgnoreCase(visible) || "yes".equalsIgnoreCase(visible) || visible.startsWith("$");
    }

    /**
     * Номер дополнительного монитора для табло.
     */
    @XmlAttribute(name = "monitor")
    public Integer monitor = 1;
    @XmlAttribute(name = "x")
    public Integer pointX = 1;
    @XmlAttribute(name = "y")
    public Integer pointY = 1;

    @XmlElement(name = "parameters")
    public Parameters parameters = new Parameters();
    @XmlElement(name = "top")
    public Segment top = new Segment();
    @XmlElement(name = "left")
    public Segment left = new Segment();
    @XmlElement(name = "right")
    public Segment right = new Segment();
    @XmlElement(name = "bottom")
    public Segment bottom = new Segment();
    @XmlElement(name = "bottom2")
    public Segment bottom2 = new Segment();

    @XmlElement()
    public InvitePanel invitePanel = new InvitePanel(); //  Панель вызванного

    public static class Segment {
        private String location = "";

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        @XmlJavaTypeAdapter(BooleanAdapter.class)
        @XmlAttribute(name = "visible")
        public Boolean visible = true;
        @XmlAttribute(name = "size")
        public Double size = 0.20d;
        @XmlElement(name = "content")
        public String content = "";

        @XmlElement(name = "runningText")
        public String runningText = "";
        @XmlElement(name = "speedRunningText")
        public Integer speedRunningText = 4;
        @XmlElement(name = "font")
        @XmlJavaTypeAdapter(FontAdapter.class)
        public Font font;
        @XmlElement()
        @XmlJavaTypeAdapter(ColorAdapter.class)
        public Color fontColor;   //  Цвет шрифта
        @XmlJavaTypeAdapter(BooleanAdapter.class)
        @XmlElement(name = "simpleDate")
        public Boolean simpleDate = false;
        @XmlElement(name = "backgroundImg")
        public String backgroundImg = "";
        @XmlElement(name = "videoFile")
        public String videoFile = "";
        @XmlElement(name = "fractal")
        public String fractal = "";
        @XmlElement(name = "nextTable")
        public NextTable nextTable = new NextTable();
    }

    public static class Parameters {
        @XmlElement()
        public Column leftColumn = new Column();
        @XmlElement()
        public Column rightColumn = new Column();
        @XmlElement()
        public ExtColumn extColumn = new ExtColumn();
        @XmlElement()
        public Row row = new Row();
        @XmlElement()
        public Header header = new Header();
        @XmlElement()
        public Integer rowCount = 5; // Количество строк на табло
        @XmlElement()
        public Integer columnCount = 1;  //  Количество столбцов на табло

        /**
         * Фоновое изображение. Подложка подо все табло.
         */
        @XmlElement()
        public String backgroundImg = "";
        @XmlElement()
        @XmlJavaTypeAdapter(ColorAdapter.class)
        public Color backgroundColor = Color.WHITE;   //  Цвет фона
        @XmlElement()
        @XmlJavaTypeAdapter(FontAdapter.class)
        public Font font = new JLabel().getFont();  //  Font name
        @XmlElement()
        public String columnSeparator = "";   //  Разделитель столбцов
        @XmlElement()
        public Integer invitationMinTime = 4; //  Минимальное время индикации на табло
        @XmlElement()
        public Boolean hideInvitationByStart = false; //  Hide invitation by start working

        public int cols() {
            return (leftColumn.visible ? 1 : 0) + (rightColumn.visible ? 1 : 0) + (extColumn.visible ? 1 : 0);
        }
    }

    public static class NextTable {
        @XmlJavaTypeAdapter(BooleanAdapter.class)
        @XmlAttribute()
        public Boolean visible = false;
        @XmlElement()
        public Integer columnCount;
        @XmlElement()
        public Integer rowCount;
        @XmlElement()
        public String caption = ""; //  Заголовок таблицы следующих
        @XmlElement()
        public Integer border; //  Рамка таблицы следующих

        @XmlElement(name = "font")
        @XmlJavaTypeAdapter(FontAdapter.class)
        public Font font;
        @XmlElement()
        @XmlJavaTypeAdapter(ColorAdapter.class)
        public Color fontColor;   //  Цвет шрифта
    }

    public static class Header {
        @XmlElement()
        @XmlJavaTypeAdapter(ColorAdapter.class)
        public Color fontColor = Color.BLUE;   //  Цвет шрифта
        @XmlElement()
        @XmlJavaTypeAdapter(FontAdapter.class)
        public Font font = new JLabel().getFont();  //  Цвет шрифта заголовка
        @XmlElement()
        @XmlJavaTypeAdapter(ColorAdapter.class)
        public Color backgroundColor = Color.WHITE;   //  Цвет фона

        @XmlJavaTypeAdapter(MatteBorderAdapter.class)
        @XmlElement()
        public MatteBorder border = new MatteBorder(1, 1, 1, 1, Color.BLACK);
    }

    public static class Row {
        @XmlElement()
        public Borders lineBorders = new Borders();

        @XmlElement()
        public Colors lineColors = new Colors(); //  Цвет строк, фоновый бакгроунд.

        @XmlElement()
        public String hint = ""; //  Надпись строки табло
        @XmlElement()
        @XmlJavaTypeAdapter(ColorAdapter.class)
        public Color hintColor = Color.BLACK; //  Цвет надписи строки табло
    }

    public static class ExtColumn extends Column {
        @XmlAttribute()
        public Integer order = 3; //  Порядок дополнительного столбца
    }

    public static class Column {
        @XmlAttribute(name = "visible")
        public Boolean visible = true;
        @XmlElement()
        public String caption = ""; //  Заголовок правого столбца
        @XmlElement()
        @XmlJavaTypeAdapter(FontAdapter.class)
        public Font font = new JLabel().getFont();  //  Font name
        @XmlElement()
        public String icon = ""; //  Left column pic

        @XmlElement()
        public Colors fontColors = new Colors(); //  Цвет шрифта строк.
    }

    public static class Colors {
        @XmlJavaTypeAdapter(ColorAdapter.class)
        @XmlElement()
        public List<Color> color = new ArrayList<>();  //  Цвет строк, фоновый бакгроунд.

        /**
         * Получить цвет строки. Строк много, по этому выдаем циклически.
         *
         * @param i номер строки для поиска ей окантовки.
         * @return окантовка.
         */
        public Color get(int i) {
            return color.get(i % color.size());
        }
    }

    public static class Borders {
        @XmlJavaTypeAdapter(MatteBorderAdapter.class)
        @XmlElement()
        public List<MatteBorder> border = new ArrayList<>();  //  Окантовка строк, Цвет рамки строки табло.

        /**
         * Получить окантовку. Строк много, по этому выдаем циклически.
         *
         * @param i номер строки для поиска ей окантовки.
         * @return окантовка.
         */
        public MatteBorder get(int i) {
            return border.isEmpty() ? null : border.get(i % border.size());
        }
    }


    public static class InvitePanel {
        @XmlJavaTypeAdapter(BooleanAdapter.class)
        @XmlAttribute()
        public Boolean show = true; //  Панель вызванного
        @XmlElement()
        public String backgroundImg = ""; //  Картинка панели вызванного
        @XmlAttribute(name = "x")
        public Integer pointX = 1;
        @XmlAttribute(name = "y")
        public Integer pointY = 1;
        @XmlElement()
        public Integer width = 1024; //  Панель вызванного-ширина
        @XmlElement()
        public Integer height = 700; //  Панель вызванного-высота
        /**
         * Панель вызванного-время показа сек.
         */
        @XmlElement()
        public Integer showDelay = 5; //  Панель вызванного-время показа сек
        /**
         * Панель вызванного-текст html+###.
         */
        @XmlElement()
        public String htmlText = ""; //  Панель вызванного-текст html+###
    }

    public static class MatteBorderAdapter extends XmlAdapter<String, MatteBorder> {

        @Override
        public MatteBorder unmarshal(String borderLine) throws Exception {

            String[] strings = borderLine.split("(\\s*;\\s*|\\s*,\\s*|\\s+)");
            return strings.length != 5
                    ? new MatteBorder(0, 0, 0, 0, Color.WHITE)
                    : new MatteBorder(Integer.parseInt(strings[0]), Integer.parseInt(strings[3]), Integer.parseInt(strings[2]), Integer.parseInt(strings[1]),
                    new ColorAdapter().unmarshal(strings[4]));
        }

        @Override
        public String marshal(MatteBorder border) throws Exception {
            return border.getBorderInsets().top + "," + border.getBorderInsets().right + "," + border.getBorderInsets().bottom + "," + border.getBorderInsets().left + ","
                    + new ColorAdapter().marshal(border.getMatteColor());
        }
    }

    public static class BooleanAdapter extends XmlAdapter<String, Boolean> {

        @Override
        public Boolean unmarshal(String bool) {
            return Boolean.parseBoolean(bool) || "1".equals(bool) || "yes".equalsIgnoreCase(bool);
        }

        @Override
        public String marshal(Boolean bool) {
            return bool == null ? "false" : bool.toString();
        }
    }

    public static class ColorAdapter extends XmlAdapter<String, Color> {

        @Override
        public Color unmarshal(String color) {
            return color == null || color.length() < 2 ? Color.WHITE : Color.decode(color);
        }

        @Override
        public String marshal(Color color) {
            return "#" + Integer.toHexString(color.getRGB()).substring(2);
        }
    }

    public static class FontAdapter extends XmlAdapter<String, Font> {

        @Override
        public Font unmarshal(String font) {
            return Font.decode(font);
        }

        @Override
        public String marshal(Font font) {
            final String style;
            switch (font.getStyle()) {
                case Font.ITALIC:
                    style = "italic";
                    break;
                case Font.BOLD:
                    style = "bold";
                    break;
                case Font.PLAIN:
                    style = "plain";
                    break;
                default:
                    style = "plain";
            }
            return font.getName() + "-" + style + "-" + font.getSize();
        }
    }

    @PostConstruct
    private final void setUp() {
        for (Field field : getClass().getDeclaredFields()) {
            if (field.getGenericType().equals(Segment.class)) {
                try {
                    final Segment segment = (Segment) field.get(this);
                    segment.location = field.getName();
                } catch (IllegalAccessException e) {
                    QLog.log().error(e);
                }
            }
        }
    }

    private Document marshalToDocument() throws ParserConfigurationException, JAXBException {
        final JAXBContext context = JAXBContext.newInstance(Board.class);
        final Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
        @SuppressWarnings("squid:S2755") // можно и так и больше никак
        final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        final Document document = docBuilderFactory.newDocumentBuilder().newDocument();
        marshaller.marshal(this, document);
        return document;
    }

    private byte[] marshalWithCData(Document document) throws TransformerException {
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        final Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "8");
        transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "content htmlText html");
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(result));
        return result.toByteArray();
    }

    /**
     * В Байты.
     */
    public byte[] marshalWithCData() throws JAXBException, ParserConfigurationException, TransformerException {
        return marshalWithCData(marshalToDocument());
    }

    public static Board unmarshal(File file) throws JAXBException, FileNotFoundException {
        return unmarshal(new FileInputStream(file));
    }

    /**
     * Загрузим Табло.
     */
    public static Board unmarshal(byte[] xml) throws JAXBException {
        return unmarshal(new ByteArrayInputStream(xml));
    }

    /**
     * Загрузим Табло.
     */
    public static Board unmarshal(InputStream inputStream) throws JAXBException {
        final JAXBContext jaxbContext = JAXBContext.newInstance(Board.class);
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setListener(new Unmarshaller.Listener() {

            @Override
            public void afterUnmarshal(Object object, Object arg1) {
                Method postConstructMethod = null;

                for (Method m : ReflectionUtils.getAllMethods(object.getClass())) {
                    if (m.getAnnotation(PostConstruct.class) != null) {
                        if (postConstructMethod != null) {
                            throw new IllegalStateException("@PostConstruct used multiple times");
                        }
                        postConstructMethod = m;
                    }
                }

                if (postConstructMethod != null) {
                    QLog.log().trace("invoking post construct: " + postConstructMethod.getName() + "()");

                    if (!Modifier.isFinal(postConstructMethod.getModifiers())) {
                        throw new IllegalArgumentException("post construct method [" + postConstructMethod.getName() + "] must be final");
                    }

                    try {
                        postConstructMethod.setAccessible(true); // thanks to skaffman
                        postConstructMethod.invoke(object);
                    } catch (Exception ex) {
                        throw new ServerException(ex);
                    }
                }
            }

        });
        return (Board) unmarshaller.unmarshal(inputStream);
    }

    /**
     * Превратить корень дерева dom4j в объект.
     *
     * @param root корень дерева dom4j.
     * @return в объект.
     */
    public static Board unmarshal(Element root) throws IOException, SAXException, JAXBException {
        final JAXBWriter jaxbWriter = new JAXBWriter("ru");
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        jaxbWriter.setOutput(outputStream);
        jaxbWriter.startDocument();
        jaxbWriter.writeElement(root);
        jaxbWriter.endDocument();

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        return unmarshal(inputStream);
    }

    /**
     * Конвертилка.
     */
    @SuppressWarnings("squid:S3776")
    public static void main(String[] args) {
        QIndicatorBoardMonitor boardMonitor = new QIndicatorBoardMonitor();
        boardMonitor.setConfigFile(args.length > 0 ? args[0] : "config/mainboard.xml");
        boardMonitor.setConfigFile(args.length > 0 ? args[0] : "config/clientboard.xml");
        Uses.ln("From file: " + boardMonitor.getConfigFile());
        Element root = boardMonitor.getConfig();

        Board board = new Board();
        board.visible = root.attributeValue(TAG_BOARD_VISIBLE_PANEL);
        board.pointX = Integer.parseInt(root.attributeValue("x"));
        board.pointY = Integer.parseInt(root.attributeValue("y"));

        ArrayList<Element> elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_MONITOR);
        board.monitor = elements.isEmpty() ? 1 : Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));


        // parameters
        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_FON_IMG);
        board.parameters.backgroundImg = elements.get(0).attributeValue(TAG_BOARD_VALUE);

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_FON_COLOR);
        board.parameters.backgroundColor = new ColorAdapter().unmarshal(elements.get(0).attributeValue(TAG_BOARD_VALUE));
        board.parameters.header.backgroundColor = board.parameters.backgroundColor;

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, "Font name");
        String fontName = elements.isEmpty() ? null : elements.get(0).attributeValue(TAG_BOARD_VALUE);
        Font font = fontName == null || fontName.isEmpty() ? new JLabel().getFont() : Font.getFont(fontName);
        board.parameters.font = font;

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_COLS_COUNT);
        board.parameters.columnCount = Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_LINES_COUNT);
        board.parameters.rowCount = Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_DELAY_VISIBLE);
        board.parameters.invitationMinTime = Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_FONT_COLOR_CAPTION);
        board.parameters.header.fontColor = new ColorAdapter().unmarshal("#" + elements.get(0).attributeValue(TAG_BOARD_VALUE));

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_FONT_SIZE_CAPTION);
        int size = Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));
        board.parameters.header.font = new Font(board.parameters.font.getName(), board.parameters.font.getStyle(), size);

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, "Разделитель столбцов");
        board.parameters.columnSeparator = elements.get(0).attributeValue(TAG_BOARD_VALUE);

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_HIDE_INVITE_BY_WORK);
        board.parameters.hideInvitationByStart = !elements.isEmpty() && new BooleanAdapter().unmarshal(elements.get(0).attributeValue(TAG_BOARD_VALUE));


        //row
        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, "Цвет надписи строки табло");
        loadRowColors(board.parameters.row, elements.get(0).attributeValue(TAG_BOARD_VALUE));
        board.parameters.row.hintColor = board.parameters.row.lineColors.get(0);

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, "Надпись строки табло");
        board.parameters.row.hint = elements.get(0).attributeValue(TAG_BOARD_VALUE);

        loadRowBorders(board.parameters.header, board.parameters.row,
                Uses.elementsByAttr(root, TAG_BOARD_NAME, "Цвет рамки строки табло").get(0).attributeValue(TAG_BOARD_VALUE),
                Uses.elementsByAttr(root, TAG_BOARD_NAME, "Окантовка строк").get(0).attributeValue(TAG_BOARD_VALUE));


        //Column left
        board.parameters.leftColumn.visible = true;
        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, "Заголовок левого столбца");
        board.parameters.leftColumn.caption = elements.get(0).attributeValue(TAG_BOARD_VALUE);

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, "Left column pic");
        board.parameters.leftColumn.icon = elements.isEmpty() ? "" : elements.get(0).attributeValue(TAG_BOARD_VALUE);

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_FONT_SIZE_LINE);
        size = Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));
        board.parameters.leftColumn.font = new Font(board.parameters.font.getName(), board.parameters.font.getStyle(), size);

        loadColumnColors(board.parameters.leftColumn, Uses.elementsByAttr(root, TAG_BOARD_NAME, "Цвет шрифта левого столбца").get(0).attributeValue(TAG_BOARD_VALUE));


        //Column rightColumn
        board.parameters.rightColumn.visible = true;
        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, "Заголовок правого столбца");
        board.parameters.rightColumn.caption = elements.get(0).attributeValue(TAG_BOARD_VALUE);

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, "Right column pic");
        board.parameters.rightColumn.icon = elements.isEmpty() ? "" : elements.get(0).attributeValue(TAG_BOARD_VALUE);

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_FONT_SIZE_LINE);
        size = Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));
        board.parameters.rightColumn.font = new Font(board.parameters.font.getName(), board.parameters.font.getStyle(), size);

        loadColumnColors(board.parameters.rightColumn, Uses.elementsByAttr(root, TAG_BOARD_NAME, "Цвет шрифта правого столбца").get(0).attributeValue(TAG_BOARD_VALUE));


        //Column EXT
        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, "Порядок дополнительного столбца");
        board.parameters.extColumn.order = Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));
        board.parameters.extColumn.visible = board.parameters.extColumn.order != 0;
        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, "Заголовок дополнительного столбца");
        board.parameters.extColumn.caption = elements.get(0).attributeValue(TAG_BOARD_VALUE);

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, "Ext column pic");
        board.parameters.extColumn.icon = elements.isEmpty() ? "" : elements.get(0).attributeValue(TAG_BOARD_VALUE);

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_FONT_SIZE_LINE);
        size = Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));
        board.parameters.extColumn.font = new Font(board.parameters.font.getName(), board.parameters.font.getStyle(), size);

        loadColumnColors(board.parameters.extColumn, "000000");


        // invitePanel
        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_CALL_PANEL);
        board.invitePanel.show = !elements.isEmpty() && new BooleanAdapter().unmarshal(elements.get(0).attributeValue(TAG_BOARD_VALUE));

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_CALL_PANEL_TEMPLATE);
        board.invitePanel.htmlText = elements.isEmpty() ? "" : elements.get(0).attributeValue(TAG_BOARD_VALUE);

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_CALL_PANEL_BACKGROUND);
        board.invitePanel.backgroundImg = elements.isEmpty() ? "" : elements.get(0).attributeValue(TAG_BOARD_VALUE);

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_CALL_PANEL_WIDTH);
        board.invitePanel.width = elements.isEmpty() ? 0 : Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_CALL_PANEL_HEIGHT);
        board.invitePanel.height = elements.isEmpty() ? 0 : Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_CALL_PANEL_X);
        board.invitePanel.pointX = elements.isEmpty() ? 0 : Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_CALL_PANEL_Y);
        board.invitePanel.pointY = elements.isEmpty() ? 0 : Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));

        elements = Uses.elementsByAttr(root, TAG_BOARD_NAME, TAG_BOARD_CALL_PANEL_DELAY);
        board.invitePanel.showDelay = elements.isEmpty() ? 0 : Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));

        loadSegment(board, root.element("Top"), board.top);
        loadSegment(board, root.element("Left"), board.left);
        loadSegment(board, root.element("Right"), board.right);
        loadSegment(board, root.element("Bottom"), board.bottom);
        loadSegment(board, root.element("Bottom2"), board.bottom2);

        String destFile = args.length > 1 ? args[1] : "config/mainboard-new.xml";
        Uses.ln("To file: " + destFile);
        try {
            Files.write(new File(destFile).toPath(), board.marshalWithCData());
            Uses.ln("Success!");
        } catch (Exception ex) {
            Uses.ln("FAILED...");
            Uses.ln(ex.toString());
            ex.printStackTrace();//NOSONAR
        }
    }

    private static void loadSegment(Board board, Element element, Segment segment) {
        segment.visible = new BooleanAdapter().unmarshal(element.attributeValue(TAG_BOARD_VISIBLE_PANEL));
        segment.size = Double.valueOf(element.attributeValue(TAG_BOARD_PANEL_SIZE));

        segment.content = element.getText().trim();

        ArrayList<Element> elements = Uses.elementsByAttr(element, TAG_BOARD_NAME, TAG_BOARD_RUNNING_TEXT);
        segment.runningText = elements.get(0).attributeValue(TAG_BOARD_VALUE).trim();

        elements = Uses.elementsByAttr(element, TAG_BOARD_NAME, TAG_BOARD_SPEED_TEXT);
        segment.speedRunningText = Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));

        elements = Uses.elementsByAttr(element, TAG_BOARD_NAME, TAG_BOARD_FON_IMG);
        segment.backgroundImg = elements.get(0).attributeValue(TAG_BOARD_VALUE);

        elements = Uses.elementsByAttr(element, TAG_BOARD_NAME, TAG_BOARD_VIDEO_FILE);
        segment.videoFile = elements.get(0).attributeValue(TAG_BOARD_VALUE);

        elements = Uses.elementsByAttr(element, TAG_BOARD_NAME, TAG_BOARD_FRACTAL);
        segment.fractal = elements.isEmpty() ? "" : elements.get(0).attributeValue(TAG_BOARD_VALUE);

        elements = Uses.elementsByAttr(element, TAG_BOARD_NAME, TAG_BOARD_SIMPLE_DATE);
        segment.simpleDate = new BooleanAdapter().unmarshal(elements.get(0).attributeValue(TAG_BOARD_VALUE));

        elements = Uses.elementsByAttr(element, TAG_BOARD_NAME, TAG_BOARD_FONT_COLOR);
        segment.fontColor = new ColorAdapter().unmarshal("#" + elements.get(0).attributeValue(TAG_BOARD_VALUE));
        segment.nextTable.fontColor = segment.fontColor;

        elements = Uses.elementsByAttr(element, TAG_BOARD_NAME, TAG_BOARD_FONT_SIZE);
        int size = Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));
        segment.font = new Font(board.parameters.font.getName(), board.parameters.font.getStyle(), size);
        segment.nextTable.font = segment.font;


        elements = Uses.elementsByAttr(element, TAG_BOARD_NAME, "Таблица следующих");
        segment.nextTable.visible = new BooleanAdapter().unmarshal(elements.get(0).attributeValue(TAG_BOARD_VALUE));

        elements = Uses.elementsByAttr(element, TAG_BOARD_NAME, "Колонки табл след");
        segment.nextTable.columnCount = Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));

        elements = Uses.elementsByAttr(element, TAG_BOARD_NAME, "Строки табл след");
        segment.nextTable.rowCount = Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));

        elements = Uses.elementsByAttr(element.getParent(), TAG_BOARD_NAME, "Заголовок таблицы следующих");
        segment.nextTable.caption = elements.get(0).attributeValue(TAG_BOARD_VALUE);

        elements = Uses.elementsByAttr(element.getParent(), TAG_BOARD_NAME, "Рамка таблицы следующих");
        segment.nextTable.border = elements.isEmpty() ? 0 : Integer.parseInt(elements.get(0).attributeValue(TAG_BOARD_VALUE));
    }

    private static final String DELIM = "(\\s*,\\s*|\\s*;\\s*|\\s+)";

    /**
     * Convert.
     *
     * @param header new
     * @param row    new
     * @param colors "FFFF00;ffffaa;77aaFF"
     * @param conts  "0,0,0,0;5,0,0,0"
     */
    @SuppressWarnings("squid:S3776")
    private static void loadRowBorders(Header header, Row row, String colors, String conts) {


        String clrs = colors;
        String[] cls = clrs.split(DELIM);
        Color[] colorRow = new Color[cls.length];
        for (int i = 0; i < cls.length; i++) {
            colorRow[i] = Color.decode("#" + (cls[i].trim().isEmpty() ? "F0F0F0" : cls[i].trim()));
        }


        String[] borderLines = (conts.isEmpty() ? "0,0,0,0" : conts).split("\\s*;\\s*");

        String[] ss1 = borderLines[0].replaceAll("[\\D&&[^,;\\s]]", "").split(DELIM);
        header.border = ss1.length < 4
                ? new MatteBorder(0, 0, 0, 0, colorRow[0])
                : new MatteBorder(Integer.parseInt(ss1[0]), Integer.parseInt(ss1[1]), Integer.parseInt(ss1[2]), Integer.parseInt(ss1[3]), colorRow[0]);
        for (int i = 0; i < borderLines.length - 1; i++) {
            Color color = colorRow.length == 1 ? colorRow[0] : (colorRow.length == 2 ? colorRow[1] : colorRow[i % (colorRow.length - 1) + 1]); //NOSONAR
            String[] ss = borderLines[borderLines.length == 1 ? 0 : (borderLines.length == 2 ? 1 : (i % (borderLines.length - 1) + 1))] //NOSONAR
                    .replaceAll("[\\D&&[^,;\\s]]", "").split(DELIM);

            row.lineBorders.border.add(ss.length < 4
                    ? new MatteBorder(0, 0, 0, 0, color)
                    : new MatteBorder(Integer.parseInt(ss[0].trim()), Integer.parseInt(ss[1].trim()), Integer.parseInt(ss[2].trim()), Integer.parseInt(ss[3].trim()), color));
        }
    }

    /**
     * Convert.
     *
     * @param column new
     * @param colors "FFFF00;ffffaa;77aaFF"
     */
    private static void loadColumnColors(Column column, String colors) {
        String clrs = colors;
        String[] cls = clrs.split(DELIM);

        for (int i = 0; i < cls.length; i++) {
            column.fontColors.color.add(Color.decode("#" + (cls[i].trim().isEmpty() ? "F0F0F0" : cls[i].trim())));
        }
    }

    /**
     * Convert.
     *
     * @param row    new
     * @param colors "FFFF00;ffffaa;77aaFF"
     */
    private static void loadRowColors(Row row, String colors) {
        String clrs = colors;
        String[] cls = clrs.split(DELIM);

        for (int i = 0; i < cls.length; i++) {
            row.lineColors.color.add(Color.decode("#" + (cls[i].trim().isEmpty() ? "F1F1F1" : cls[i].trim())));
        }
        if (row.lineColors.color.isEmpty()) {
            row.lineColors.color.add(Color.LIGHT_GRAY);
        }
    }
}
