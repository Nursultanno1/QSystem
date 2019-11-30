/*
 * Copyright (C) 2012 Evgeniy Egorov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ru.apertum.qsystem.client.model;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;
import javax.swing.border.EmptyBorder;

import com.sun.java.swing.plaf.motif.MotifComboBoxUI; //NOSONAR
import com.sun.java.swing.plaf.windows.WindowsComboBoxUI; //NOSONAR
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Модель для дерева услуг выбора в комбобоксе.
 */
public final class JTreeComboBox extends JComboBox implements TreeSelectionListener {

    protected Color selectedBackground;
    protected Color selectedForeground;
    protected Color backgroundColor;
    protected Color foregroundColor;
    protected JTree tree = null;

    /**
     * Модель для дерева услуг выбора в комбобоксе.
     */
    public JTreeComboBox(TreeModel treeModel) {
        initializeTree();
        setTreeModel(treeModel);
    }

    private void initializeTree() {
        tree = new JTree();
        tree.setCellRenderer(new CustomTreeRenderer());
        tree.setVisibleRowCount(8);
        tree.setBackground(backgroundColor);
        tree.addTreeSelectionListener(this);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);

    }

    /**
     * Установим модель.
     *
     * @param treeModel эту.
     */
    public void setTreeModel(TreeModel treeModel) {
        tree.setModel(treeModel);

        setSelection(treeModel.getRoot());
    }

    /**
     * makeVisible.
     *
     * @param treePath путь в дереве.
     */
    public void makeVisible(TreePath treePath) {
        tree.makeVisible(treePath);
    }

    @Override
    public void setSelectedItem(Object item) {
        if (item == null) {
            removeAllItems();
            return;
        }
        if (treeContains((TreeNode) tree.getModel().getRoot(), (TreeNode) item)) {
            setSelection(item);
        }
    }

    private void setSelection(Object item) {
        removeAllItems();
        addItem(item);
    }

    private boolean treeContains(TreeNode root, TreeNode node) {
        if (root.getIndex(node) != -1) {
            return true;
        }

        for (int i = 0; i < root.getChildCount(); i++) {
            if (treeContains(root.getChildAt(i), node)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void updateUI() {
        ComboBoxUI cui = (ComboBoxUI) UIManager.getUI(this);
        if (cui instanceof MetalComboBoxUI) {
            cui = new MetalTreeComboBoxUI();
        } else {
            if (cui instanceof MotifComboBoxUI) {
                cui = new MotifTreeComboBoxUI();
            } else {
                if (cui instanceof WindowsComboBoxUI) {
                    cui = new WindowsTreeComboBoxUI();
                }
            }
        }
        setUI(cui);
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        TreeNode selectedNode = (TreeNode) tree.getLastSelectedPathComponent();

        if (selectedNode == null) {
            return;
        }

        setSelection(selectedNode);

        hidePopup();
    }

    // Inner classes are used purely to keep TreeComboBox component in one file
    //////////////////////////////////////////////////////////////
    // UI Inner classes -- one for each supported Look and Feel
    //////////////////////////////////////////////////////////////
    class MetalTreeComboBoxUI extends MetalComboBoxUI {

        @Override
        protected ComboPopup createPopup() {
            return new TreePopup(comboBox);
        }
    }

    class WindowsTreeComboBoxUI extends WindowsComboBoxUI {

        @Override
        protected ComboPopup createPopup() {
            return new TreePopup(comboBox);
        }
    }

    class MotifTreeComboBoxUI extends MotifComboBoxUI {

        @Override
        protected ComboPopup createPopup() {
            return new TreePopup(comboBox);
        }
    }

    //////////////////////////////////////////////////////////////
    // TreePopup inner class
    //////////////////////////////////////////////////////////////
    final class TreePopup implements ComboPopup, MouseMotionListener,
        MouseListener, KeyListener, PopupMenuListener {

        protected JComboBox comboBox;
        protected JPopupMenu popup;

        public TreePopup(JComboBox comboBox) {
            this.comboBox = comboBox;

            // check Look and Feel
            backgroundColor = UIManager.getColor("ComboBox.background");
            foregroundColor = UIManager.getColor("ComboBox.foreground");
            selectedBackground = UIManager.getColor("ComboBox.selectionBackground");
            selectedForeground = UIManager.getColor("ComboBox.selectionForeground");

            selectedBackground = new Color(153, 153, 204);
            initializePopup();
        }

        //========================================
        // begin ComboPopup method implementations
        //
        @Override
        public void show() {
            updatePopup();
            popup.show(comboBox, 0, comboBox.getHeight());
            popup.setVisible(true);
        }

        @Override
        public void hide() {
            popup.setVisible(false);
        }

        protected JList list = new JList();

        @Override
        public JList getList() {
            return list;
        }

        @Override
        public MouseListener getMouseListener() {
            return this;
        }

        @Override
        public MouseMotionListener getMouseMotionListener() {
            return this;
        }

        @Override
        public KeyListener getKeyListener() {
            return this;
        }

        @Override
        public boolean isVisible() {
            return popup.isVisible();
        }

        @Override
        public void uninstallingUI() {
            popup.removePopupMenuListener(this);
        }

        //
        // end ComboPopup method implementations
        //======================================
        //===================================================================
        // begin Event Listeners
        //
        // MouseListener
        @Override
        public void mousePressed(MouseEvent e) {
            // not gor using.
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // not gor using.
        }

        // something else registered for MousePressed
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }
            if (!comboBox.isEnabled()) {
                return;
            }
            if (comboBox.isEditable()) {
                comboBox.getEditor().getEditorComponent().requestFocus();
            } else {
                comboBox.requestFocus();
            }
            togglePopup();
        }

        protected boolean mouseInside = false;

        @Override
        public void mouseEntered(MouseEvent e) {
            mouseInside = true;
        }

        @Override
        public void mouseExited(MouseEvent e) {
            mouseInside = false;
        }

        // MouseMotionListener
        @Override
        public void mouseDragged(MouseEvent e) {
            // not gor using.
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            // not gor using.
        }

        // KeyListener
        @Override
        public void keyPressed(KeyEvent e) {
            // not gor using.
        }

        @Override
        public void keyTyped(KeyEvent e) {
            // not gor using.
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE
                || e.getKeyCode() == KeyEvent.VK_ENTER) {
                togglePopup();
            }
        }

        /**
         * Variables hideNext and mouseInside are used to hide the popupMenu by clicking the mouse in the JComboBox.
         */
        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
            // not gor using.
        }

        protected boolean hideNext = false;

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            hideNext = mouseInside;
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            // not gor using.
        }

        //
        // end Event Listeners
        //=================================================================
        //===================================================================
        // begin Utility methods
        //
        protected void togglePopup() {
            if (isVisible() || hideNext) {
                hide();
            } else {
                show();
            }
            hideNext = false;
        }

        //
        // end Utility methods
        //=================================================================
        JScrollPane scroller = new JScrollPane();

        protected void initializePopup() {
            popup = new JPopupMenu();
            popup.setLayout(new BorderLayout());
            popup.setBorder(new EmptyBorder(0, 0, 0, 0));
            popup.addPopupMenuListener(this);
            popup.add(scroller);
            popup.pack();
        }

        protected void updatePopup() {
            scroller.setViewportView(tree);

            int width = comboBox.getWidth();
            int height = (int) tree.getPreferredScrollableViewportSize().getHeight();

            popup.setPopupSize(width, height);
        }
    }

    @SuppressWarnings("squid:MaximumInheritanceDepth")
    class CustomTreeRenderer extends DefaultTreeCellRenderer {

        protected transient Object lastNode = null;

        public CustomTreeRenderer() {
            setOpaque(true);
            tree.addMouseMotionListener(new MouseMotionAdapter() {

                @Override
                public void mouseMoved(MouseEvent me) {
                    TreePath treePath = tree.getPathForLocation(me.getX(), me.getY());
                    Object obj;
                    if (treePath != null) {
                        obj = treePath.getLastPathComponent();
                    } else {
                        obj = null;
                    }
                    if (obj != lastNode) {
                        lastNode = obj;
                        tree.repaint();
                    }
                }
            });
        }

        @Override
        public Component getTreeCellRendererComponent(
            JTree tree, Object value,
            boolean isSelected, boolean isExpanded,
            boolean isLeaf, int row, boolean hasFocus) {

            JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value,
                isSelected, isExpanded, isLeaf, row, hasFocus);
            if (value == lastNode || (lastNode == null && isSelected)) {
                label.setBackground(selectedBackground);
                label.setForeground(selectedForeground);
            } else {
                label.setBackground(backgroundColor);
                label.setForeground(foregroundColor);
            }

            return label;
        }
    }

    //////////////////////////////////////////////////////////////
    // This is only included to provide a sample GUI
    //////////////////////////////////////////////////////////////

    /**
     * Тестилка.
     */
    public static void main(String[] args) {
        DefaultMutableTreeNode root1 = new DefaultMutableTreeNode("test1");
        DefaultTreeModel treeModel1 = new DefaultTreeModel(root1);
        DefaultMutableTreeNode root2 = new DefaultMutableTreeNode("test2");
        DefaultTreeModel treeModel2 = new DefaultTreeModel(root2);

        for (int i = 1; i < 10; i++) {
            treeModel1.insertNodeInto(new DefaultMutableTreeNode("Node" + i), root1, root1.getChildCount());
        }

        for (int i = 1; i < 10; i++) {
            treeModel2.insertNodeInto(new DefaultMutableTreeNode("Node" + i), root2, root2.getChildCount());
        }

        JFrame frame = new JFrame();
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(new JLabel("Tree 1:"));
        JTreeComboBox cb = new JTreeComboBox(treeModel1);
        panel.add(cb);
        panel.add(new JLabel("Tree 2:"));
        JTreeComboBox dcb = new JTreeComboBox(treeModel2);
        dcb.setEditable(true);
        panel.add(dcb);

        contentPane.add(panel);
        contentPane.add(Box.createVerticalStrut(200));
        contentPane.add(Box.createVerticalGlue());

        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setSize(500, 200);
        frame.setVisible(true);
    }
}
