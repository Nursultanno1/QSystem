/*
 *  Copyright (C) 2011 egorov
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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.reflections.Reflections;
import ru.apertum.qsystem.common.exceptions.ServerException;

import javax.persistence.Entity;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static ru.apertum.qsystem.common.QLog.log;

/**
 * <h3>Вся связь с БД через Hibernate.</h3>
 * Тут создадим фабрику сессий, укажем хиберу как работать, проведем некоторые опирации с БД.
 *
 * @author egorov
 */
public class Dao {

    private final String driverClassName;
    private final String url;
    private final String username;
    private final String password;

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    private final SessionFactory sessionFactory;

    private Dao() {
        ChangeServerAction config = new ChangeServerAction();
        driverClassName = config.getDriver();
        url = config.getUrl();
        username = config.getUser();
        password = config.getParolcheg();
        final String dialect = driverClassName.contains("mysql") ? MySQLDialect.class.getName() : H2Dialect.class.getName();
        try {
            Configuration configuration = new Configuration();
            new Reflections("ru.apertum.qsystem.server.model").getTypesAnnotatedWith(Entity.class).forEach(configuration::addAnnotatedClass);
            new Reflections("ru.apertum.qsystem.common.model").getTypesAnnotatedWith(Entity.class).forEach(configuration::addAnnotatedClass);
            new Reflections("ru.apertum.qsystem.reports.model").getTypesAnnotatedWith(Entity.class).forEach(configuration::addAnnotatedClass);
            final StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties())
                    .applySetting("hibernate.connection.driver_class", driverClassName)
                    .applySetting("hibernate.dialect", dialect)
                    .applySetting("hibernate.connection.url", url)
                    .applySetting("hibernate.connection.username", username)
                    .applySetting("hibernate.connection.password", password)
                    .applySetting("hibernate.show_sql", false)
                    .applySetting("hibernate.format_sql", true)
                    .applySetting("hibernate.current_session_context_class", "thread")
                    .applySetting("hibernate.c3p0.min_size", 1)
                    .applySetting("hibernate.c3p0.max_size", 10)
                    .applySetting("hibernate.c3p0.timeout", 1000)
                    .applySetting("hibernate.c3p0.max_statements", 50)
                    .applySetting("hibernate.c3p0.idle_test_period", 3600)
                    .applySetting("hibernate.c3p0.validate", true)
                    .applySetting("hibernate.connection.provider_class", "org.hibernate.c3p0.internal.C3P0ConnectionProvider");
            sessionFactory = configuration.buildSessionFactory(builder.build());
        } catch (Exception e) {
            throw new ServerException("Hibernate configuration excapton.", e);
        }
    }

    public static Dao get() {
        return HibHolder.INSTANCE;
    }

    private static class HibHolder {

        private static final Dao INSTANCE = new Dao();
    }

    /**
     * Для транзакций через передачу на выполнениее класса с методом где реализована работа с БД.
     *
     * @param doSome функция, возвращающая без возврата исключение, в ней работа с БД.
     * @return произошедшее исключение.
     */
    public Exception execute(DoSomething doSome) {
        return execute(() -> {
            try {
                doSome.go();
            } catch (Exception ex) {
                return ex;
            }
            return null;
        });
    }

    /**
     * Для транзакций через передачу на выполнениее класса с методом где реализована работа с БД.
     *
     * @param doSome функция, возвращающая исключение, в ней работа с БД.
     * @return произошедшее исключение.
     */
    public Exception execute(Supplier<? extends Exception> doSome) {
        final Session session = getSession();
        final Transaction transaction = session.beginTransaction();
        try {
            Exception exception = doSome.get();
            if (exception != null) {
                log().error("BD has problem.", exception);
                transaction.markRollbackOnly();
            }
            return exception;
        } catch (Exception ex) {
            log().error("BD has problem!", ex);
            transaction.markRollbackOnly();
            return ex;
        } finally {
            if (transaction.getRollbackOnly()) {
                log().error("Transaction Rollback!");
                transaction.rollback();
            } else {
                transaction.commit();
            }
            session.close();
        }
    }

    /**
     * Сохраним всех. Без обновления.
     *
     * @param list их всех в БД.
     */
    public void saveAll(Collection list) {
        final Session ses = getSession();
        list.forEach(ses::save);
        ses.flush();
    }

    /**
     * Сохраним или обновим всех.
     *
     * @param list их всех в БД.
     */
    public void saveOrUpdateAll(Collection list) {
        final Session ses = getSession();
        list.forEach(ses::saveOrUpdate);
        ses.flush();
    }

    /**
     * Сохраним или обновим объект в БД.
     *
     * @param obj его сохраним.
     */
    public void saveOrUpdate(Object obj) {
        final Session ses = getSession();
        ses.saveOrUpdate(obj);
        ses.flush();
    }

    /**
     * Удалить коллекцию из БД.
     *
     * @param list их всех удалим.
     */
    public void deleteAll(Collection list) {
        final Session ses = getSession();
        list.forEach(ses::delete);
        ses.flush();
    }

    /**
     * Просто удалить объект из БД.
     *
     * @param obj его удалим.
     */
    public void delete(Object obj) {
        final Session ses = getSession();
        ses.delete(obj);
        ses.flush();
    }

    public List loadAll(Class clazz) {
        return findByCriteria(DetachedCriteria.forClass(clazz).setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY));
    }

    /**
     * Найти единственную запись и проиницыализировать готовый объект.
     *
     * @param obj    Заполняем этот объект.
     * @param srlzbl ищим по этому ID.
     */
    public void load(Object obj, Serializable srlzbl) {
        try (Session ses = openSession()) {
            ses.load(obj, srlzbl);
        }
    }

    /**
     * Найти по критерию.
     *
     * @param criteria критерий.
     * @return Найденный список.
     */
    public List findByCriteria(DetachedCriteria criteria) {
        try (Session ses = openSession()) {
            return criteria.getExecutableCriteria(ses).list();
        }
    }

    /**
     * Найти единственную запись.
     *
     * @param clazz  ищим этот тип.
     * @param srlzbl ищим по этому ID.
     * @param <T>    Результирующий тип.
     * @return Объект соглано ID.
     */
    public <T> T find(Class<T> clazz, Serializable srlzbl) {
        try (Session ses = openSession()) {
            return ses.get(clazz, srlzbl);
        }
    }

    /**
     * Выполнить поиск. Выражение hql.
     *
     * @param hql Выражение hql.
     * @return Список найденных записей как объекты.
     */
    public List find(String hql) {
        try (Session ses = openSession()) {
            return ses.createQuery(hql).list();
        }
       
    }

    /**
     * Получить открытую сессию для работы с БД.
     *
     * @return готовая хиберовская сессия.
     */
    public Session getSession() {
        final Session currentSession = sessionFactory.getCurrentSession();
        if (currentSession != null && currentSession.isOpen()) {
            return currentSession;
        } else {
            return sessionFactory.openSession();
        }
    }

    private Session openSession() {
        return sessionFactory.openSession();
    }
}
