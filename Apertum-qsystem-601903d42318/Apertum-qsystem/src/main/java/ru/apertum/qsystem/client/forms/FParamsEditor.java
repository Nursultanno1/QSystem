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

import org.dom4j.Element;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import ru.apertum.qsystem.QSystem;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.exceptions.ClientException;
import ru.apertum.qsystem.server.board.Board;
import ru.apertum.qsystem.server.board.IParam;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Редактор параметров. Универсальный для простых параметров Created on 16 Апрель 2009 г., 19:26 Визуальный редактор параметров для редактирования параметров
 * главных табло. Умеет принять xml-параметры, отредактировать и вернуть результат. Универсальный для простых параметров. Параметры должны быть в таком формате
 * Параметры Параметер Наименование="высота" Тип="1" Значение="13"/ Параметер Наименование="ширина" Тип="2" Значение="13.2"/ Параметер Наименование="Заголовок"
 * Тип="3" Значение="Это текст"/ / Параметры
 *
 * @author Evgeniy Egorov
 */
@SuppressWarnings({"squid:S1192", "squid:S1172", "squid:S1450", "squid:S1604", "squid:S1161", "squid:MaximumInheritanceDepth"})
public class FParamsEditor extends AFBoardRedactor {

    private static ResourceMap localeMap = null;

    /**
     * Локализация строк.
     *
     * @param key ключ для строки.
     * @return локализованная строка.
     */
    public static String getLocaleMessage(String key) {
        if (localeMap == null) {
            localeMap = Application.getInstance(QSystem.class).getContext().getResourceMap(FParamsEditor.class);
        }
        return localeMap.getString(key);
    }

    /**
     * Это модель списка свойств.
     */
    class ParamList extends DefaultListModel {

        public ParamList(Element root) {
            root.elements(Uses.TAG_BOARD_PROP).stream().forEach(o ->
                    addElement(new IParam.Param((Element) o))
            );
        }

        public ParamList() {
            // тут все параметры собирем друг за другом.
            load("board", board);
        }

        private Object load(String parent, Object obj) {
            for (final Field field : obj.getClass().getFields()) {
                final IParam.ParamJaxb paramJaxb = IParam.ParamJaxbImpl.makeParameter(field, parent + "." + field.getName(), obj);
                if (paramJaxb == null) {
                    if (!field.getGenericType().equals(Board.Segment.class)) {
                        try {
                            load(field.getName(), field.get(obj));
                        } catch (IllegalAccessException e) {
                            QLog.l().logger().error(e);
                        }
                    }
                } else {
                    addElement(paramJaxb);
                }
            }
            return null;
        }
    }

    @Override
    protected void refresh() {
        listProps.setModel(board == null ? new ParamList(getParams()) : new ParamList());
    }

    /**
     * Creates new form FParamsEditor.
     */
    private FParamsEditor(JFrame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        init();
    }

    private void init() {
        try {
            if (FParamsEditor.class.getResource("/ru/apertum/qsystem/client/forms/resources/admin.png") != null) {
                setIconImage(ImageIO.read(FParamsEditor.class.getResource("/ru/apertum/qsystem/client/forms/resources/admin.png")));
            }
        } catch (IOException ex) {
            QLog.l().logger().error(ex);
        }
    }

    private static FParamsEditor editor = null;

    /**
     * Получить редактор просто в виде класса для дальнейших действий с ним. Singleton
     *
     * @param parent модально относительно этогй формы
     * @param modal  режим модальности
     * @return ссылка на форму редактора
     */
    public static FParamsEditor getParamsEditor(JFrame parent, boolean modal) {
        if (editor == null || (parent != editor.parentFarame || modal != editor.modalMode)) {
            editor = new FParamsEditor(parent, modal);
        }
        return editor;
    }

    /**
     * Отредактировать набор параметров.
     *
     * @param parent  модально относительно этой формы
     * @param modal   режим модальности
     * @param params  XML-параметры для редактирования
     * @param caption заголовок редактора.
     */
    public static void changeParams(JFrame parent, boolean modal, Element params, String caption) {
        getParamsEditor(parent, modal);
        Uses.setLocation(editor);
        editor.setParams(params);
        editor.setTitle(caption);
        editor.mainMenu.setVisible(false);
        editor.setVisible(true);
    }

    /**
     * Отредактировать набор параметров.
     *
     * @param parent  модально относительно этой формы
     * @param modal   режим модальности
     * @param board   java-параметры для редактирования
     * @param caption заголовок редактора.
     */
    public static void changeParamsJaxb(JFrame parent, boolean modal, Board board, String caption) {
        getParamsEditor(parent, modal);
        Uses.setLocation(editor);
        editor.setParams(board);
        editor.setTitle(caption);
        editor.mainMenu.setVisible(false);
        editor.setVisible(true);
    }

    private transient Board board;

    /**
     * Метод передачи данных в редактор для ихменения.
     *
     * @param board сами параметры в формате java
     */
    public void setParams(Board board) {
        this.board = board;
        refresh();
        onChangeParams();
    }

    /**
     * Меняем значение параметра.
     */
    @Action
    public void changeParamValue() {

        final IParam param = (IParam) listProps.getSelectedValue();
        if (param == null) {
            return;
        }
        if (param.isReadOnly()) {
            JOptionPane.showMessageDialog(null, "Parameter is read only.", "Impossible to change", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // тут надо фокус перекинуть, чтоб названия услуги изменилось с учетом приоритета.
        listProps.requestFocus();
        listProps.requestFocusInWindow();
        final String value = (String) JOptionPane.showInputDialog(this,
                param.getName(),
                getLocaleMessage("editor.dialog.title"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                param.getValue());
        //Если не выбрали, то выходим
        if (value != null) {
            try {
                switch (param.getType()) {
                    case Uses.BOARD_TYPE_INT:
                        param.setValue(Integer.parseInt(value));
                        break;
                    case Uses.BOARD_TYPE_DOUBLE:
                        param.setValue(Double.parseDouble(value));
                        break;
                    case Uses.BOARD_TYPE_STR:
                        param.setValue(value);
                        break;
                    case Uses.BOARD_TYPE_BOOL:
                        param.setValue("1".equals(value) || "true".equalsIgnoreCase(value));
                        break;
                    default:
                        throw new ClientException("Неправильный тип \"" + param.getType() + "\" параметра \"" + value + "\"");
                }
            } catch (NumberFormatException ex) {
                QLog.l().logger().error("Попытка ввода параметра неправильного типа. " + ex);
                JOptionPane.showMessageDialog(null, getLocaleMessage("editor.dialog2.title"), getLocaleMessage("editor.dialog2.caption"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "squid:S2177"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        listProps = new javax.swing.JList();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance().getContext().getResourceMap(FParamsEditor.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        listProps.setModel(new javax.swing.AbstractListModel() {
            String[] strings = {"Item 1", "Item 2", "Item 3", "Item 4", "Item 5"};

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        listProps.setName("listProps"); // NOI18N
        listProps.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                listPropsMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(listProps);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance().getContext().getActionMap(FParamsEditor.class, this);
        jButton1.setAction(actionMap.get("changeParamValue")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N

        jButton2.setAction(actionMap.get("hideRedactor")); // NOI18N
        jButton2.setText(resourceMap.getString("jButton2.text")); // NOI18N
        jButton2.setName("jButton2"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap(335, Short.MAX_VALUE)
                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jButton2)
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 660, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void listPropsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listPropsMouseClicked

        // назначение значения параметру.
        if (evt.getClickCount() == 2) {
            changeParamValue();
        }
    }//GEN-LAST:event_listPropsMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList listProps;
    // End of variables declaration//GEN-END:variables
}
