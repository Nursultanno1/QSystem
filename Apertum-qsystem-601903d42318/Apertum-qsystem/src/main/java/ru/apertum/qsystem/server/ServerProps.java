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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.hibernate.TransactionException;
import ru.apertum.qsystem.common.QConfig;
import ru.apertum.qsystem.common.QModule;
import ru.apertum.qsystem.common.exceptions.ServerException;
import ru.apertum.qsystem.hibernate.Dao;
import ru.apertum.qsystem.server.controller.ServerEvents;
import ru.apertum.qsystem.server.model.QNet;
import ru.apertum.qsystem.server.model.QProperty;
import ru.apertum.qsystem.server.model.QStandards;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import static ru.apertum.qsystem.common.QLog.log;
import static ru.apertum.qsystem.common.QLog.logProp;

/**
 * Это класс для получения серверных настроек. Использовать только внутри сервера! Для остальных компонент системы использовать QProperties
 * QProperties.get().getProperty(SECTION, PROP);
 *
 * @author Evgeniy Egorov
 */
@SuppressWarnings("squid:S1319")
public class ServerProps {

    private final QNet netProp = new QNet();
    private final QStandards standards = new QStandards();
    private final LinkedHashMap<String, Section> properties = new LinkedHashMap<>();

    public QNet getProps() {
        return netProp;
    }

    public QStandards getStandards() {
        return standards;
    }

    public LinkedHashMap<String, Section> getDBproperties() {
        return properties;
    }

    /**
     * Получить параметры из БД.
     *
     * @param needReload Если true, то параметры будут предапрительно загружены из БД, false - просто получены те, которые уже когда-то загружались.
     * @return
     */
    public LinkedHashMap<String, Section> getDBproperties(boolean needReload) {
        if (needReload) {
            reloadProperties();
        }
        return getDBproperties();
    }

    private void applyLocalProperties(String filePath) {
        if (Files.notExists(FileSystems.getDefault().getPath(filePath), LinkOption.NOFOLLOW_LINKS)) { //NOSONAR
            return;
        }
        final Parameters params = new Parameters();
        final FileBasedConfigurationBuilder<XMLConfiguration> builder = new FileBasedConfigurationBuilder<>(XMLConfiguration.class)
                .configure(params.xml()
                        .setFileName(filePath).setEncoding("UTF-8")
                        .setListDelimiterHandler(new DefaultListDelimiterHandler(',')));
        try {
            final XMLConfiguration config = builder.getConfiguration();
            // access configuration properties
            final List<String> sections = config.getList(String.class, "property[@section]");
            final List<String> keys = config.getList(String.class, "property[@key]");
            final List<String> values = config.getList(String.class, "property[@value]");
            final List<String> comments = config.getList(String.class, "property[@comment]");

            for (int i = 0; i < sections.size(); i++) {
                final Section secmap = getSection(sections.get(i));
                if (secmap == null || !secmap.properties.containsKey(keys.get(i))) {
                    saveOrUpdateProperty(sections.get(i), keys.get(i), values.get(i), comments.get(i));
                }
            }
        } catch (ConfigurationException ex) {
            log().error("Error for apply local properties. \n" + ex.toString() + "\n" + Arrays.toString(ex.getStackTrace()));
        } catch (Exception ex) {
            log().error("Bad local properties. \n" + ex.toString() + "\n" + Arrays.toString(ex.getStackTrace()));
        }
    }

    private ServerProps() {
        // сначала загрузим
        if (QConfig.cfg().getModule().isServer() || QConfig.cfg().getModule().isAdminApp() || QConfig.cfg().getModule() == QModule.unknown) {
            load();
            ServerEvents.getInstance().registerListener(this::load);
        }
        // потом обновим с диска
        applyLocalProperties("config/properties.xml");
    }

    private void load() {
        Dao.get().load(netProp, (long) 1);
        Dao.get().load(standards, (long) 1);
        reloadProperties();
    }

    private void loadProperties() {
        final List<QProperty> list = Dao.get().loadAll(QProperty.class);
        list.stream().map(p -> {
            if (properties.get(p.getSection()) == null) {
                properties.put(p.getSection(), new Section(p.getSection(), new LinkedHashMap<>()));
            }
            return p;
        }).forEach(p ->
                properties.get(p.getSection()).addProperty(p)
        );
    }

    public static ServerProps getInstance() {
        return ServerPropsHolder.INSTANCE;
    }

    private static class ServerPropsHolder {

        private static final ServerProps INSTANCE = new ServerProps();
    }

    /**
     * Может быть NULL, если нет такой сейции.
     *
     * @param section имя секции или NULL.
     * @return список параметров.
     */
    public LinkedHashMap<String, QProperty> getSectionProps(String section) {
        return properties.get(section).properties;
    }

    /**
     * Получить существующую секцию.
     *
     * @param section имя существующей секции
     * @return существующая секция с именем или NULL
     */
    public Section getSection(String section) {
        return properties.get(section);
    }

    /**
     * Добавить пустую секцию если её небыло.
     *
     * @param section имя требуемой секции
     * @return вернет созданную секцию если её небыло или существующую с таким именем.
     */
    public Section addSection(String section) {
        if (getSection(section) == null) {
            properties.put(section, new Section(section, new LinkedHashMap<>()));
        }
        return getSection(section);
    }

    /**
     * Удалим всю секцию.
     *
     * @param section по имени.
     */
    public void deleteSection(String section) {
        if (getSection(section) != null) {
            final Section secDel = getSection(section);
            secDel.getProperties().values().forEach(prop -> {
                if (prop.getId() != null) {
                    deleteProp(prop);
                }
            });
            secDel.getProperties().clear();
            properties.remove(section);
        }
    }

    /**
     * Получить все параметры списком.
     *
     * @return список всех параметров из БД
     */
    public ArrayList<QProperty> getAllProperties() {
        final ArrayList<QProperty> col = new ArrayList<>();
        properties.values().forEach(sec -> col.addAll(sec.properties.values()));
        return col;
    }

    /**
     * Получить все настройки, что лежат в БД.
     *
     * @return список секций с параметрами.
     */
    public Collection<Section> getSections() {
        return properties.values();
    }

    public static final class Section {

        @Expose
        @SerializedName("name")
        private final String name;
        @Expose
        @SerializedName("properties")
        private final LinkedHashMap<String, QProperty> properties;

        public Section(String name, LinkedHashMap<String, QProperty> properties) {
            this.name = name;
            this.properties = properties;
        }

        public String getName() {
            return name;
        }

        public LinkedHashMap<String, QProperty> getProperties() {
            return properties;
        }

        /**
         * Получить параметр из секции.
         *
         * @param key ключ параметра
         * @return требуемый параметр. Может быть NULL если по имени не нашлось.
         */
        public QProperty getProperty(String key) {
            return properties.get(key);
        }

        /**
         * Добавим пропертю.
         *
         * @param key     с ключом.
         * @param value   со значением.
         * @param comment с комментпрпием.
         * @return созданная пропертя.
         */
        public QProperty addProperty(String key, String value, String comment) {
            final QProperty res = new QProperty(name, key, value, comment);
            properties.put(key, res);
            return res;
        }

        /**
         * Добавим пропертю.
         *
         * @param prop попертя для удаления.
         */
        public void addProperty(QProperty prop) {
            if ((prop.getSection() == null && name == null) || name.equals(prop.getSection())) {
                if (properties.containsKey(prop.getKey())) {
                    throw new IllegalArgumentException("Key " + prop.getKey() + " already exists in this section \"" + name + "\"");
                }
                properties.put(prop.getKey(), prop);
            } else {
                throw new IllegalArgumentException("Property " + prop + " already has the section but not from \"" + name + "\"");
            }
        }

        /**
         * Удаляем пропертю.
         *
         * @param prop пропертя для удаления..
         */
        public void removeProperty(QProperty prop) {
            if ((prop.getSection() == null && name == null) || name.equals(prop.getSection())) {
                properties.remove(prop.getKey());
            } else {
                throw new IllegalArgumentException("Property " + prop + " is not from this section \"" + name + "\"");
            }
        }

        /**
         * Удаляем пропертю по ключу.
         *
         * @param key ключ.
         */
        public void removeProperty(String key) {
            if (properties.containsKey(key)) {
                properties.remove(key);
            }
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Получает параметр по имени из секции. Секция должна существовать и параметр должен присутствовать, иначе NULL.
     *
     * @param section название секции
     * @param key     название параметра
     * @return запрашиваемый параметер, может быть NULL
     */
    public QProperty getProperty(String section, String key) {
        triceParam(section + "->" + key);
        if (key == null) {
            return null;
        }
        final Section secmap = getSection(section);
        if (secmap == null) {
            return null;
        }
        if (!secmap.properties.containsKey(key)) {
            return null;
        }
        return secmap.properties.get(key);
    }

    /**
     * Получает параметр по имени из секции. Секция должна существовать и параметр должен присутствовать, иначе deafultValue.
     *
     * @param section      название секции
     * @param key          название параметра
     * @param deafultValue если не найдена секция или параметр
     * @return запрашиваемый параметер, deafultValue если не найдена секция или параметр
     */
    public String getProperty(String section, String key, String deafultValue) {
        triceParam(section + "->" + key + "[=" + deafultValue + "]");
        final QProperty p = getProperty(section, key);
        return p == null ? deafultValue : p.getValue();
    }

    /**
     * Получить параметр. Если такого не найдется, то создастся новый параметр.
     *
     * @param section        секция
     * @param key            ключ
     * @param deafultValue   создать параметр с этим значением если параметр еще не существует.
     * @param deafultComment этот коммент добавится к созданному параметру
     * @return найденный или созданный параметр.
     */
    public QProperty getProperty(String section, String key, String deafultValue, String deafultComment) {
        triceParam(section + "->" + key + "[=" + deafultValue + "] " + deafultComment);
        final QProperty p = getProperty(section, key);
        if (p == null) {
            return saveOrUpdateProperty(section, key, deafultValue, deafultComment);
        } else {
            return p;
        }
    }

    private final HashMap<String, Long> trace = new HashMap<>();

    private void triceParam(String str) {
        Long tm = trace.get(str);
        if (tm == null || 1000 * 60 * 60 < System.currentTimeMillis() - tm) {
            trace.put(str, System.currentTimeMillis());
            logProp().info(str);
        }
    }

    public void reloadProperties() {
        properties.clear();
        loadProperties();
    }

    /**
     * Добавляем параметр. Если нет секции или параметер не существует, то секция или параметер будут созданы. Если параметер существует, то он будет изменен и
     * сохранен в БД.
     *
     * @param section параметер в этой секции
     * @param key     с таким ключем
     * @param value   новове значение параметра
     * @param comment коментарий по параметру
     * @return созданный или сохраненный параметр
     */
    public QProperty saveOrUpdateProperty(String section, String key, String value, String comment) {
        if (key == null) {
            throw new IllegalArgumentException("Key must be not NULL");
        }
        final Section secmap = addSection(section);
        if (!secmap.properties.containsKey(key)) {
            secmap.properties.put(key, new QProperty(section, key));
        }
        final QProperty p = secmap.getProperty(key);
        p.setValue(value);
        p.setComment(comment);

        return saveProp(p);
    }

    /**
     * Сохранить спараметр.
     *
     * @param prop эти данные сохраним
     * @return созданный или сохраненный параметр
     */
    public QProperty saveOrUpdateProperty(QProperty prop) {
        if (prop.getKey() == null) {
            throw new IllegalArgumentException("Key is NULL but must be not NULL");
        }
        if (prop.getId() == null) {
            final Section secmap = addSection(prop.getSection());
            if (!secmap.properties.containsKey(prop.getKey())) {
                secmap.properties.put(prop.getKey(), prop);
            }
            final QProperty p = secmap.getProperty(prop.getKey());
            return saveProp(p);
        } else {
            final LinkedList<QProperty> res = new LinkedList<>();
            properties.values().forEach(sec -> sec.properties.values().forEach(p -> {
                        if (p.equals(prop) && p.getId().equals(prop.getId()) && p.hashCode() == prop.hashCode()) {
                            res.add(prop);
                        }
                    }
            ));
            if (res.size() == 1) {
                return saveProp(prop);
            } else {
                throw new IllegalArgumentException("Properties " + prop + " is weird. It has ID but among the properties was not found.");
            }
        }
    }

    /**
     * Удалить параметр из БД.
     *
     * @param prop этот параметр удаляем.
     * @return вернет входящий параметр, можно игнорировать.
     */
    private QProperty deleteProp(QProperty prop) {
        return persistProp(prop, false);
    }

    private QProperty saveProp(QProperty prop) {
        return persistProp(prop, true);
    }

    private QProperty persistProp(QProperty prop, boolean save) {
        if (!QConfig.cfg().getModule().isServer()) {
            return prop;
        }
        final Exception res;
        try {
            res = Dao.get().execute(() -> {
                try {
                    if (save) {
                        Dao.get().saveOrUpdate(prop);
                    } else {
                        Dao.get().delete(prop);
                    }
                } catch (Exception ex) {
                    log().error("Ошибка при " + (save ? "сохранении" : "удалении") + " \n" + ex.toString() + "\n" + Arrays.toString(ex.getStackTrace()));
                    return ex;
                }
                return null;
            });
        } catch (TransactionException ex) {
            throw new ServerException("Ошибка выполнения операции изменения данных в БД(JDBC). Возможно введенные вами параметры не могут быть сохранены.\n(" + ex.toString() + ")");
        }
        if (res != null) {
            throw new ServerException("Ошибка выполнения операции изменения данных в БД(JDBC). Возможно введенные вами параметры не могут быть сохранены.\n[" + res.getLocalizedMessage() + "]\n(" + res.toString() + ")\nSQL: ");
        }
        return prop;
    }

    /**
     * Удаляем параметр.
     *
     * @param section параметер в этой секции
     * @param key     с таким ключем
     */
    public void removeProperty(String section, String key) {
        if (key == null || getSection(section) == null) {
            return;
        }
        final Section secmap = getSection(section);
        if (!secmap.properties.containsKey(key)) {
            return;
        }
        if (secmap.getProperty(key).getId() != null) {
            deleteProp(secmap.getProperty(key));
        }
        secmap.removeProperty(key);
    }

    /**
     * Удаляем параметр.
     *
     * @param prop то что нужно удалить
     */
    public void removeProperty(QProperty prop) {
        if (prop.getKey() == null || getSection(prop.getSection()) == null) {
            if (prop.getId() != null) {
                deleteProp(prop);
            }
        } else {
            final Section secmap = getSection(prop.getSection());
            if (!secmap.properties.containsKey(prop.getKey())) {
                if (prop.getId() != null) {
                    deleteProp(prop);
                }
            } else {
                if (secmap.getProperty(prop.getKey()).getId() != null) {
                    deleteProp(secmap.getProperty(prop.getKey()));
                }
                secmap.removeProperty(prop);
            }
        }
    }
}
