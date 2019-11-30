/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.qsystem.ub485.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.exceptions.ServerException;

/**
 * Класс с адресами кнопочных устройств вызова.
 *
 * @author Evgeniy Egorov
 */
public class AddrProp {

    private final HashMap<Long, ButtonDevice> addrs = new HashMap<>();

    public Map<Long, ButtonDevice> getAddrs() {
        return addrs;
    }

    private static final File ADDR_FILE = new File("config/qub.adr");

    private AddrProp() {
        if (!ADDR_FILE.exists()) {
            throw new ServerException(ADDR_FILE.getAbsolutePath() + " not exists.");
        }
        try {
            init();
        } catch (IOException ex) {
            throw new ServerException(ex);
        }
    }

    private void init() throws IOException {
        try (FileInputStream fis = new FileInputStream(ADDR_FILE);
             Scanner s = new Scanner(fis)) {
            while (s.hasNextLine()) {
                addAddrsFromLine(s.nextLine().trim());
            }
        }
    }

    private void addAddrsFromLine(String line) {
        if (!line.isEmpty() && !line.startsWith("#")) {
            final String[] ss = line.split("=");
            if (ss.length != 2) {
                QLog.l().logger().error(ADDR_FILE.getAbsolutePath() + " contaned an error, line: \"" + line + "\"");
            } else {
                final String[] ssl = ss[1].split("\\s+");
                if ((ssl.length == 1 && Uses.isInt(ss[0])) || (ssl.length == 2 && Uses.isInt(ss[0]) && Uses.isInt(ssl[1]))) {
                    addrs.put(Long.valueOf(ss[0]), new ButtonDevice(Long.valueOf(ss[0]), Byte.parseByte(ssl[0]), ssl.length == 1 ? null : Long.parseLong(ssl[1])));
                    QLog.l().logger().trace(ADDR_FILE.getAbsolutePath() + " Read line: \"" + line + "\"");
                } else {
                    QLog.l().logger().error(ADDR_FILE.getAbsolutePath() + " contaned an error: \"" + line + "\" value \"" + Arrays.toString(ssl) + "\" is bad.");
                }
            }
        }
    }

    public static AddrProp getInstance() {
        return AddrPropHolder.INSTANCE;
    }

    private static class AddrPropHolder {

        private static final AddrProp INSTANCE = new AddrProp();
    }

    public ButtonDevice getAddr(Long userId) {
        return addrs.get(userId);
    }

    /**
     * Получаем кнопочное устройстро по его адресу.
     *
     * @param rsAddr адрес устройства.
     * @return Кнопочное устройство.
     */
    public ButtonDevice getAddrByRSAddr(byte rsAddr) {
        for (ButtonDevice adr : AddrProp.getInstance().getAddrs().values().toArray(new ButtonDevice[0])) {
            if (adr.addres == rsAddr) {
                return adr;
            }
        }
        return null;
    }

    /**
     * Тетилка.
     */
    @SuppressWarnings("squid:S106")
    public static void main(String[] ss) {
        System.out.println("addrs:");
        getInstance().addrs.keySet().stream().forEach(address ->
                System.out.println(address + "=" + getInstance().getAddr(address).addres
                        + " " + getInstance().getAddr(address).redirectServiceId));
    }
}
