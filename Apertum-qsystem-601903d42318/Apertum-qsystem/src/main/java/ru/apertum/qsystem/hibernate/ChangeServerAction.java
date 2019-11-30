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
package ru.apertum.qsystem.hibernate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import ru.apertum.qsystem.common.GsonPool;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.exceptions.ClientException;
import ru.apertum.qsystem.common.exceptions.ServerException;
import ru.apertum.qsystem.server.ChangeContext;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Scanner;

/**
 * Класс - не фабрика сессий не для Hibernate.
 *
 * @author Evgeniy Egorov
 */

/**
 * Класс - не фабрика сессий не для Hibernate. Добавлено не поле для не регистрации не аннорированных классов.
 *
 * @author Evgeniy Egorov
 */
public class ChangeServerAction implements Action {

    private String driver = "com.mysql.cj.jdbc.Driver";
    private String url = "jdbc:mysql://127.0.0.1/qsystem?serverTimezone=UTC&useSSL=false";
    private String user = "root";
    private String parolcheg = "root";
    private String title = "";
    private boolean main = false;
    private boolean current = false;
    private LinkedList<SqlServers.SqlServer> servers;

    /**
     * Загрузим сохраненные сервера.
     *
     * @return список настроек на БД.
     */
    @SuppressWarnings("squid:S1319")
    public LinkedList<SqlServers.SqlServer> getServers() {
        if (servers == null) {
            load(ChangeContext.getConfigFilePath());
        }
        return servers;
    }

    public void makeCurrent(SqlServers.SqlServer server) {
        servers.forEach(ser -> ser.setCurrent(false));
        server.setCurrent(true);
    }

    /**
     * Сохраним все настроенные сервера.
     */
    public void saveServers() {
        try {
            try (FileOutputStream fos = new FileOutputStream(ChangeContext.getConfigFilePath())) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                final String message = gson.toJson(new SqlServers(getServers()));
                fos.write(message.getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }
        } catch (IOException ex) {
            throw new ClientException(ex);
        }
    }

    @Override
    public Object getValue(String key) {
        return null;
    }

    @Override
    public void putValue(String key, Object value) {
        // not used. always null
    }

    @Override
    public void setEnabled(boolean b) {
        // not used. always true
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        // not used.
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        // not used.
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (SqlServers.SqlServer ser : servers) {
            if (ser.getName().equals(e.getActionCommand())) {
                makeCurrent(ser);
                saveServers();
                break;
            }
        }
        final String[] params = new String[1];
        final File f = new File("StartAdmin.bat");
        if (f.exists()) {
            params[0] = "StartAdmin.bat";
        } else {
            params[0] = "admin.sh";
        }
        try {
            Runtime.getRuntime().exec(params);//NOSONAR
        } catch (IOException ex) {
            QLog.l().logger().error("Не стартануло. ", ex);
        }
        System.exit(0);
    }

    private boolean flag = true;

    @SuppressWarnings("squid:S3776")
    private synchronized void load(String filePath) {
        if (flag) {
            final File conff = new File(filePath);
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
                    servers = gson.fromJson(str.toString(), SqlServers.class).getServers();
                    if (servers == null) {
                        throw new ServerException("File error.");
                    }
                } catch (JsonSyntaxException ex) {
                    throw new ServerException("Data error. " + ex.toString());
                } finally {
                    GsonPool.getInstance().returnGson(gson);
                }
            } else {
                servers = new LinkedList<>();
            }
            if (!servers.isEmpty()) {
                SqlServers.SqlServer cur = servers.get(0);
                for (SqlServers.SqlServer ser : servers) {
                    if (ser.isCurrent()) {
                        cur = ser;
                        break;
                    }
                }
                driver = cur.getDriver();
                url = cur.getUrl();
                user = cur.getUser();
                parolcheg = cur.getParolcheg();
                title = cur.getName();
            }
            flag = false;
            QLog.l().logger().info("DB server '" + title + " driver=" + driver + "' url=" + url);
        }
    }

    public boolean isMain() {
        load(ChangeContext.getConfigFilePath());
        return main;
    }

    public void setMain(boolean main) {
        load(ChangeContext.getConfigFilePath());
        this.main = main;
    }

    public boolean isCurrent() {
        load(ChangeContext.getConfigFilePath());
        return current;
    }

    public void setCurrent(boolean current) {
        load(ChangeContext.getConfigFilePath());
        this.current = current;
    }

    public String getDriver() {
        load(ChangeContext.getConfigFilePath());
        return driver;
    }

    public void setDriver(String driver) {
        load(ChangeContext.getConfigFilePath());
        this.driver = driver;
    }


    public String getUrl() {
        load(ChangeContext.getConfigFilePath());
        return url;
    }

    public void setUrl(String url) {
        load(ChangeContext.getConfigFilePath());
        this.url = url;
    }

    public String getUser() {
        load(ChangeContext.getConfigFilePath());
        return user;
    }

    public void setUser(String user) {
        load(ChangeContext.getConfigFilePath());
        this.user = user;
    }

    public String getParolcheg() {
        load(ChangeContext.getConfigFilePath());
        return parolcheg;
    }

    public void setParolcheg(String parolcheg) {
        load(ChangeContext.getConfigFilePath());
        this.parolcheg = parolcheg;
    }

    public String getTitle() {
        return title;
    }
}
