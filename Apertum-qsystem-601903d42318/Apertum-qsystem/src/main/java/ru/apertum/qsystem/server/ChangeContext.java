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
package ru.apertum.qsystem.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import ru.apertum.qsystem.hibernate.SqlServers;
import ru.apertum.qsystem.hibernate.SqlServers.SqlServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Scanner;

import static ru.apertum.qsystem.common.Uses.ln;
import static ru.apertum.qsystem.common.Uses.lo;

/**
 * Утилита изменения Dao-контекста в инсталлированном приложении Класс изменения Dao-контекста в инсталлированном приложении. Консольный классик простого
 * редактирования XML-файла
 *
 * @author Evgeniy Egorov
 */
@SuppressWarnings("squid:S00112")
public class ChangeContext {

    /**
     * Файл настроек с описаниями подключения к БД. Это вообще-то константа.
     */
    private static final String FILE_PATH = "config/asfb.dat";
    /**
     * Файл настроек с описаниями подключения к БД. Это для перекрытия константы FILE_PATH.
     */
    private static String filePath = null;

    public static void setFilePath(String filePath) {
        ChangeContext.filePath = filePath;
    }

    /**
     * Получить путь к файлу настроек с описаниями подключения к БД.
     *
     * @return Вообще это константный путь, но он может быть перекрыт, например для тестов чтоб в тестовую БД слазить.
     */
    public static String getConfigFilePath() {
        return filePath != null ? filePath : FILE_PATH;
    }

    private static final String REG_EX_FLOAT = "-?\\d+(\\.\\d+)?";

    /**
     * Утилитка настройки.
     */
    public static void main(String[] args) throws IOException {
        final LinkedList<SqlServer> servs = load(new File(FILE_PATH));

        String str = "asd";
        while (!"".equals(str)) {
            ln(null);
            ln(null);

            if (servs.isEmpty()) {
                ln("No servers.");
            } else {
                ln("Servers:");
                int i = 0;
                for (SqlServer ser : servs) {
                    ln((++i) + " " + ser);
                }
                ln("");
            }

            if (!servs.isEmpty()) {

                ln("Press '+' for create DB server");
                ln("Press '-' for remove DB server");
                ln("Press 'm' for select main DB server");
                ln("Press 'c' for select current DB server");
                lo("Press number for edit DB server(enter - exit):");
                str = read();

                if ("+".equals(str)) {
                    servs.add(new SqlServer("serverDB_" + servs.size(), "root", "root",
                            "jdbc:mysql://127.0.0.1/qsystem?serverTimezone=UTC&useSSL=false", false, false));
                }
                if ("-".equals(str)) {
                    ln("");
                    ln("Choose number for removing('enter' for cancel): ");
                    int i = 0;
                    for (SqlServer ser : servs) {
                        ln("  " + (++i) + "  " + ser);
                    }
                    lo("Removing server:");
                    str = read();
                    if (!"".equals(str) && str.matches(REG_EX_FLOAT) && Integer.parseInt(str) >= 1 && Integer.parseInt(str) <= servs.size()) {
                        servs.remove(Integer.parseInt(str) - 1);
                    }
                    str = "-";
                }

                if ("m".equals(str)) {
                    ln("");
                    ln("Choose number for main('enter' for cancel): ");
                    int i = 0;
                    for (SqlServer ser : servs) {
                        ln("  " + (++i) + "  " + ser);
                    }
                    lo("Main server:");
                    str = read();
                    if (!"".equals(str) && str.matches(REG_EX_FLOAT) && Integer.parseInt(str) >= 1 && Integer.parseInt(str) <= servs.size()) {
                        servs.stream().forEach(ser -> ser.setMain(Boolean.FALSE));
                        servs.get(Integer.parseInt(str) - 1).setMain(Boolean.TRUE);
                    }
                    str = "m";
                }
                if ("c".equals(str)) {
                    ln("");
                    ln("Choose number for current('enter' for cancel): ");
                    int i = 0;
                    for (SqlServer ser : servs) {
                        ln("  " + (++i) + "  " + ser);
                    }
                    lo("Current server:");
                    str = read();
                    if (!"".equals(str) && str.matches(REG_EX_FLOAT) && Integer.parseInt(str) >= 1 && Integer.parseInt(str) <= servs.size()) {
                        servs.stream().forEach(ser -> ser.setCurrent(Boolean.FALSE));
                        servs.get(Integer.parseInt(str) - 1).setCurrent(Boolean.TRUE);
                    }
                    str = "c";
                }
                if (!"".equals(str) && str.matches(REG_EX_FLOAT) && Integer.parseInt(str) >= 1 && Integer.parseInt(str) <= servs.size()) {
                    final SqlServer ser = servs.get(Integer.parseInt(str) - 1);
                    String params = null;
                    if (ser.getUrl().contains("//") && ser.getUrl().indexOf('/', ser.getUrl().indexOf("//")) > -1) {
                        params = ser.getUrl().substring(ser.getUrl().indexOf("//") + 2, ser.getUrl().indexOf('/', ser.getUrl().indexOf("//") + 2));
                    }
                    ln("");
                    ln("");
                    ln("Choose number for edit('enter' for cancel): ");
                    ln("  1 user=" + ser.getUser());
                    ln("  2 password=" + ser.getParolcheg());
                    ln("  3 url=" + ser.getUrl());
                    ln("  4 driver=" + ser.getDriver());
                    if (params != null) {
                        ln("  5 adress=" + params);
                    }
                    lo("Parameter for edit:");
                    str = read();
                    if (!"".equals(str) && str.matches(REG_EX_FLOAT) && Integer.parseInt(str) >= 1 && Integer.parseInt(str) <= 4) {
                        switch (Integer.parseInt(str)) {
                            case 1:
                                ln("User");
                                ln("Old value: " + ser.getUser());
                                lo("New value: ");
                                ser.setUser(read());
                                break;
                            case 2:
                                ln("Password");
                                ln("Old  value: " + ser.getParolcheg());
                                lo("New  value: ");
                                ser.setParolcheg(read());
                                break;
                            case 3:
                                ln("URL");
                                ln("Old value:  " + ser.getUrl());
                                lo("New value:  ");
                                ser.setUrl(read());
                                break;
                            case 4:
                                ln(" Driver");
                                ln(" Old value: " + ser.getDriver());
                                lo(" New value: ");
                                ser.setDriver(read());
                                break;
                            case 5:
                                if (params != null) {
                                    ln("Adress");
                                    ln("Old value: " + params);
                                    lo("New value: ");
                                    ser.setUrl(ser.getUrl().replace(params, read()));
                                }
                                break;
                            default:
                                throw new IllegalArgumentException("Bad params = " + str);
                        }
                    }
                    str = "c";
                }
            } else {
                lo("Press '+' for create first DB server(enter - exit):");
                str = read();
                if ("+".equals(str)) {
                    lo("Enter a name of the new DB server:");
                    str = read();
                    servs.add(new SqlServer(str, "root", "root", "jdbc:mysql://127.0.0.1/qsystem?serverTimezone=UTC&useSSL=false", false, false));
                }
            }
        }

        ln(null);
        save(servs);
    }

    private static void save(LinkedList<SqlServer> servs) throws IOException {
        lo("Save context(1 - yes, any key - no): ");
        if ("1".equals(read())) {
            try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
                final String message;
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                message = gson.toJson(new SqlServers(servs));
                fos.write(message.getBytes("UTF-8"));
                fos.flush();
            }
            ln("Save and Exit");
        } else {
            ln("Exit without save");
        }
    }

    private static LinkedList<SqlServer> load(File conff) {
        final LinkedList<SqlServer> servs;
        if (conff.exists()) {
            StringBuilder str = new StringBuilder();
            try (FileInputStream fis = new FileInputStream(conff);
                 Scanner s = new Scanner(new InputStreamReader(fis, "UTF-8"))) {
                while (s.hasNextLine()) {
                    final String line = s.nextLine().trim();
                    str.append(line);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            Gson gson = new GsonBuilder().create();
            try {
                servs = gson.fromJson(str.toString(), SqlServers.class).getServers();
                if (servs == null) {
                    throw new RuntimeException("File error.");
                }
            } catch (JsonSyntaxException ex) {
                throw new RuntimeException("Data error. " + ex.toString());
            } finally {
                str.setLength(0);
            }
        } else {
            servs = new LinkedList<>();
        }
        return servs;
    }

    /**
     * Читаем введеную строку в консоли.
     *
     * @return введеная строка.
     */
    private static String read() {
        Scanner scanner = new Scanner(new InputStreamReader(System.in));
        return scanner.nextLine();
    }
}
