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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import ru.apertum.qsystem.QSystem;
import ru.apertum.qsystem.client.Locales;
import ru.apertum.qsystem.common.CodepagePrintStream;
import ru.apertum.qsystem.common.GsonPool;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.exceptions.ClientException;
import ru.apertum.qsystem.common.exceptions.ServerException;
import ru.apertum.qsystem.hibernate.SqlServers;
import ru.apertum.qsystem.hibernate.SqlServers.SqlServer;
import ru.apertum.qsystem.server.ChangeContext;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Created on 25 Май 2009 г., 13:11 Форма конфигурирования подключения к СУБД и COM-порту.
 *
 * @author Evgeniy Egorov
 */
@SuppressWarnings({"squid:S1192", "squid:S1172", "squid:S1450", "squid:S1604", "squid:S1161", "squid:MaximumInheritanceDepth"})
public class FServerConfig extends javax.swing.JFrame {

    private static ResourceMap localeMap = null;

    private static String getLocaleMessage(String key) {
        if (localeMap == null) {
            localeMap = Application.getInstance(QSystem.class).getContext().getResourceMap(FServerConfig.class);
        }
        return localeMap.getString(key);
    }

    /**
     * Creates new form FServerConfig.
     */
    public FServerConfig() {
        initComponents();

        try {
            setIconImage(ImageIO.read(FAdmin.class.getResource("/ru/apertum/qsystem/client/forms/resources/client.png")));
        } catch (IOException ex) {
            QLog.l().logger().error(ex);
        }

        final File conff = new File(ChangeContext.getConfigFilePath());
        final LinkedList<SqlServer> servs = loadSqlServers(conff);

        final DefaultListModel<SqlServer> model = new DefaultListModel<>();
        servs.stream().forEach(model::addElement);
        listServs.setModel(model);
        panelParams.setVisible(false);

        listServs.addListSelectionListener((ListSelectionEvent e) -> {
            final SqlServer ser = (SqlServer) listServs.getSelectedValue();
            if (ser != null) {
                panelParams.setVisible(true);

                cbDB.setSelectedIndex("com.mysql.cj.jdbc.Driver".equalsIgnoreCase(ser.getDriver()) ? 0 : 1);
                textFieldServerUrl.setText(ser.getUrl());

                textFieldUserName.setText(ser.getUser());
                textFieldPassword.setText(ser.getParolcheg());
                cbCurrent.setSelected(ser.isCurrent());
                cbMain.setSelected(ser.isMain());
            }
        });
        if (!servs.isEmpty()) {
            listServs.setSelectedIndex(0);
        }

        int ii = 1;
        final ButtonGroup bg = new ButtonGroup();
        final String currLng = Locales.getInstance().getLangCurrName();
        for (String lng : Locales.getInstance().getAvailableLocales()) {
            final FServerConfig sc = this;
            final JRadioButtonMenuItem item = new JRadioButtonMenuItem(org.jdesktop.application.Application.getInstance(ru.apertum.qsystem.QSystem.class).getContext().getActionMap(FServerConfig.class, sc).get("setCurrentLang"));
            bg.add(item);
            item.setSelected(lng.equals(currLng));
            item.setText(lng); // NOI18N
            item.setName("QRadioButtonMenuItem" + (ii++)); // NOI18N
            menuLangs.add(item);
        }
    }

    private LinkedList<SqlServer> loadSqlServers(File conff) {
        final LinkedList<SqlServer> servs;
        if (conff.exists()) {
            StringBuilder str = new StringBuilder();
            try (FileInputStream fis = new FileInputStream(conff);
                 Scanner s = new Scanner(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
                while (s.hasNextLine()) {
                    final String line = s.nextLine().trim();
                    str.append(line);
                }
            } catch (IOException ex) {
                QLog.l().logger().error(ex);
                throw new ServerException(ex);
            }
            Gson gson = GsonPool.getInstance().borrowGson();
            try {
                servs = gson.fromJson(str.toString(), SqlServers.class).getServers();
                if (servs == null) {
                    throw new ServerException("File error.");
                }
            } catch (JsonSyntaxException ex) {
                throw new ServerException("Data error. " + ex.toString());
            } finally {
                GsonPool.getInstance().returnGson(gson);
            }
        } else {
            servs = new LinkedList<>();
        }
        return servs;
    }

    /**
     * Установим текущую локаль.
     */
    @Action
    public void setCurrentLang() {
        for (int i = 0; i < menuLangs.getItemCount(); i++) {
            if (((JRadioButtonMenuItem) menuLangs.getItem(i)).isSelected()) {
                Locales.getInstance().setLangCurrent(((JRadioButtonMenuItem) menuLangs.getItem(i)).getText());
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        popListServs = new javax.swing.JPopupMenu();
        miAdd = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        miRemove = new javax.swing.JMenuItem();
        jPanel1 = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        listServs = new javax.swing.JList();
        panelParams = new javax.swing.JPanel();
        btnCheckDBcnt = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        textFieldPassword = new javax.swing.JTextField();
        textFieldUserName = new javax.swing.JTextField();
        textFieldServerUrl = new javax.swing.JTextField();
        cbCurrent = new javax.swing.JCheckBox();
        cbMain = new javax.swing.JCheckBox();
        buttonSaveServer = new javax.swing.JButton();
        cbDB = new javax.swing.JComboBox();
        jPanel3 = new javax.swing.JPanel();
        btnSaveAll = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        buttonAdd = new javax.swing.JButton();
        buttonRemove = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        menuLangs = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();

        popListServs.setName("popListServs"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(ru.apertum.qsystem.QSystem.class).getContext().getResourceMap(FServerConfig.class);
        miAdd.setText(resourceMap.getString("miAdd.text")); // NOI18N
        miAdd.setName("miAdd"); // NOI18N
        miAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miAddActionPerformed(evt);
            }
        });
        popListServs.add(miAdd);

        jSeparator1.setName("jSeparator1"); // NOI18N
        popListServs.add(jSeparator1);

        miRemove.setText(resourceMap.getString("miRemove.text")); // NOI18N
        miRemove.setName("miRemove"); // NOI18N
        miRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miRemoveActionPerformed(evt);
            }
        });
        popListServs.add(miRemove);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel1.border.title"))); // NOI18N
        jPanel1.setName("jPanel1"); // NOI18N

        jSplitPane1.setDividerLocation(550);
        jSplitPane1.setDividerSize(7);
        jSplitPane1.setContinuousLayout(true);
        jSplitPane1.setName("jSplitPane1"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        listServs.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("listServs.border.title"))); // NOI18N
        listServs.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listServs.setComponentPopupMenu(popListServs);
        listServs.setName("listServs"); // NOI18N
        jScrollPane1.setViewportView(listServs);

        jSplitPane1.setLeftComponent(jScrollPane1);

        panelParams.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("panelParams.border.title"))); // NOI18N
        panelParams.setName("panelParams"); // NOI18N

        btnCheckDBcnt.setText(resourceMap.getString("btnCheckDBcnt.text")); // NOI18N
        btnCheckDBcnt.setName("btnCheckDBcnt"); // NOI18N
        btnCheckDBcnt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCheckDBcntActionPerformed(evt);
            }
        });

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        textFieldPassword.setText(resourceMap.getString("textFieldPassword.text")); // NOI18N
        textFieldPassword.setName("textFieldPassword"); // NOI18N

        textFieldUserName.setText(resourceMap.getString("textFieldUserName.text")); // NOI18N
        textFieldUserName.setName("textFieldUserName"); // NOI18N

        textFieldServerUrl.setText(resourceMap.getString("textFieldServerUrl.text")); // NOI18N
        textFieldServerUrl.setName("textFieldServerUrl"); // NOI18N

        cbCurrent.setText(resourceMap.getString("cbCurrent.text")); // NOI18N
        cbCurrent.setName("cbCurrent"); // NOI18N

        cbMain.setText(resourceMap.getString("cbMain.text")); // NOI18N
        cbMain.setName("cbMain"); // NOI18N

        buttonSaveServer.setText(resourceMap.getString("buttonSaveServer.text")); // NOI18N
        buttonSaveServer.setName("buttonSaveServer"); // NOI18N
        buttonSaveServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveServerActionPerformed(evt);
            }
        });

        cbDB.setModel(new javax.swing.DefaultComboBoxModel(new String[]{"MySQL", "H2"}));
        cbDB.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("cbDB.border.title"))); // NOI18N
        cbDB.setName("cbDB"); // NOI18N

        javax.swing.GroupLayout panelParamsLayout = new javax.swing.GroupLayout(panelParams);
        panelParams.setLayout(panelParamsLayout);
        panelParamsLayout.setHorizontalGroup(
                panelParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelParamsLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelParamsLayout.createSequentialGroup()
                                                .addGroup(panelParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(panelParamsLayout.createSequentialGroup()
                                                                .addComponent(cbMain)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(cbCurrent))
                                                        .addComponent(btnCheckDBcnt)
                                                        .addComponent(buttonSaveServer))
                                                .addGap(42, 277, Short.MAX_VALUE))
                                        .addGroup(panelParamsLayout.createSequentialGroup()
                                                .addGroup(panelParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(panelParamsLayout.createSequentialGroup()
                                                                .addGroup(panelParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(jLabel1)
                                                                        .addComponent(jLabel3)
                                                                        .addComponent(jLabel4))
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(panelParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                                        .addComponent(textFieldUserName, javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(textFieldServerUrl)
                                                                        .addComponent(textFieldPassword)))
                                                        .addGroup(panelParamsLayout.createSequentialGroup()
                                                                .addComponent(cbDB, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(0, 0, Short.MAX_VALUE)))
                                                .addContainerGap())))
        );
        panelParamsLayout.setVerticalGroup(
                panelParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelParamsLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(cbDB, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(panelParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel1)
                                        .addComponent(textFieldServerUrl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel3)
                                        .addComponent(textFieldUserName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(textFieldPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel4))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(panelParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(cbMain)
                                        .addComponent(cbCurrent))
                                .addGap(18, 18, 18)
                                .addComponent(btnCheckDBcnt)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonSaveServer)
                                .addContainerGap(15, Short.MAX_VALUE))
        );

        jSplitPane1.setRightComponent(panelParams);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1110, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jSplitPane1)
        );

        jPanel3.setBorder(new javax.swing.border.MatteBorder(null));
        jPanel3.setName("jPanel3"); // NOI18N

        btnSaveAll.setText(resourceMap.getString("btnSaveAll.text")); // NOI18N
        btnSaveAll.setName("btnSaveAll"); // NOI18N
        btnSaveAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onClickOK(evt);
            }
        });

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance().getContext().getActionMap(FServerConfig.class, this);
        btnClose.setAction(actionMap.get("quit")); // NOI18N
        btnClose.setText(resourceMap.getString("btnClose.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N

        buttonAdd.setText(resourceMap.getString("buttonAdd.text")); // NOI18N
        buttonAdd.setName("buttonAdd"); // NOI18N
        buttonAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddActionPerformed(evt);
            }
        });

        buttonRemove.setText(resourceMap.getString("buttonRemove.text")); // NOI18N
        buttonRemove.setName("buttonRemove"); // NOI18N
        buttonRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(buttonAdd)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonRemove)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnSaveAll)
                                .addGap(17, 17, 17)
                                .addComponent(btnClose)
                                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnSaveAll)
                                        .addComponent(btnClose)
                                        .addComponent(buttonAdd)
                                        .addComponent(buttonRemove))
                                .addContainerGap())
        );

        jMenuBar1.setName("jMenuBar1"); // NOI18N

        jMenu1.setText(resourceMap.getString("jMenu1.text")); // NOI18N
        jMenu1.setName("jMenu1"); // NOI18N

        menuLangs.setText(resourceMap.getString("menuLangs.text")); // NOI18N
        menuLangs.setName("menuLangs"); // NOI18N
        jMenu1.add(menuLangs);

        jMenuItem1.setAction(actionMap.get("quit")); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        jMenu1.add(jMenuItem1);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCheckDBcntActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCheckDBcntActionPerformed

        // Название драйвера
        final String driverName;
        final String url = textFieldServerUrl.getText().trim();
        switch (cbDB.getSelectedIndex()) {
            case 0:
                driverName = "com.mysql.cj.jdbc.Driver";
                break;
            case 1:
                driverName = "org.h2.Driver";
                break;
            default:
                throw new AssertionError();
        }
        QLog.l().logger().info(url + "\n" + textFieldUserName.getText() + "\n" + textFieldPassword.getText());
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException ex) {
            QLog.l().logger().error(ex);
            JOptionPane.showMessageDialog(this, getLocaleMessage("servercfg.dialog2.title") + "\n" + ex.getMessage() + "\n" + ex,
                    getLocaleMessage("servercfg.dialog2.caption"), JOptionPane.WARNING_MESSAGE);
            throw new ServerException(getLocaleMessage("servercfg.bd.fail"), ex);
        }

        try (Connection con = DriverManager.getConnection(url, textFieldUserName.getText(), textFieldPassword.getText())) {
            if (url.contains("bc:mysql://de") && url.contains("ev.apertum.") && url.contains("m.ru/qsk")) {
                long start = System.currentTimeMillis();
                boolean go = true;
                for (int i = 1; go && i <= 30; i++) {
                    long startIter = System.currentTimeMillis();

                    con.setAutoCommit(false);
                    try (PreparedStatement preparedStatement = con.prepareStatement("select distinct pa.ip ip "
                            + "FROM pager_results pa "
                            + "left join location lo on pa.ip=lo.ip "
                            + "where "
                            + "lo.place is null  "
                            + "limit 20")) {

                        final StringBuilder builder = new StringBuilder("INSERT INTO location (ip,country,place) VALUES ");
                        String comma = "";
                        boolean fail = true;

                        try (ResultSet resultSet = preparedStatement.executeQuery()) {
                            while (resultSet.next()) {
                                final String ip = resultSet.getString("ip");
                                final long ipInt = parseIp(ip);
                                Uses.lo(ip + " = " + ipInt + " --> ");
                                try (PreparedStatement statement = con.prepareStatement("select country_name, region_name, city_name "
                                        + "from ip2location_db3 "
                                        + "where "
                                        + "ip_from<=? and ip_to>=?")) {
                                    statement.setLong(1, ipInt);
                                    statement.setLong(2, ipInt);
                                    fail = true;
                                    try (ResultSet set = statement.executeQuery()) {
                                        while (set.next()) {
                                            fail = false;
                                            final String country = set.getString("country_name").replace("'", "");
                                            final String region = set.getString("region_name").replace("'", "");
                                            final String city = set.getString("city_name").replace("'", "");
                                            builder.append(comma).append("('" + ip + "','" + country + "','" + region + ", " + city + "')");
                                            comma = ",";
                                            Uses.ln("(" + ip + ", " + country + ", " + region + " - " + city + ")");
                                        }
                                    }
                                    if (fail) {
                                        break;
                                    }
                                }
                            }
                        }
                        if (comma.isEmpty()) {
                            Uses.ln("NO NEW LOCATIONS! good.");
                            go = false;
                        } else {
                            if (fail) {
                                Uses.ln("FAILED");
                                go = false;
                            } else {
                                try (PreparedStatement statement = con.prepareStatement(builder.toString())) {
                                    if (statement.executeUpdate() > 0) {
                                        con.commit();
                                        Uses.ln("OK");
                                    } else {
                                        con.rollback();
                                        Uses.ln("FAIL");
                                        go = false;
                                    }
                                }
                            }
                        }
                    }
                    long millis = System.currentTimeMillis() - startIter;
                    Uses.ln(String.format("Iteration %d time: %d min, %d sec", i,
                            TimeUnit.MILLISECONDS.toMinutes(millis),
                            TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))));
                }
                long millis = System.currentTimeMillis() - start;
                Uses.ln(String.format("Total time: %d min, %d sec",
                        TimeUnit.MILLISECONDS.toMinutes(millis),
                        TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, getLocaleMessage("servercfg.dialog2.title") + ".\nНомер ошибки: " + ex + "\n" + ex,
                    getLocaleMessage("servercfg.dialog2.caption"), JOptionPane.WARNING_MESSAGE);
            throw new ServerException(getLocaleMessage("servercfg.bd.fail"), ex);
        } catch (Exception ex) {
            QLog.l().logger().error(ex);
            JOptionPane.showMessageDialog(this, getLocaleMessage("servercfg.dialog2.title") + ". " + ex + "\n" + ex,
                    getLocaleMessage("servercfg.dialog2.caption"), JOptionPane.WARNING_MESSAGE);
            throw new ServerException(getLocaleMessage("servercfg.bd.fail"), ex);
        }
        JOptionPane.showMessageDialog(this, getLocaleMessage("servercfg.dialog3.title"), getLocaleMessage("servercfg.dialog3.caption"), JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_btnCheckDBcntActionPerformed

    /**
     * Преобразовать ip адрес в строковом виде в целое число.
     *
     * @param address ip адрес в строковом виде.
     * @return в целое число.
     */
    public static long parseIp(String address) {
        long result = 0;
        // iterate over each octet
        for (String part : address.split(Pattern.quote("."))) {
            // shift the previously parsed bits over by 1 byte
            result = result << 8;
            // set the low order bits to the current octet
            result |= Long.parseLong(part);
        }
        return result;
    }

    /**
     * Преобразовать ip адрес в целочисленном строковое представление.
     *
     * @param ip ip адрес в целочисленном виде.
     * @return ip адрес в строковом виде.
     */
    public static String parseIp(long ip) {
        final long[] ints = new long[4];
        ints[0] = ip & 0xFF;
        ints[1] = (ip >> 8) & 0xFF;
        ints[2] = (ip >> 16) & 0xFF;
        ints[3] = (ip >> 24) & 0xFF;
        return String.format("%d.%d.%d.%d", ints[3], ints[2], ints[1], ints[0]);
    }

    private void onClickOK(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onClickOK
        // в темповый файл
        try {
            try (FileOutputStream fos = new FileOutputStream(ChangeContext.getConfigFilePath())) {
                final String message;
                Gson gson = GsonPool.getInstance().borrowGson();
                try {
                    final LinkedList<SqlServer> servs = new LinkedList<>();
                    for (int i = 0; i < listServs.getModel().getSize(); i++) {
                        servs.add((SqlServer) (listServs.getModel().getElementAt(i)));
                    }
                    message = gson.toJson(new SqlServers(servs));
                } finally {
                    GsonPool.getInstance().returnGson(gson);
                }
                fos.write(message.getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }
        } catch (Exception ex) {
            throw new ClientException(ex);
        }
        JOptionPane.showMessageDialog(this, "List of connections was saved successfully.", "Saving", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_onClickOK

    private void buttonSaveServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveServerActionPerformed

        final SqlServer ser = (SqlServer) listServs.getSelectedValue();
        if (ser != null) {

            ser.setUrl(textFieldServerUrl.getText().trim());
            switch (cbDB.getSelectedIndex()) {
                case 0:
                    ser.setDriver("com.mysql.cj.jdbc.Driver");
                    break;
                case 1:
                    ser.setDriver("org.h2.Driver");
                    break;
                default:
                    throw new AssertionError();
            }

            ser.setUser(textFieldUserName.getText());
            ser.setParolcheg(textFieldPassword.getText());
            setMainAndCurrent();
            ser.setCurrent(cbCurrent.isSelected());
            ser.setMain(cbMain.isSelected());

            final int i = listServs.getSelectedIndex();
            listServs.setModel(listServs.getModel());
            listServs.setSelectedIndex(i);
            JOptionPane.showMessageDialog(this, "DB connection was updated in list successfully.", "Updating", JOptionPane.INFORMATION_MESSAGE);
        }
    }//GEN-LAST:event_buttonSaveServerActionPerformed

    private void setMainAndCurrent() {
        if (cbCurrent.isSelected()) {
            for (int i = 0; i < listServs.getModel().getSize(); i++) {
                ((SqlServer) (listServs.getModel().getElementAt(i))).setCurrent(false);
            }
        }
        if (cbMain.isSelected()) {
            for (int i = 0; i < listServs.getModel().getSize(); i++) {
                ((SqlServer) (listServs.getModel().getElementAt(i))).setMain(false);
            }
        }
    }

    private void miAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miAddActionPerformed
        final String inputData = JOptionPane.showInputDialog(this, getLocaleMessage("name.new.server"), getLocaleMessage("addind.server"), 3);
        if (inputData == null || inputData.isEmpty()) {
            return;
        }
        ((DefaultListModel) (listServs.getModel())).addElement(new SqlServer(inputData, "root", "root",
                "jdbc:mysql://127.0.0.1/qsystem?serverTimezone=UTC&useSSL=false", false, false));
        listServs.setSelectedIndex(listServs.getModel().getSize() - 1);
    }//GEN-LAST:event_miAddActionPerformed

    private void miRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miRemoveActionPerformed
        final SqlServer ser = (SqlServer) listServs.getSelectedValue();
        if (ser != null) {
            ((DefaultListModel) (listServs.getModel())).removeElement(ser);
            listServs.setModel(listServs.getModel());
            if (listServs.getModel().getSize() > 0) {
                listServs.setSelectedIndex(0);
            } else {
                panelParams.setVisible(false);
            }
        }
    }//GEN-LAST:event_miRemoveActionPerformed

    private void buttonAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddActionPerformed
        miAddActionPerformed(null);
    }//GEN-LAST:event_buttonAddActionPerformed

    private void buttonRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveActionPerformed
        miRemoveActionPerformed(null);
    }//GEN-LAST:event_buttonRemoveActionPerformed

    static boolean ide = false;

    /**
     * Утилита настройки сервера.
     */
    public static void main(String[] args) {
        Locale.setDefault(Locales.getInstance().getLangCurrent());
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                //System.out.println(info.getTitle())
                /*Metal Nimbus CDE/Motif Windows   Windows Classic  */
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            // ignore
        }

        for (String str : args) {
            if (str.startsWith("ide")) {
                ide = true;
                break;
            }
        }
        //Установка вывода консольных сообщений в нужной кодировке
        if ("\\".equals(File.separator)) {
            try {
                String consoleEnc = System.getProperty("console.encoding", "Cp866");
                System.setOut(new CodepagePrintStream(System.out, consoleEnc)); //NOSONAR
                System.setErr(new CodepagePrintStream(System.err, consoleEnc)); //NOSONAR
            } catch (UnsupportedEncodingException e) {
                QLog.l().logger().error("Unable to setup console codepage: " + e);
            }
        }

        java.awt.EventQueue.invokeLater(() -> {
            final FServerConfig sc = new FServerConfig();
            Uses.setLocation(sc);
            sc.setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCheckDBcnt;
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnSaveAll;
    private javax.swing.JButton buttonAdd;
    private javax.swing.JButton buttonRemove;
    private javax.swing.JButton buttonSaveServer;
    private javax.swing.JCheckBox cbCurrent;
    private javax.swing.JComboBox cbDB;
    private javax.swing.JCheckBox cbMain;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JList listServs;
    private javax.swing.JMenu menuLangs;
    private javax.swing.JMenuItem miAdd;
    private javax.swing.JMenuItem miRemove;
    private javax.swing.JPanel panelParams;
    private javax.swing.JPopupMenu popListServs;
    private javax.swing.JTextField textFieldPassword;
    private javax.swing.JTextField textFieldServerUrl;
    private javax.swing.JTextField textFieldUserName;
    // End of variables declaration//GEN-END:variables
}
