/*
 * Copyright (C) 2017 Apertum Project LLC
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
package ru.apertum.qsystem.common;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Синглтон предобразования строк с включениями EL ${...}. Потокобезопасный должен быть. Используется библиотека commons-jexl3.
 *
 * @author Evgeniy Egorov
 */
public class ElProcessor {

    private static final Logger LOGGER = QLog.log();

    private final Pattern pattern = Pattern.compile("\\$\\{.+?\\}");
    public static final String ONLY_LETTERS_REGEX = "[\\W&&[^а-яА-Я]]+?";

    private ElProcessor() {
    }

    public static ElProcessor get() {
        return ElProcessorHolder.INSTANCE;
    }

    private static class ElProcessorHolder {

        private static final ElProcessor INSTANCE = new ElProcessor();

        private ElProcessorHolder() {
        }
    }

    /**
     * Вычисляет EL выражения, найденные в строке, возвращяет результат.
     *
     * @param txt     строка с включенными в нее EL выражениями. Может быть NULL, в этом случае вернется строка "null".
     * @param context Контекст вычислетия, т.е. списов POJO классов, доступных по неким строковым именам - ключам этого ассоциированного массива. Вычисления кэшируются для
     *                определенного контекста. Если содерживое контекста изменилось, т.е. содержащиеся там POJO классы, то пересоздать сам класс контекста, положить в него
     *                данные и передать новый класс для вычислений. Иначе из кэша будут использованы вычисленные ранее значения.
     * @return Результирующая строка.
     */
    public String process(String txt, Map<String, Object> context) {
        if (txt == null) {
            return "null";
        }
        LOGGER.trace("EL.process(\"{}\") context: {}", txt, context.keySet());
        final StringBuilder sb = new StringBuilder();
        String txtEl = txt.replace("\r", "").replace("\n", "");
        final Matcher m = pattern.matcher(txtEl);
        int flag = 0;
        while (m.find()) {
            String exp = txtEl.substring(m.start() + 2, m.end() - 1);
            sb.append(txtEl.substring(flag, m.start()));
            flag = m.end();
            final String eval = eval(exp, context);
            sb.append(eval);
        }
        sb.append(txtEl.substring(flag));
        final String res = sb.toString();
        sb.setLength(0);
        return res;
    }

    /**
     * Вычисление EL выражения.
     *
     * @param jexlExp Правильное EL выражение без лишних строк и символов.
     * @param context Контекст вычислетия, т.е. списов POJO классов, доступных по неким строковым именам - ключам этого ассоциированного массива.
     * @return Объект-результат вычислений.
     */
    public Object evaluate(String jexlExp, Map<String, Object> context) {
        if (jexlExp == null) {
            return null;
        }
        LOGGER.trace("EL.evaluate(\"{}\") context: {}", jexlExp, context.keySet());
        // Create a context and add data
        final JexlContext jexlContext = makeContext(context);

        // Create or retrieve an engine
        final JexlEngine jexl = JexlPool.getInstance().borrowJexl();
        final JexlExpression e;
        try {
            // Create an expression
            e = jexl.createExpression(jexlExp.replace("\r", "").replace("\n", ""));
        } finally {
            JexlPool.getInstance().returnJexl(jexl);
        }
        // Now evaluate the expression, getting the result
        return e.evaluate(jexlContext);
    }

    /**
     * Создадим контекст и положим туда данные.
     *
     * @param context Данные
     * @return Контекст с данными
     */
    private JexlContext makeContext(Map<String, Object> context) {
        final JexlContext jexlContext = new MapContext();
        for (Map.Entry<String, Object> ctx : context.entrySet()) {
            jexlContext.set(ctx.getKey(), ctx.getValue());
        }
        return jexlContext;
    }

    /**
     * Вычисление EL в строку.
     *
     * @param jexlExp выражение EL
     * @param context Контекст вычислетия, т.е. списов POJO классов, доступных по неким строковым именам - ключам этого ассоциированного массива.
     * @return преобразованная строка.
     */
    private String eval(String jexlExp, Map<String, Object> context) {
        final Object o = evaluate(jexlExp, context);
        return o == null ? "null" : o.toString(); // тут оперируем только строковыми предствавлениями.
    }

}
