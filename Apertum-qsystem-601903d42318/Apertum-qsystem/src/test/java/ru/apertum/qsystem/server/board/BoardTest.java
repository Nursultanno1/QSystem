package ru.apertum.qsystem.server.board;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.border.MatteBorder;
import java.awt.Color;
import java.awt.Font;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class BoardTest {

    @BeforeMethod
    public void setUp() {
    }

    @Test
    public void testFontAdapter() throws Exception {
        Board.FontAdapter fontAdapter = new Board.FontAdapter();
        Font font = new Font("Verdana", 2, 24);
        assertEquals(fontAdapter.unmarshal(fontAdapter.marshal(font)), font);
        font = new Font("Terminal", 1, 14);
        assertEquals(fontAdapter.unmarshal(fontAdapter.marshal(font)), font);
    }

    @Test
    public void testColorAdapter() throws Exception {
        Board.ColorAdapter colorAdapter = new Board.ColorAdapter();
        Color color = Color.cyan;
        assertEquals(colorAdapter.unmarshal(colorAdapter.marshal(color)), color);
        color = Color.red;
        assertEquals(colorAdapter.unmarshal(colorAdapter.marshal(color)), color);
        color = Color.lightGray;
        assertEquals(colorAdapter.unmarshal(colorAdapter.marshal(color)), color);
    }

    @Test
    public void testBooleanAdapter() throws Exception {
        Board.BooleanAdapter booleanAdapter = new Board.BooleanAdapter();
        Boolean bool = true;
        assertEquals(booleanAdapter.unmarshal(booleanAdapter.marshal(bool)), bool);
        bool = false;
        assertEquals(booleanAdapter.unmarshal(booleanAdapter.marshal(bool)), bool);

        assertTrue(booleanAdapter.unmarshal("1"));
        assertTrue(booleanAdapter.unmarshal("Yes"));
        assertTrue(booleanAdapter.unmarshal("TRUE"));
        assertFalse(booleanAdapter.unmarshal("0"));
        assertFalse(booleanAdapter.unmarshal("false"));
        assertFalse(booleanAdapter.unmarshal(null));
    }

    @Test
    public void testMatteBorderAdapter() throws Exception {
        Board.MatteBorderAdapter matteBorderAdapter = new Board.MatteBorderAdapter();
        MatteBorder border = new MatteBorder(1, 2, 3, 4, Color.DARK_GRAY);
        assertEquals(matteBorderAdapter.unmarshal(matteBorderAdapter.marshal(border)).getBorderInsets().top, border.getBorderInsets().top);
        assertEquals(matteBorderAdapter.unmarshal(matteBorderAdapter.marshal(border)).getBorderInsets().right, border.getBorderInsets().right);
        assertEquals(matteBorderAdapter.unmarshal(matteBorderAdapter.marshal(border)).getBorderInsets().bottom, border.getBorderInsets().bottom);
        assertEquals(matteBorderAdapter.unmarshal(matteBorderAdapter.marshal(border)).getBorderInsets().left, border.getBorderInsets().left);
        assertEquals(matteBorderAdapter.unmarshal(matteBorderAdapter.marshal(border)).getMatteColor().getRGB(), border.getMatteColor().getRGB());
    }

    @Test
    public void testMurshaller() throws Exception {
        Board board = new Board();
        board.pointX = 10;
        board.pointY = 20;
        board.monitor = 1;
        board.visible = "1";
        Board.Segment segment = new Board.Segment();
        segment.visible = true;
        segment.size = 12.34d;
        segment.backgroundImg = "backgroundImg";
        segment.content = "<><><><><>";
        board.top = segment;
        board.left = segment;
        board.right = segment;
        board.bottom = segment;
        board.bottom2 = segment;
        Board.Parameters parameters = new Board.Parameters();
        parameters.backgroundColor = Color.GRAY;
        parameters.header.font = new Font("Verdana", 2, 24);
        Board.InvitePanel invitePanel = new Board.InvitePanel();
        invitePanel.height = 150;
        invitePanel.show = true;
        invitePanel.htmlText = "<asd asd='asd'\\>";
        board.invitePanel = invitePanel;
        board.parameters = parameters;
        byte[] bytes = board.marshalWithCData();
        Board newBoard = Board.unmarshal(bytes);
        assertEquals(board.pointY, newBoard.pointY);
        assertEquals(board.parameters.backgroundColor, newBoard.parameters.backgroundColor);
        assertEquals(board.parameters.header.font, newBoard.parameters.header.font);
        assertEquals(board.left.content, newBoard.left.content);
        assertEquals(board.invitePanel.htmlText, newBoard.invitePanel.htmlText);
    }
}