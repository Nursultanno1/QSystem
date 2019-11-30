/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.qsystem.server.htmlboard;

import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.exceptions.ServerException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

/**
 * Настройки для HTML табло.
 *
 * @author Evgeniy Egorov
 */
@SuppressWarnings("squid:S1192")
public class HtmlBoardProps {

    private final HashMap<String, String> addrs = new HashMap<>();
    private final HashMap<String, String> ids = new HashMap<>();

    public Map<String, String> getAddrs() {
        return addrs;
    }

    private static final File ADDR_FILE = new File("config/html_main_board/links.adr");
    private static final String MAIN_PROPS_FILE = "config/html_main_board/mainboard.properties";
    final Properties settings = new Properties();

    private HtmlBoardProps() {
        try (FileInputStream fis = new FileInputStream(ADDR_FILE); Scanner s = new Scanner(fis)) {
            while (s.hasNextLine()) {
                final String line = s.nextLine().trim();
                if (!line.startsWith("#")) {
                    final String[] ss = line.split("=");
                    addrs.put(ss[0], ss[1]);
                    ids.put(ss[1], ss[0]);
                }
            }
        } catch (IOException ex) {
            throw new ServerException(ex);
        }

        File f = new File(MAIN_PROPS_FILE);
        if (f.exists()) {
            try (final FileInputStream inStream = new FileInputStream(f)) {
                final InputStreamReader reader = new InputStreamReader(inStream, "UTF-8");
                try {
                    settings.load(reader);
                } finally {
                    reader.close();
                }
            } catch (IOException ex) {
                throw new ServerException("Cant read version. " + ex);
            }

            topSize = Integer.parseInt(settings.getProperty("top.size", "0"));
            topUrl = settings.getProperty("top.url");
            leftSize = Integer.parseInt(settings.getProperty("left.size", "0"));
            leftUrl = settings.getProperty("left.url");
            rightSize = Integer.parseInt(settings.getProperty("right.size", "0"));
            rightUrl = settings.getProperty("right.url");
            bottomSize = Integer.parseInt(settings.getProperty("bottom.size", "0"));
            bottomUrl = settings.getProperty("bottom.url");
            needReload = "1".equals(settings.getProperty("need_reload", "1")) || "true".equals(settings.getProperty("need_reload", "true"));
        } else {
            try {
                boolean newFile = f.createNewFile();
                if (!newFile) {
                    QLog.l().logger().warn("Properties file wasnt crated!");
                }
            } catch (IOException ex) {
                QLog.l().logger().error("Properties file wasnt crated.", ex);
            }
        }
    }

    /**
     * Табло умеет сохранять настройки.
     */
    public void saveProps() {
        settings.setProperty("top.size", "" + topSize);
        settings.setProperty("top.url", topUrl);
        settings.setProperty("left.size", "" + leftSize);
        settings.setProperty("left.url", leftUrl);
        settings.setProperty("right.size", "" + rightSize);
        settings.setProperty("right.url", rightUrl);
        settings.setProperty("bottom.size", "" + bottomSize);
        settings.setProperty("bottom.url", bottomUrl);
        settings.setProperty("need_reload", needReload ? "1" : "0");
        try (final FileOutputStream fos = new FileOutputStream(MAIN_PROPS_FILE)) {
            settings.store(fos, "в пикселах / in pixel");
        } catch (IOException ex) {
            QLog.l().logger().error("Properties warent saved.", ex);
        }
    }

    public static HtmlBoardProps getInstance() {
        return AddrPropHolder.INSTANCE;
    }

    private static class AddrPropHolder {

        private static final HtmlBoardProps INSTANCE = new HtmlBoardProps();
    }

    public String getId(String adr) {
        return ids.get(adr);
    }

    public String getAddr(String id) {
        return addrs.get(id);
    }

    int topSize = 0;
    String topUrl = "";
    int leftSize = 0;
    String leftUrl = "";
    int rightSize = 0;
    String rightUrl = "";
    int bottomSize = 0;
    String bottomUrl = "";
    boolean needReload = true;

    public boolean isNeedReload() {
        return needReload;
    }

    public void setNeedReload(boolean needReload) {
        this.needReload = needReload;
    }

    public int getTopSize() {
        return topSize;
    }

    public String getTopUrl() {
        return topUrl;
    }

    public int getLeftSize() {
        return leftSize;
    }

    public String getLeftUrl() {
        return leftUrl;
    }

    public int getRightSize() {
        return rightSize;
    }

    public int getBottomSize() {
        return bottomSize;
    }

    public String getBottomUrl() {
        return bottomUrl;
    }

    public String getRightUrl() {
        return rightUrl;
    }
}
