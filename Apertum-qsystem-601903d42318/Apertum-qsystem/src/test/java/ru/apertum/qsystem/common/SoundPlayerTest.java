package ru.apertum.qsystem.common;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.LinkedList;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static ru.apertum.qsystem.common.SoundPlayer.SAMPLES_PACKAGE;

public class SoundPlayerTest {

    SoundPlayer player;

    @BeforeMethod
    public void setUp() {
    }

    @Test
    public void testToSoundSimple2() {
        LinkedList<String> strings = SoundPlayer.toSoundSimple2("/", "a129b");
        assertEquals(strings.size(), 6);
        strings = SoundPlayer.toSoundSimple2("/", "as127bd");
        assertEquals(strings.size(), 8);
        strings = SoundPlayer.toSoundSimple2("/", "asd");
        assertEquals(strings.size(), 3);
        strings = SoundPlayer.toSoundSimple2("/", "100");
        assertEquals(strings.size(), 1);
        strings = SoundPlayer.toSoundSimple2("/", "108");
        assertEquals(strings.size(), 2);
        strings = SoundPlayer.toSoundSimple2("/", "");
        assertEquals(strings.size(), 0);
        strings = SoundPlayer.toSoundSimple2("/", "10005");
        assertEquals(strings.size(), 2);
        strings = SoundPlayer.toSoundSimple2("/", "00005");
        assertEquals(strings.size(), 1);
        strings = SoundPlayer.toSoundSimple2("/", "00000");
        assertEquals(strings.size(), 0);
        strings = SoundPlayer.toSoundSimple2("/", "000100");
        assertEquals(strings.size(), 1);
        assertEquals(strings.get(0), "/100.wav");
        strings = SoundPlayer.toSoundSimple2("/", "72as");
        assertEquals(strings.size(), 5);
        strings = SoundPlayer.toSoundSimple2("/", "sa69");
        assertEquals(strings.size(), 5);
    }

    //String findSoundResourcesFolder(String soundTemplate, String language)
    @Test
    public void testFindSoundResourcesFolder() throws Exception {
        Method[] methods = SoundPlayer.class.getDeclaredMethods();
        for (Method method : methods) {
            if ("findSoundResourcesFolder".equals(method.getName())) {
                method.setAccessible(true);
                String folder = (String) method.invoke(SoundPlayer.class, "13101", "RU_ru");
                assertEquals(folder, "/ru/apertum/qsystem/server/sound/");

                folder = (String) method.invoke(SoundPlayer.class, "", "RU_ru");
                assertEquals(folder, "/ru/apertum/qsystem/server/sound/");


                return;
            }
        }
        assertTrue(false);
    }

    //int addGong(LinkedList<String> res, String soundTemplate, String path, boolean isFirst)
    // второй байт это за гонг
    @Test
    public void testAddGong() throws Exception {
        Method[] methods = SoundPlayer.class.getDeclaredMethods();
        for (Method method : methods) {
            if ("addGong".equals(method.getName())) {
                method.setAccessible(true);
                LinkedList<Object> list = new LinkedList<>();
                int gong = (int) method.invoke(SoundPlayer.class, list, "13101", "/path/", true);
                assertEquals(gong, 3);
                assertEquals(list.size(), 1);
                assertEquals(list.get(0), "/path/ding.wav");

                list.clear();
                gong = (int) method.invoke(SoundPlayer.class, list, "13101", "/path/", false);
                assertEquals(gong, 3);
                assertEquals(list.size(), 0);

                list.clear();
                gong = (int) method.invoke(SoundPlayer.class, list, "12101", "/path/", false);
                assertEquals(gong, 2);
                assertEquals(list.size(), 1);
                assertEquals(list.get(0), "/path/ding.wav");

                list.clear();
                gong = (int) method.invoke(SoundPlayer.class, list, "", "/path/", false);
                assertEquals(gong, 1);
                assertEquals(list.size(), 0);

                return;
            }
        }
        assertTrue(false);
    }

    //void addClient(LinkedList<String> res, String soundTemplate, String path)
    // третий байт за "клиент..."
    @Test
    public void testAddClient() throws Exception {
        Method[] methods = SoundPlayer.class.getDeclaredMethods();
        for (Method method : methods) {
            if ("addClient".equals(method.getName())) {
                method.setAccessible(true);
                LinkedList<Object> list = new LinkedList<>();
                method.invoke(SoundPlayer.class, list, "13101", "/path/");
                assertEquals(list.size(), 1);
                assertEquals(list.get(0), "/path/client.wav");

                list.clear();
                method.invoke(SoundPlayer.class, list, "13001", "/path/");
                assertEquals(list.size(), 0);

                list.clear();
                method.invoke(SoundPlayer.class, list, "", "/path/");
                assertEquals(list.size(), 0);

                return;
            }
        }
        assertTrue(false);
    }

    // void addClientNumber(LinkedList<String> res, String soundTemplate, String path, String clientNumber)
    // четвертый байт за номер клиента
    @Test
    public void testAddClientNumber() throws Exception {
        Method[] methods = SoundPlayer.class.getDeclaredMethods();
        for (Method method : methods) {
            if ("addClientNumber".equals(method.getName())) {
                method.setAccessible(true);
                LinkedList<Object> list = new LinkedList<>();
                method.invoke(SoundPlayer.class, list, "13111", "/path/", "1");
                assertEquals(list.size(), 1);
                assertEquals(list.get(0), "/path/1.wav");

                list.clear();
                method.invoke(SoundPlayer.class, list, "13001", "/path/", "1");
                assertEquals(list.size(), 0);

                list.clear();
                method.invoke(SoundPlayer.class, list, "", "/path/", "1");
                assertEquals(list.size(), 0);

                return;
            }
        }
        assertTrue(false);
    }

    // void addDestination(LinkedList<String> res, String soundTemplate, String path)
    // пятый байт за "подойтите к..."
    @Test
    public void testAddDestination() throws Exception {
        Method[] methods = SoundPlayer.class.getDeclaredMethods();
        for (Method method : methods) {
            if ("addDestination".equals(method.getName())) {
                method.setAccessible(true);
                LinkedList<Object> list = new LinkedList<>();
                method.invoke(SoundPlayer.class, list, "13101", "/path/");
                assertEquals(list.size(), 1);
                assertEquals(list.get(0), "/path/tocabinet.wav");

                list.clear();
                method.invoke(SoundPlayer.class, list, "13100", "/path/");
                assertEquals(list.size(), 0);

                list.clear();
                method.invoke(SoundPlayer.class, list, "12104", "/path/");
                assertEquals(list.size(), 1);
                assertEquals(list.get(0), "/path/totable.wav");

                list.clear();
                method.invoke(SoundPlayer.class, list, "", "/path/");
                assertEquals(list.size(), 0);

                return;
            }
        }
        assertTrue(false);
    }

    //void addPointNumber(LinkedList<String> res, String soundTemplate, String path, String pointNumber)
    // шестой байт за произносение номера кабинета
    @Test
    public void testAddPointNumber() throws Exception {
        Method[] methods = SoundPlayer.class.getDeclaredMethods();
        for (Method method : methods) {
            if ("addPointNumber".equals(method.getName())) {
                method.setAccessible(true);
                LinkedList<Object> list = new LinkedList<>();
                method.invoke(SoundPlayer.class, list, "130001", "/path/", "2");
                assertEquals(list.size(), 1);
                assertEquals(list.get(0), "/path/2.wav");

                list.clear();
                method.invoke(SoundPlayer.class, list, "130000", "/path/", "2");
                assertEquals(list.size(), 0);

                list.clear();
                method.invoke(SoundPlayer.class, list, "", "/path/", "2");
                assertEquals(list.size(), 0);

                return;
            }
        }
        assertTrue(false);
    }

    //LinkedList<String> prepareTemplate(String soundTemplate, String language, boolean isFirst, String clientNumber, String pointNumber)
    @Test
    public void testPrepareTemplate() throws Exception {
        Method[] methods = SoundPlayer.class.getDeclaredMethods();
        for (Method method : methods) {
            if ("prepareTemplate".equals(method.getName())) {
                method.setAccessible(true);
                LinkedList<String> strings = (LinkedList<String>) method.invoke(SoundPlayer.class, "131111", "RU_ru", true, "1", "2");
                assertEquals(strings.size(), 1);
                assertEquals(strings.get(0), SAMPLES_PACKAGE + "ding.wav");

                strings = (LinkedList<String>) method.invoke(SoundPlayer.class, "030001", "RU_ru", false, "1", "2");
                assertEquals(strings.size(), 1);
                assertEquals(strings.get(0), SAMPLES_PACKAGE + "2.wav");

                strings = (LinkedList<String>) method.invoke(SoundPlayer.class, "010001", "RU_ru", true, "1", "2");
                assertEquals(strings.size(), 1);
                assertEquals(strings.get(0), SAMPLES_PACKAGE + "2.wav");

                strings = (LinkedList<String>) method.invoke(SoundPlayer.class, "020001", "RU_ru", true, "1", "2");
                assertEquals(strings.size(), 2);
                assertEquals(strings.get(0), SAMPLES_PACKAGE + "ding.wav");
                assertEquals(strings.get(1), SAMPLES_PACKAGE + "2.wav");

                strings = (LinkedList<String>) method.invoke(SoundPlayer.class, "021111", "RU_ru", true, "1", "2");
                assertEquals(strings.size(), 5);
                assertEquals(strings.get(0), SAMPLES_PACKAGE + "ding.wav");
                assertEquals(strings.get(1), SAMPLES_PACKAGE + "client.wav");
                assertEquals(strings.get(2), SAMPLES_PACKAGE + "1.wav");
                assertEquals(strings.get(3), SAMPLES_PACKAGE + "tocabinet.wav");
                assertEquals(strings.get(4), SAMPLES_PACKAGE + "2.wav");


                return;
            }
        }
        assertTrue(false);
    }
}