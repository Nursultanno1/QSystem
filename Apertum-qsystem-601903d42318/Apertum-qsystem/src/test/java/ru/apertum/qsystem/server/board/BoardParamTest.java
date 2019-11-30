package ru.apertum.qsystem.server.board;

import org.powermock.reflect.Whitebox;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import ru.apertum.qsystem.client.forms.FParamsEditor;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.border.MatteBorder;
import java.awt.Color;
import java.awt.Font;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

@Ignore
public class BoardParamTest {

    Board board;

    @BeforeMethod
    public void setUp() {
        board = new Board();
    }

    @Test
    public void common() throws Exception {
        FParamsEditor paramsEditor = FParamsEditor.getParamsEditor(null, false);
        paramsEditor.setParams(board);
        Whitebox.invokeMethod(paramsEditor, "refresh");
        JList<IParam> listProps = Whitebox.getInternalState(paramsEditor, "listProps");
        assertNotNull(listProps);
        ListModel<IParam> model = listProps.getModel();
        assertNotNull(model);
        assertEquals(model.getSize(), 44);
        for (int i = 0; i < model.getSize(); i++) {
            assertTrue(model.getElementAt(i).getName().contains("."));
            assertNotNull(model.getElementAt(i).getValue());
        }
    }


    @Test
    public void nullType() throws Exception {
        assertNull(IParam.ParamJaxbImpl.makeParameter(BoardParamTest.class.getDeclaredField("board"), "board", this));
    }

    @Test
    public void simpleType() throws Exception {
        board.monitor = 100500;
        board.parameters.columnSeparator = ">";

        FParamsEditor paramsEditor = FParamsEditor.getParamsEditor(null, false);
        paramsEditor.setParams(board);
        Whitebox.invokeMethod(paramsEditor, "refresh");
        JList<IParam> listProps = Whitebox.getInternalState(paramsEditor, "listProps");
        ListModel<IParam> model = listProps.getModel();
        boolean flag = false;
        boolean flag2 = false;
        for (int i = 0; i < model.getSize(); i++) {
            IParam param = model.getElementAt(i);
            if ("board.monitor".equals(param.getName())) {
                assertEquals(param.getValue(), "100500");
                param.setValue(50);
                flag = true;
            }
            if ("parameters.columnSeparator".equals(param.getName())) {
                assertEquals(param.getValue(), ">");
                param.setValue("<");
                flag2 = true;
            }
        }
        assertTrue(flag);
        assertTrue(flag2);
        assertEquals(board.monitor, Integer.valueOf(50));
        assertEquals(board.parameters.columnSeparator, "<");
    }

    @Test
    public void coloreType() throws Exception {
        board.parameters.header.backgroundColor = Color.RED;

        FParamsEditor paramsEditor = FParamsEditor.getParamsEditor(null, false);
        paramsEditor.setParams(board);
        Whitebox.invokeMethod(paramsEditor, "refresh");
        JList<IParam> listProps = Whitebox.getInternalState(paramsEditor, "listProps");
        ListModel<IParam> model = listProps.getModel();
        boolean flag = false;
        for (int i = 0; i < model.getSize(); i++) {
            IParam param = model.getElementAt(i);
            if ("header.backgroundColor".equals(param.getName())) {
                assertEquals(param.getValue(), new Board.ColorAdapter().marshal(Color.RED));
                param.setValue(new Board.ColorAdapter().marshal(Color.YELLOW));
                flag = true;
            }
        }
        assertTrue(flag);
        assertEquals(board.parameters.header.backgroundColor, Color.YELLOW);
    }

    @Test
    public void fontType() throws Exception {
        Font font = new Font("Arial", 1, 10);
        Font font2 = new Font("Tahoma", 2, 20);
        board.parameters.header.font = font;

        FParamsEditor paramsEditor = FParamsEditor.getParamsEditor(null, false);
        paramsEditor.setParams(board);
        Whitebox.invokeMethod(paramsEditor, "refresh");
        JList<IParam> listProps = Whitebox.getInternalState(paramsEditor, "listProps");
        ListModel<IParam> model = listProps.getModel();
        boolean flag = false;
        for (int i = 0; i < model.getSize(); i++) {
            IParam param = model.getElementAt(i);
            if ("header.font".equals(param.getName())) {
                assertEquals(param.getValue(), new Board.FontAdapter().marshal(font));
                param.setValue(new Board.FontAdapter().marshal(font2));
                flag = true;
            }
        }
        assertTrue(flag);
        assertEquals(board.parameters.header.font, font2);
    }

    @Test
    public void matteborderType() throws Exception {
        MatteBorder border = new MatteBorder(1, 1, 1, 1, Color.BLACK);
        MatteBorder border2 = new MatteBorder(2, 2, 2, 2, Color.WHITE);
        board.parameters.header.border = border;

        FParamsEditor paramsEditor = FParamsEditor.getParamsEditor(null, false);
        paramsEditor.setParams(board);
        Whitebox.invokeMethod(paramsEditor, "refresh");
        JList<IParam> listProps = Whitebox.getInternalState(paramsEditor, "listProps");
        ListModel<IParam> model = listProps.getModel();
        boolean flag = false;
        for (int i = 0; i < model.getSize(); i++) {
            IParam param = model.getElementAt(i);
            if ("header.border".equals(param.getName())) {
                assertEquals(param.getValue(), new Board.MatteBorderAdapter().marshal(border));
                param.setValue(new Board.MatteBorderAdapter().marshal(border2));
                flag = true;
            }
        }
        assertTrue(flag);
        assertEquals(board.parameters.header.border.getMatteColor(), border2.getMatteColor());
        assertEquals(board.parameters.header.border.getBorderInsets(), border2.getBorderInsets());
    }

    @Test
    public void colorsType() throws Exception {
        Board.Colors colors = new Board.Colors();
        colors.color.add(Color.BLACK);
        colors.color.add(Color.BLACK);
        Board.Colors colors2 = new Board.Colors();
        colors2.color.add(Color.WHITE);
        colors2.color.add(Color.WHITE);
        board.parameters.leftColumn.fontColors = colors;

        FParamsEditor paramsEditor = FParamsEditor.getParamsEditor(null, false);
        paramsEditor.setParams(board);
        Whitebox.invokeMethod(paramsEditor, "refresh");
        JList<IParam> listProps = Whitebox.getInternalState(paramsEditor, "listProps");
        ListModel<IParam> model = listProps.getModel();
        boolean flag = false;
        for (int i = 0; i < model.getSize(); i++) {
            IParam param = model.getElementAt(i);
            if ("leftColumn.fontColors".equals(param.getName())) {
                String clr = new Board.ColorAdapter().marshal(colors.color.get(0));
                assertEquals(param.getValue(), clr + ";" + clr);
                clr = new Board.ColorAdapter().marshal(colors2.color.get(0));
                param.setValue(clr + ";" + clr);
                flag = true;
            }
        }
        assertTrue(flag);
        assertEquals(board.parameters.leftColumn.fontColors.color.size(), 2);
        assertEquals(board.parameters.leftColumn.fontColors.color.get(0), Color.WHITE);
        assertEquals(board.parameters.leftColumn.fontColors.color.get(1), Color.WHITE);
    }

    @Test
    public void bordersType() throws Exception {

        Board.Borders borders = new Board.Borders();
        MatteBorder border = new MatteBorder(1, 1, 1, 1, Color.BLACK);
        MatteBorder border2 = new MatteBorder(1, 1, 1, 1, Color.BLACK);
        borders.border.add(border);
        borders.border.add(border2);
        Board.Borders borders2 = new Board.Borders();
        border = new MatteBorder(2, 2, 2, 2, Color.WHITE);
        border2 = new MatteBorder(2, 2, 2, 2, Color.WHITE);
        borders2.border.add(border);
        borders2.border.add(border2);
        board.parameters.row.lineBorders = borders;

        FParamsEditor paramsEditor = FParamsEditor.getParamsEditor(null, false);
        paramsEditor.setParams(board);
        Whitebox.invokeMethod(paramsEditor, "refresh");
        JList<IParam> listProps = Whitebox.getInternalState(paramsEditor, "listProps");
        ListModel<IParam> model = listProps.getModel();
        boolean flag = false;
        for (int i = 0; i < model.getSize(); i++) {
            IParam param = model.getElementAt(i);
            if ("row.lineBorders".equals(param.getName())) {
                String clr = new Board.MatteBorderAdapter().marshal(borders.border.get(0));
                assertEquals(param.getValue(), clr + ";" + clr);
                clr = new Board.MatteBorderAdapter().marshal(borders2.border.get(0));
                param.setValue(clr + ";" + clr);
                flag = true;
            }
        }
        assertTrue(flag);
        assertEquals(board.parameters.row.lineBorders.border.size(), 2);
        assertEquals(board.parameters.row.lineBorders.border.get(0).getMatteColor(), borders2.border.get(0).getMatteColor());
        assertEquals(board.parameters.row.lineBorders.border.get(0).getBorderInsets(), borders2.border.get(0).getBorderInsets());
        assertEquals(board.parameters.row.lineBorders.border.get(1).getMatteColor(), borders2.border.get(1).getMatteColor());
        assertEquals(board.parameters.row.lineBorders.border.get(1).getBorderInsets(), borders2.border.get(1).getBorderInsets());
    }
}