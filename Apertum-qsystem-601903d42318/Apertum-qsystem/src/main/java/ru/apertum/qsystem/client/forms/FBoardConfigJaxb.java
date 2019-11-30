package ru.apertum.qsystem.client.forms;

import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.exceptions.ClientException;
import ru.apertum.qsystem.common.model.INetProperty;
import ru.apertum.qsystem.server.board.Board;

import javax.swing.JFrame;
import java.io.ByteArrayInputStream;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class FBoardConfigJaxb extends FBoardConfig {

    private static FBoardConfigJaxb boardConfig;
    private transient Board board;

    public Board getBoard() {
        return board;
    }

    /**
     * Получить редактор для табло.
     */
    public static FBoardConfigJaxb getBoardConfig(JFrame parent, boolean modal) {
        if (boardConfig == null || (parent != boardConfig.parentFarame || modal != boardConfig.modalMode)) {
            boardConfig = new FBoardConfigJaxb(parent, modal);
        }
        return boardConfig;
    }

    /**
     * Creates new form FBoardConfig.
     */
    public FBoardConfigJaxb(JFrame parent, boolean modal) {
        super(parent, modal);
    }

    /**
     * Обновить параметры.
     */
    @Override
    protected void refresh() {
        try {
            board = Board.unmarshal(getParams());
        } catch (Exception ex) {
            throw new ClientException("XML data was destroyed.", ex);
        }
        //выставим размеры и видимость
        checkBoxUp.setSelected(board.top.visible);
        checkBoxLeft.setSelected(board.left.visible);
        checkBoxRight.setSelected(board.right.visible);
        checkBoxDown.setSelected(board.bottom.visible);
        checkBoxDown2.setSelected(board.bottom2.visible);

        setDividerLocation();
    }

    @Override
    protected void setDividerLocation() {
        spUp.setDividerLocation(board.top.size);
        spDown.setDividerLocation(board.bottom.size);
        spLeft.setDividerLocation(board.left.size);
        spRight.setDividerLocation(board.right.size);
        spDown2.setDividerLocation(board.bottom2.size); // вычитаемое это подгон адекватного открытия, видно пикселки на что-то еще жрутся.
    }

    @Override
    protected void saveForm() {
        board.top.visible = checkBoxUp.isSelected();
        board.left.visible = checkBoxLeft.isSelected();
        board.right.visible = checkBoxRight.isSelected();
        board.bottom.visible = checkBoxDown.isSelected();
        board.bottom2.visible = checkBoxDown2.isSelected();

        board.top.size = Uses.roundAs(spUp.getDividerLocation() / (spUp.getHeight() + 0.009d), 3);
        board.left.size = Uses.roundAs(spLeft.getDividerLocation() / (spLeft.getWidth() + 0.009d), 3);
        board.right.size = Uses.roundAs(spRight.getDividerLocation() / (spRight.getWidth() + 0.009d), 3);
        board.bottom.size = Uses.roundAs(spDown.getDividerLocation() / (spDown.getHeight() + 0.009), 3);
        board.bottom2.size = Uses.roundAs(spDown2.getDividerLocation() / (spDown2.getHeight() - 0.03), 3);

        try {
            final SAXReader xmlReader = new SAXReader();
            final Element root = xmlReader.read(new ByteArrayInputStream(board.marshalWithCData())).getRootElement();
            setParams(root);
        } catch (Exception ex) {
            throw new ClientException("XML data was not prepared for saving.", ex);
        }
    }

    @Override
    protected void changeParamsTop(JFrame owner, Element params, String caption, INetProperty netProps) {
        FBoardParams.changeParams(owner, board.top, caption, netProps);
    }

    @Override
    protected void changeParamsLeft(JFrame owner, Element params, String caption, INetProperty netProps) {
        FBoardParams.changeParams(owner, board.left, caption, netProps);
    }

    @Override
    protected void changeParamsRight(JFrame owner, Element params, String caption, INetProperty netProps) {
        FBoardParams.changeParams(owner, board.right, caption, netProps);
    }

    @Override
    protected void changeParamsBottom(JFrame owner, Element params, String caption, INetProperty netProps) {
        FBoardParams.changeParams(owner, board.bottom, caption, netProps);
    }

    @Override
    protected void changeParamsBottom2(JFrame owner, Element params, String caption, INetProperty netProps) {
        FBoardParams.changeParams(owner, board.bottom2, caption, netProps);
    }

    @Override
    protected void btnMainActionPerformed() {
        FParamsEditor.changeParamsJaxb(this.parentFarame, true, board, getLocaleMessage("cfg.params.main"));
    }
}
