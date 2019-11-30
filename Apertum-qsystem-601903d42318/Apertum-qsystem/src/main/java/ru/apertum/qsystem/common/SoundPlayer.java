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
package ru.apertum.qsystem.common;

import ru.apertum.qsystem.client.QProperties;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.server.ServerProps;
import ru.apertum.qsystem.server.model.QService;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.apertum.qsystem.common.QLog.log;

/**
 * Класс проигрывания звуковых ресурсов и файлов. Создает отдельный поток для каждого проигрыша, но игоает синхронизированно. По этому все ресурсы проиграются
 * друг за другом и это не будет тормозить основной поток. Воспроизведение кучи мелких файлов глючит, накладываются др. на др.
 *
 * @author Evgeniy Egorov
 */
@SuppressWarnings({"squid:S1319"})
public class SoundPlayer implements Runnable {

    // путь к звуковым файлам
    public static final String SAMPLES_PACKAGE = "/ru/apertum/qsystem/server/sound/";

    public SoundPlayer(LinkedList<String> resourceList) {
        this.resourceList = resourceList;
    }

    /**
     * Тут храним имя ресурса для загрузки.
     */
    private final LinkedList<String> resourceList;

    /**
     * Проиграть звуковой ресурс.
     *
     * @param resourceName имя проигрываемого ресурса
     */
    public static void play(String resourceName) {
        final LinkedList<String> resourceList = new LinkedList<>();
        resourceList.add(resourceName);
        play(resourceList);
    }

    /**
     * Проиграть набор звуковых ресурсов.
     *
     * @param resourceList список имен проигрываемых ресурсов
     */
    public static void play(LinkedList<String> resourceList) {
        // и запускаем новый вычислительный поток (см. ф-ю run())
        final Thread playThread = new Thread(new SoundPlayer(resourceList));
        playThread.setPriority(Thread.NORM_PRIORITY);
        playThread.start();
    }

    /**
     * Asks the user to select a file to play.
     */
    public File getFileToPlay() {
        File file = null;
        JFrame frame = new JFrame();
        JFileChooser chooser = new JFileChooser(".");
        int returnvalue = chooser.showDialog(frame, "Select File to Play");
        if (returnvalue == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
        }
        return file;
    }

    private static final Object SYNC = new Object();

    @Override
    public void run() {
        synchronized (SYNC) {
            doSounds(this, resourceList);
        }
    }

    /**
     * Листенер, срабатываюшщий при начале проигрывания семплов.
     */
    private static ActionListener startListener = null;

    public static ActionListener getStartListener() {
        return startListener;
    }

    public static void setStartListener(ActionListener startListener) {
        SoundPlayer.startListener = startListener;
    }

    /**
     * Событие завершения проигрывания семплов.
     */
    private static ActionListener finishListener = null;

    public static ActionListener getFinishListener() {
        return finishListener;
    }

    public static void setFinishListener(ActionListener finishListener) {
        SoundPlayer.finishListener = finishListener;
    }

    private static synchronized void doSounds(Object o, LinkedList<String> resourceList) {
        if (startListener != null) {
            startListener.actionPerformed(new ActionEvent(o, 1, "start do sounds"));
        }
        resourceList.stream().filter(resource -> {
            final InputStream stream = resource.getClass().getResourceAsStream(resource);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // press it.
                }
                return true;
            } else {
                return Paths.get(resource).toFile().exists();
            }
        }).forEach(SoundPlayer::doSound);
        if (finishListener != null) {
            finishListener.actionPerformed(new ActionEvent(o, 1, "finish do sounds"));
        }
    }

    private static synchronized void doSound(String resourceName) {
        final String audioPlaybackVariant = ServerProps.getInstance().getProperty(QProperties.SECTION_SYSTEM, "audio_playback_variant", "1");
        log().debug("Try to play sound \"" + resourceName + "\" SYSTEM.audio_playback_variant = " + audioPlaybackVariant);
        switch (audioPlaybackVariant) {
            case "2": {
                Audio.playSound(resourceName);
                break;
            }
            case "3": {
                Audio.playWave(resourceName);
                break;
            }
            case "4": {
                Audio.playBasic(resourceName);
                break;
            }
            default: {
                Audio.playSound(resourceName);
            }
        }
    }

    /**
     * Разбить фразу на звуки и сформировать набор файлов для воспроизведения. Упрощенный вариант с поиском существующих семплов.
     *
     * @param path   путь, где лежать звуковые ресурсы, это могут быть файлы на диске или ресурсы в jar
     * @param phrase фраза для разбора
     * @return список файлов для воспроизведения фразы
     */
    public static LinkedList<String> toSoundSimple2(String path, String phrase) {
        final LinkedList<String> res = new LinkedList<>();

        // Разделим на буквы и цыфры
        Matcher m = Pattern.compile("\\d").matcher(phrase);

        // Добавим лидирующие буквы если они есть во фразе и в ресурсах
        if (m.find()) {
            for (int i = 0; i < m.start(); i++) {
                String elem = phrase.substring(i, i + 1);
                final String fileName = reRus(path, elem.toLowerCase());
                if (fileName == null) {
                    continue;
                }
                res.add(path + fileName.toLowerCase() + ".wav");
            }
            phrase = phrase.substring(m.start());
        }

        m = Pattern.compile("\\D").matcher(phrase);
        final LinkedList<String> last = new LinkedList<>();

        // Добавим лидирующие буквы если они есть во фразе и в ресурсах
        if (m.find()) {
            final int b = m.start();
            for (int i = b; i < phrase.length(); i++) {
                String elem = phrase.substring(i, i + 1);
                final String fileName = reRus(path, elem.toLowerCase());
                if (fileName == null) {
                    continue;
                }
                last.add(path + fileName.toLowerCase() + ".wav");
            }
            phrase = phrase.substring(0, b);
        }

        // ну теперь расщепим цифры, найдеи под них ресурсы и сложим в список для воспроизведения
        String lastAdded = "";
        for (int i = 0; i < phrase.length(); i++) {

            final String elem = phrase.substring(i).toLowerCase();
            String file = path + elem + ".wav";
            final InputStream streamUp = file.getClass().getResourceAsStream(file);
            if (streamUp != null) {
                try {
                    streamUp.close();
                } catch (IOException e) {
                    // press it.
                }
                // тут иногда перед произнесением очередных цыфр надо добавить типа препозицию, типа "тридцать и пять"
                if (lastAdded.length() == 2 && elem.length() == 1) {
                    res.add(path + "and.wav");
                }
                res.add(path + elem + ".wav");
                break;
            }

            String elemZer = (elem.substring(0, 1) + "00000000000000000000000000").substring(0, elem.length());
            String elemEnd = elem.length() > 1 ? elem.substring(1) : "";
            boolean needAdd = true;
            // а вот если есть записи с интонациеей продолжения специально для больших порядков, типа 100_ ...
            if (elemEnd.matches("[0-9]*[1-9]+[0-9]*")) {
                file = path + elemZer + "_.wav";
                final InputStream stream = file.getClass().getResourceAsStream(file);
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        // press it.
                    }
                    lastAdded = elemZer;
                    res.add(file);
                    needAdd = false;
                }
            }
            if (needAdd && !elemZer.startsWith("0")) {
                // тут иногда перед произнесением очередных цыфр надо добавить типа препозицию, типа "тридцать и пять"
                if (lastAdded.length() == 2 && elem.length() == 1) {
                    res.add(path + "and.wav");
                }

                lastAdded = elemZer;
                res.add(path + elemZer + ".wav");
            }
        }
        res.addAll(last);
        return res;
    }

    /**
     * Тестилка.
     */
    public static void main(String[] args) {
        Uses.loadPlugins("./plugins/");
        String num;
        num = args.length > 0 ? args[0] : "A123";
        String point = args.length > 1 ? args[1] : "117";

        QService service = new QService();
        service.setSoundTemplate("111111");
        SoundPlayer.inviteClient(service, new QCustomer(), num, point, true);
    }

    private static final HashMap<String, String> LATTERS = new HashMap<>();
    private static String preffix = "";

    private static String reRus(String path, String elem) {
        if (LATTERS.isEmpty()) {
            try {
                final InputStream ris = elem.getClass().getResourceAsStream(path + "latters.properties");
                if (ris != null) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(ris, Charset.forName("utf8")))) {
                        String line;
                        boolean f = true;
                        while ((line = br.readLine()) != null) {
                            line = (f ? line.substring(1) : line);
                            f = false;
                            String[] ss = line.split("=");
                            if (ss[0].startsWith("pref")) {
                                preffix = ss[1];
                            } else {
                                LATTERS.put(ss[0], ss[1]);
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                log().error("Не найден зкуковой ресурс или что-то в этом роде. " + ex);
                return null;
            }
        }
        return LATTERS.get(elem) == null ? elem : preffix + LATTERS.get(elem);
    }

    /**
     * Проговорить вызов клиента голосом.
     *
     * @param service      услуга.
     * @param customer     кастомер.
     * @param clientNumber номер вызываемого клиента
     * @param pointNumber  номер кабинета, куда вызвали
     * @param isFirst      первый ли раз.
     */
    public static void inviteClient(QService service, QCustomer customer, String clientNumber, String pointNumber, boolean isFirst) {
        // Для начала найдем шаблон
        QService tempServ = service;
        while ((tempServ.getSoundTemplate() == null || tempServ.getSoundTemplate().startsWith("0")) && tempServ.getParent() != null) {
            tempServ = tempServ.getParent();
        }
        if (tempServ.getSoundTemplate() == null || tempServ.getSoundTemplate().startsWith("0") || tempServ.getSoundTemplate().split("#").length == 0) {
            return;
        }
        SoundPlayer.play(prepareTemplate(tempServ.getSoundTemplate(), customer.getLanguage(), isFirst, clientNumber, pointNumber));
    }

    private static LinkedList<String> prepareTemplate(String soundTemplate, String language, boolean isFirst, String clientNumber, String pointNumber) {
        // путь к звуковым файлам
        final String path = findSoundResourcesFolder(soundTemplate, language);
        final LinkedList<String> res = new LinkedList<>();
        final int gong = addGong(res, soundTemplate, path, isFirst);
        if (!(isFirst && gong == 3)) {
            addClient(res, soundTemplate, path);
            addClientNumber(res, soundTemplate, path, clientNumber);
            addDestination(res, soundTemplate, path);
            addPointNumber(res, soundTemplate, path, pointNumber);
        }
        return res;
    }

    private static String findSoundResourcesFolder(String soundTemplate, String language) {
        // путь к звуковым файлам
        String path = SAMPLES_PACKAGE;

        final String[] parts = soundTemplate.split("#");
        // У услуги есть признак плагина специального для нее
        if (parts.length > 1 && !parts[1].isEmpty() && QPlugins.get().getPluginByName(QPlugins.Type.QSOUND, parts[1]) != null) {
            final QPlugins.QPlugin plugin = QPlugins.get().getPluginByName(QPlugins.Type.QSOUND, parts[1]);
            path = plugin.getPkg() + (plugin.getPkg().endsWith("/") ? "" : "/");
        } else {
            // нет признака или плагина. Ищем по признаку использования плагинов персонально по языку, выбранному пользователем.
            if (parts.length > 2 && "1".equals(parts[2]) && QPlugins.get().getPluginByLng(QPlugins.Type.QSOUND, language) != null) {
                final QPlugins.QPlugin plugin = QPlugins.get().getPluginByLng(QPlugins.Type.QSOUND, language);
                path = plugin.getPkg() + (plugin.getPkg().endsWith("/") ? "" : "/");
            }
        }
        return path;
    }

    private static int addGong(LinkedList<String> res, String soundTemplate, String path, boolean isFirst) {
        final String[] parts = soundTemplate.split("#");
        int gong = 1;
        if (parts[0].length() > 1) {
            switch (parts[0].substring(1, 2)) {
                case "1":
                    gong = 1;
                    break;
                case "2":
                    gong = 2;
                    break;
                case "3":
                    gong = 3;
                    break;
                default:
                    gong = 1;
            }
        }
        if ((isFirst && gong == 3) || gong == 2) {
            final String dingFilePath = ServerProps.getInstance().getProperty(QProperties.SECTION_SERVER, "ding.wav", "her");
            res.add(Files.exists(Paths.get(dingFilePath)) ? dingFilePath : (path + "ding.wav")); // NOSONAR
        }
        return gong;
    }

    private static void addClient(LinkedList<String> res, String soundTemplate, String path) {
        final String[] parts = soundTemplate.split("#");
        if (parts[0].length() > 2 && "1".equals(parts[0].substring(2, 3))) {
            res.add(path + "client.wav");
        }
    }

    private static void addClientNumber(LinkedList<String> res, String soundTemplate, String path, String clientNumber) {
        final String[] parts = soundTemplate.split("#");
        if (parts[0].length() > 3 && "1".equals(parts[0].substring(3, 4))) {
            res.addAll(toSoundSimple2(path, clientNumber));
        }
    }

    private static void addDestination(LinkedList<String> res, String soundTemplate, String path) {
        final String[] parts = soundTemplate.split("#");
        int goTo = 5;
        if (parts[0].length() > 4) {
            switch (parts[0].substring(4, 5)) {
                case "1":
                    goTo = 1;
                    break;
                case "2":
                    goTo = 2;
                    break;
                case "3":
                    goTo = 3;
                    break;
                case "4":
                    goTo = 4;
                    break;
                case "5":
                    goTo = 5;
                    break;
                default:
                    goTo = 5;
            }
        }

        switch (goTo) {
            case 1:
                res.add(path + "tocabinet.wav");
                break;
            case 2:
                res.add(path + "towindow.wav");
                break;
            case 3:
                res.add(path + "tostoika.wav");
                break;
            case 4:
                res.add(path + "totable.wav");
                break;
            case 5:
                // Это мы пропускаем произношение пункта приема.
                break;
            default:
                throw new IllegalArgumentException("Bad number = " + goTo);
        }
    }

    private static void addPointNumber(LinkedList<String> res, String soundTemplate, String path, String pointNumber) {
        final String[] parts = soundTemplate.split("#");
        if (parts[0].length() > 5 && parts[0].endsWith("1")) {
            res.addAll(toSoundSimple2(path, pointNumber));
        }
    }
}
