/*
 * Copyright (C) 2014 Evgeniy Egorov
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
package ru.apertum.qsystem.reports.common;

import ru.apertum.qsystem.client.Locales;

import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.apertum.qsystem.common.QLog.lgRep;

/**
 * Ресурсы локализации для отчетов.
 *
 * @author Evgeniy Egorov
 */
public class RepResBundle {

    private final ResourceBundle bundle;

    private RepResBundle() {
        bundle = ResourceBundle.getBundle("ru/apertum/qsystem/reports/templates/i3-label-rep", Locales.getInstance().getLangCurrent());
    }

    public static RepResBundle getInstance() {
        return RepResBundleHolder.INSTANCE;
    }

    private static class RepResBundleHolder {

        private static final RepResBundle INSTANCE = new RepResBundle();
    }

    public String getString(String key) {
        return bundle.getString(key);
    }

    public boolean present(String key) {
        return bundle.containsKey(key);
    }

    /**
     * Достать строку из бандла.
     *
     * @param key по ключу.
     * @return локализованная строка или NULL.
     */
    public String getStringNulled(String key) {
        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        } else {
            lgRep().warn("No lang bundle \"" + key + "\".");
            return null;
        }
    }

    /**
     * Достать строку из бандла.
     *
     * @param key по ключу.
     * @return локализованная строка или ключ.
     */
    public String getStringSafe(String key) {
        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        } else {
            lgRep().warn("No lang bundle. \"" + key + "\".");
            return key;
        }
    }

    /**
     * Подменить все якоря на локализованные строки.
     *
     * @param source Текст с якорями.
     * @return Готовый локализованный текст.
     */
    public String prepareString(String source) {
        final ArrayList<String> forRemove = new ArrayList<>();
        final Matcher m = Pattern.compile("\\$R\\{.+?\\}").matcher(source);
        while (m.find()) {
            forRemove.add(m.group().substring(3, m.group().length() - 1));
        }

        for (String res : forRemove) {
            source = source.replaceFirst("\\$R\\{" + res + "\\}", getStringSafe(res));
        }

        return source;
    }
}
