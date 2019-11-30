/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.qsystem.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import ru.apertum.qsystem.common.exceptions.ServerException;

import java.awt.Color;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Пул маршаллеров Gson.
 *
 * @author egorov
 */
public class GsonPool extends SoftReferenceObjectPool {

    private static class ColorSerializer implements JsonDeserializer<Color>, JsonSerializer<Color> {

        @Override
        public JsonElement serialize(Color arg0, Type arg1, JsonSerializationContext arg2) {
            return new JsonPrimitive(arg0.getRGB());
        }

        @Override
        public Color deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            return new Color(json.getAsInt());
        }
    }

    private static class DateSerializer implements JsonDeserializer<Date>, JsonSerializer<Date> {

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

        @Override
        public JsonElement serialize(Date arg0, Type arg1, JsonSerializationContext arg2) {
            return new JsonPrimitive(dateFormat.format(arg0));
        }

        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            try {
                return dateFormat.parse(json.getAsString());
            } catch (ParseException ex) {
                throw new ServerException("Not pars JSON by proxy!", ex);
            }
        }
    }

    private GsonPool(BasePoolableObjectFactory basePoolableObjectFactory) {
        super(basePoolableObjectFactory);
    }

    private static GsonPool instance = null;

    /**
     * Синглтон.
     */
    public static GsonPool getInstance() {
        if (instance == null) {

            instance = new GsonPool(new BasePoolableObjectFactory() {

                @Override
                public Object makeObject() {
                    final GsonBuilder gsonb = new GsonBuilder();
                    final DateSerializer ds = new DateSerializer();
                    final ColorSerializer cs = new ColorSerializer();
                    gsonb.registerTypeHierarchyAdapter(Date.class, ds);
                    gsonb.registerTypeHierarchyAdapter(Color.class, cs);
                    return gsonb.excludeFieldsWithoutExposeAnnotation().create();
                }
            });
        }
        return instance;
    }

    /**
     * Получить.
     *
     * @return полученный маршаллер.
     */
    public Gson borrowGson() {
        try {
            return (Gson) instance.borrowObject();
        } catch (Exception ex) {
            throw new ServerException("Проблемы с gson pool. ", ex);
        }
    }

    /**
     * Вернуть.
     *
     * @param gson его возвращаем.
     */
    public void returnGson(Gson gson) {
        try {
            instance.returnObject(gson);
        } catch (Exception ex) {
            throw new ServerException("Проблемы с  gson pool. ", ex);
        }
    }
}
