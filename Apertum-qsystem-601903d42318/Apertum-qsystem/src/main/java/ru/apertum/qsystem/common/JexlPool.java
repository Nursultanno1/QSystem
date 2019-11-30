package ru.apertum.qsystem.common;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import ru.apertum.qsystem.common.exceptions.ServerException;


/**
 * Это пулл объектов JexlEngine для вычисления EL выражений.
 *
 * @author egorov
 */
public class JexlPool extends SoftReferenceObjectPool {

    private JexlPool(BasePoolableObjectFactory basePoolableObjectFactory) {
        super(basePoolableObjectFactory);
    }

    private static final class JexlPoolHolder {
        private static JexlPool instance = new JexlPool(new BasePoolableObjectFactory() {

            @Override
            public Object makeObject() throws Exception {
                return new JexlBuilder().create();
            }
        });
    }

    public static JexlPool getInstance() {
        return JexlPoolHolder.instance;
    }

    /**
     * Получаем движок преобразователя.
     *
     * @return Движок преобразователя.
     */
    public JexlEngine borrowJexl() {
        try {
            return (JexlEngine) JexlPoolHolder.instance.borrowObject();
        } catch (Exception ex) {
            throw new ServerException("Проблемы с JexlEngine pool. ", ex);
        }
    }

    /**
     * Возвращаем движок преобразователя.
     *
     * @param gson Движок преобразователя.
     */
    public void returnJexl(JexlEngine gson) {
        try {
            JexlPoolHolder.instance.returnObject(gson);
        } catch (Exception ex) {
            throw new ServerException("Проблемы с  JexlEngine pool. ", ex);
        }
    }
}

