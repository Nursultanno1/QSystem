/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.qsystem.ub485.core;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import ru.apertum.qsystem.common.exceptions.ServerException;

/**
 * Пулл классов для распараллеливаия обработки пришедшего пакета. Чтоб их постоянно не создавать.
 *
 * @author Evgeniy Egorov
 */
public class ActionRunnablePool extends SoftReferenceObjectPool {

    private ActionRunnablePool(BasePoolableObjectFactory basePoolableObjectFactory) {
        super(basePoolableObjectFactory);
    }

    private static ActionRunnablePool instance = null;

    /**
     * Объект пула.
     */
    public static ActionRunnablePool getInstance() {
        if (instance == null) {

            instance = new ActionRunnablePool(new BasePoolableObjectFactory() {
                @Override
                public Object makeObject() throws Exception {
                    return new ActionTransmit();
                }
            });

        }
        return instance;
    }

    /**
     * Получить класс для использования.
     */
    public ActionTransmit borrowTransmitter() {
        try {
            return (ActionTransmit) instance.borrowObject();
        } catch (Exception ex) {
            throw new ServerException("Проблемы с ActionRunnablePool. ", ex);
        }
    }

    /**
     * Вернуть в пулл.
     *
     * @param transmitter его возвращаем.
     */
    public void returnTransmitter(ActionTransmit transmitter) {
        try {
            instance.returnObject(transmitter);
        } catch (Exception ex) {
            throw new ServerException("Проблемы с  ActionRunnablePool. ", ex);
        }
    }
}
