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
package ru.apertum.qsystem.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedBuilderParametersImpl;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.ex.ConfigurationException;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.exceptions.ServerException;
import ru.apertum.qsystem.extra.Agent;

/**
 * Тут весь миханизм для локализации. Текузая локаль,ю признаки муцльтиязычности, анаты и прочее.
 *
 * @author Evgeniy Egorov
 */
public final class Locales {

    private static final String WELCOME = "welcome";
    private static final String LOCALE_CURRENT = "locale.current";
    private static final String WELCOME_LNG = "welcome.multylangs";
    private static final String WELCOME_LNG_POS = "welcome.multylangs.position";
    private static final String WELCOME_LNG_BTN_FILL = "welcome.multylangs.areafilled";
    private static final String WELCOME_LNG_BTN_BORDER = "welcome.multylangs.border";

    private static final String[] RUSSIAN_MONAT = {
        "Января",
        "Февраля",
        "Марта",
        "Апреля",
        "Мая",
        "Июня",
        "Июля",
        "Августа",
        "Сентября",
        "Октября",
        "Ноября",
        "Декабря"
    };

    public static String[] getRussianMonat() {
        return RUSSIAN_MONAT;
    }

    private static final String[] UKRAINIAN_MONAT = {
        "Січня",
        "Лютого",
        "Березня",
        "Квітня",
        "Травня",
        "Червня",
        "Липня",
        "Серпня",
        "Вересня",
        "Жовтня",
        "Листопада",
        "Грудня"
    };

    public static String[] getUkrainianMonat() {
        return UKRAINIAN_MONAT;
    }

    private static final String[] AZERBAIJAN_MONAT = {"Yanvar",
        "Fevral",
        "Mart",
        "Aprel",
        "May",
        "Iyun",
        "Iyul",
        "Avqust",
        "Sentyabr",
        "Oktyabr",
        "Noyabr",
        "Dekabr"};

    public static String[] getAzerbaijanMonat() {
        return AZERBAIJAN_MONAT;
    }

    private static final String[] ARMENIAN_MONAT = {"Հունվարի",
        "Փետրվարի",
        "Մարտի",
        "Ապրիլի",
        "Մայիսի",
        "Հունիսի",
        "Հուլիսի",
        "Օգոստոսի",
        "Սեպտեմբերի",
        "Հոկտեմբերի",
        "Նոեմբերի",
        "Դեկտեմբերի"};

    public static String[] getArmenianMonat() {
        return ARMENIAN_MONAT;
    }

    private static final ResourceBundle TRANSLATE = ResourceBundle.getBundle("ru/apertum/qsystem/common/resources/i3-label", Locales.getInstance().getLangCurrent());

    public static String locMes(String key) {
        return TRANSLATE.getString(key);
    }

    /**
     * Формат даты без времени, с годом и месяц прописью.
     */
    public static final String DATE_FORMAT_FULL = "dd MMMM yyyy";
    public static final String DATE_FORMAT_FULL_SHORT = "dd.MM.yyyy";
    /**
     * Форматы дат 2009 январь 26 16:10:41.
     */
    public final SimpleDateFormat formatForLabel;
    public final SimpleDateFormat formatForLabel2;
    public final SimpleDateFormat formatForLabel2Short;
    public final SimpleDateFormat formatForPrint;
    public final SimpleDateFormat formatForPrintShort;
    public final SimpleDateFormat formatDdMmmm;
    public final SimpleDateFormat formatDdMmYyyyTime;
    public final SimpleDateFormat formatDdMmmmYyyy;
    public final SimpleDateFormat formatDdMmYyyy;

    private Locales() {

        // Загрузка плагинов из папки plugins
        QLog.l().logger().info("Languages are loading...");
        final File[] list = loadLocalesFromFiles();

        final HashSet<String> locs = new HashSet<>();
        for (File list1 : list) {
            final String s = list1.getName().split("\\.")[0];
            locs.add(s);
            final Properties settings = new Properties();
            try (final InputStream in = settings.getClass().getResourceAsStream("/" + s + ".properties");
                 final InputStreamReader inR = new InputStreamReader(in, "UTF-8")) {
                settings.load(inR);
            } catch (Exception ex) {
                QLog.l().logger().error("Language description " + list1.getName() + " did NOT load. " + ex);
                continue;
            }
            QLog.l().logger().debug("   Langusge: " + settings.getProperty("name") + " " + settings.getProperty("lng") + "_" + settings.getProperty("country"));

            final Locale locale = new Locale(settings.getProperty("lng"), settings.getProperty("country"));
            localesMap.put(s, locale);
            localesName.put(locale, s);
            lngs.put(settings.getProperty("name"), s);
            lngsNames.put(s, settings.getProperty("name"));
            lngsButtonText.put(s, settings.getProperty("buttontext"));
        }

        File f = new File(configFileName);
        if (!f.exists()) {
            configFileName = "../" + configFileName;
            f = new File(configFileName);
            if (!f.exists()) {
                final Exception ex = new FileNotFoundException(configFileName);
                QLog.l().logger().error(ex);
                throw new ServerException(ex);
            }
        }

        final FileBasedConfigurationBuilder<FileBasedConfiguration> builder
            = new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
            .configure(new FileBasedBuilderParametersImpl().setFileName(configFileName).setEncoding("utf8"));
        builder.setAutoSave(true);
        try {
            config = builder.getConfiguration();
            // config contains all properties read from the file
        } catch (ConfigurationException cex) {
            QLog.l().logger().error(cex);
            throw new ServerException(cex);
        }

        locs.stream().forEach(loc -> lngsWelcome.put(loc, config.getString(loc, "1")));

        russSymbolDateFormat = new DateFormatSymbols(getLocaleByName("RU"));
        russSymbolDateFormat.setMonths(RUSSIAN_MONAT);

        ukrSymbolDateFormat = new DateFormatSymbols(getLocaleByName("UA"));
        ukrSymbolDateFormat.setMonths(UKRAINIAN_MONAT);

        final DateFormatSymbols symbols = new DateFormatSymbols(getLangCurrent());
        switch (getLangCurrent().toString().toLowerCase(Locale.US)) {
            case "ru_ru":
                symbols.setMonths(RUSSIAN_MONAT);
                break;
            case "uk_ua":
                symbols.setMonths(UKRAINIAN_MONAT);
                break;
            case "az_az":
                symbols.setMonths(AZERBAIJAN_MONAT);
                break;
            case "hy_am":
                symbols.setMonths(ARMENIAN_MONAT);
                break;
            default:
                QLog.l().logger().trace("No cpec monats.");
        }

        formatForLabel = new SimpleDateFormat("dd MMMM HH.mm.ss", symbols);
        formatForLabel2 = new SimpleDateFormat("dd MMMM HH.mm:ss", symbols);
        formatForLabel2Short = new SimpleDateFormat("dd.MM HH.mm:ss", symbols);
        formatForPrint = new SimpleDateFormat("dd MMMM HH:mm", symbols);
        formatForPrintShort = new SimpleDateFormat("dd.MM HH:mm", symbols);
        formatDdMmmm = new SimpleDateFormat("dd MMMM", symbols);
        formatDdMmYyyyTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", symbols);
        formatDdMmmmYyyy = new SimpleDateFormat(DATE_FORMAT_FULL, symbols);
        formatDdMmYyyy = new SimpleDateFormat(DATE_FORMAT_FULL_SHORT, symbols);
    }

    private File[] loadLocalesFromFiles() {
        final File[] list = new File("languages").listFiles((File dir, String name) -> name.matches(".._..\\.(jar|JAR)"));
        if (list != null && list.length != 0) {
            for (File file : list) {
                Agent.addClassPath(file);
            }
        } else {
            throw new ServerException("Lacales were not loaded.");
        }
        return list;
    }

    private String configFileName = "config/langs.properties";
    private final FileBasedConfiguration config;

    public boolean isRuss() {
        return getNameOfPresentLocale().toLowerCase(Locale.ENGLISH).startsWith("ru");
    }

    public boolean isUkr() {
        return getNameOfPresentLocale().toLowerCase(Locale.ENGLISH).startsWith("ukr");
    }

    public boolean isEng() {
        return getNameOfPresentLocale().toLowerCase(Locale.ENGLISH).startsWith("en");
    }

    private final DateFormatSymbols russSymbolDateFormat;
    private final DateFormatSymbols ukrSymbolDateFormat;

    public DateFormatSymbols getRussSymbolDateFormat() {
        return russSymbolDateFormat;
    }

    public DateFormatSymbols getUkrSymbolDateFormat() {
        return ukrSymbolDateFormat;
    }

    /**
     * eng -> Locale(eng).
     */
    private final LinkedHashMap<String, Locale> localesMap = new LinkedHashMap<>();
    /**
     * Locale(eng)-> eng.
     */
    private final LinkedHashMap<Locale, String> localesName = new LinkedHashMap<>();
    /**
     * English -> eng.
     */
    private final LinkedHashMap<String, String> lngs = new LinkedHashMap<>();
    /**
     * eng -> English.
     */
    private final LinkedHashMap<String, String> lngsNames = new LinkedHashMap<>();
    /**
     * eng -> buttontext.
     */
    private final LinkedHashMap<String, String> lngsButtonText = new LinkedHashMap<>();
    /**
     * eng -> 1/0.
     */
    private final LinkedHashMap<String, String> lngsWelcome = new LinkedHashMap<>();

    public static Locales getInstance() {
        return LocalesHolder.INSTANCE;
    }

    private static class LocalesHolder {

        private static final Locales INSTANCE = new Locales();
    }

    public boolean isWelcomeMultylangs() {
        return config.getString(WELCOME_LNG) != null && ("1".equals(config.getString(WELCOME_LNG)) || config.getString(WELCOME_LNG).startsWith("$"));
    }

    /**
     * Установить для велкома мультиязычный интерфейс.
     *
     * @param multylangs да или нет.
     */
    public void setWelcomeMultylangs(boolean multylangs) {
        if (!config.getString(WELCOME_LNG).startsWith("$")) {
            config.setProperty(WELCOME_LNG, multylangs ? "1" : "0");
        }
    }

    public boolean isIde() {
        return config.getString(WELCOME_LNG).startsWith("$");
    }

    public boolean isWelcomeFirstLaunch() {
        return config.getString(WELCOME) != null && "1".equals(config.getString(WELCOME)) && !config.getString(WELCOME_LNG).startsWith("$");
    }

    public boolean isWelcomeMultylangsButtonsFilled() {
        return config.getString(WELCOME_LNG_BTN_FILL) == null || "1".equals(config.getString(WELCOME_LNG_BTN_FILL));
    }

    public boolean isWelcomeMultylangsButtonsBorder() {
        return config.getString(WELCOME_LNG_BTN_BORDER) == null || "1".equals(config.getString(WELCOME_LNG_BTN_BORDER));
    }

    public int getMultylangsPosition() {
        return config.getString(WELCOME_LNG_POS) == null ? 1 : Integer.parseInt(config.getString(WELCOME_LNG_POS));
    }

    public Locale getLangCurrent() {
        return localesMap.get(config.getString(LOCALE_CURRENT)) == null ? Locale.getDefault() : localesMap.get(config.getString(LOCALE_CURRENT));
    }

    public Locale getLocaleByName(String name) {
        return localesMap.get(name) == null ? Locale.getDefault() : localesMap.get(name);
    }

    public String getLangCurrName() {
        return "".equals(config.getString(LOCALE_CURRENT)) ? lngsNames.get("eng") : lngsNames.get(config.getString(LOCALE_CURRENT));
    }

    public String getLangButtonText(String lng) {
        return lngsButtonText.get(lng);
    }

    public String getLangWelcome(String lng) {
        return lngsWelcome.get(lng);
    }

    public String getNameOfPresentLocale() {
        return localesName.get(Locale.getDefault());
    }

    /**
     * Установим используемую локаль.
     *
     * @param name English к примеру eng
     */
    public void setLangCurrent(String name) {
        config.setProperty(LOCALE_CURRENT, lngs.get(name));
    }

    public void setWelcome(String count) {
        config.setProperty(WELCOME, count);
    }

    public void setLangWelcome(String name, boolean on) {
        config.setProperty(name, on ? "1" : "0");
        lngsWelcome.put(name, on ? "1" : "0");
    }

    public List<String> getAvailableLocales() {
        return new ArrayList<>(lngs.keySet());
    }

    public List<String> getAvailableLangs() {
        return new ArrayList<>(lngsNames.keySet());
    }
}
