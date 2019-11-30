/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.qsystem.common;

import java.util.LinkedList;

/**
 * Все загруженные плагины. Пока только звуковые.
 *
 * @author Evgeniy Egorov
 */
public class QPlugins {

    public enum Type {
        QLOCALIZE,
        QSERVER,
        QCLIENT,
        QWELCOME,
        QWELCOME_CHECK,
        QWELCOME_INPUT,
        QSOUND,
        QPLUGIN;
    }

    public static class QPlugin {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        private Type type;

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public boolean isQSound() {
            return type.equals(Type.QSOUND);
        }

        private String language;

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        private String pkg;

        public String getPkg() {
            return pkg;
        }

        public void setPkg(String pkg) {
            this.pkg = pkg;
        }

        /**
         * Плагин.
         */
        public QPlugin(String name, Type type, String language, String pkg) {
            this.name = name;
            this.type = type;
            this.language = language;
            this.pkg = pkg;
        }

        public QPlugin() {
        }

        @Override
        public String toString() {
            return "QPLugin " + name + ": " + type + "; [" + language + "] package " + pkg + ";";
        }
    }

    private QPlugins() {
    }

    public static QPlugins get() {
        return QPluginsHolder.INSTANCE;
    }

    private static class QPluginsHolder {

        private static final QPlugins INSTANCE = new QPlugins();
    }

    private final LinkedList<QPlugin> plugins = new LinkedList<>();

    public void addPlugin(QPlugin plugin) {
        plugins.add(plugin);
    }

    /**
     * Получить плагин.
     *
     * @param type тип.
     * @param lng  язык.
     * @return плагин.
     */
    public QPlugin getPluginByLng(Type type, String lng) {
        for (QPlugin plugin : plugins) {
            if (plugin.getType() == type && plugin.getLanguage().equalsIgnoreCase(lng)) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Получить плагин.
     *
     * @param name имя.
     * @param type тип.
     * @return плагин.
     */
    public QPlugin getPluginByName(Type type, String name) {
        for (QPlugin plugin : plugins) {
            if (plugin.getType() == type && plugin.getName().equalsIgnoreCase(name)) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Получить плагин.
     *
     * @param name имя.
     * @param type тип.
     * @param lng  язык.
     * @return плагин.
     */
    public QPlugin getPlugin(String name, Type type, String lng) {
        for (QPlugin plugin : plugins) {
            if (plugin.getName().equalsIgnoreCase(name) && plugin.getType() == type && plugin.getLanguage().equalsIgnoreCase(lng)) {
                return plugin;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return plugins.toString();
    }
}
