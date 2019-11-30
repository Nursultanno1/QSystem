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
 *
 */
package ru.apertum.qsystem.common.cmd;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import ru.apertum.qsystem.server.model.QProperty;

import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.apertum.qsystem.common.QLog.log;

/**
 * Пармаметры для команд.
 *
 * @author Evgeniy Egorov
 */
public class CmdParams {

    public static final String CMD = "cmd";

    public CmdParams() {
    }

    public CmdParams(String params) {
        initFromString(params);
    }

    @Expose
    @SerializedName("service_id")
    public Long serviceId;
    @Expose
    @SerializedName("user_id")
    public Long userId;
    @Expose
    @SerializedName("pass")
    public String parolcheg;
    @Expose
    @SerializedName("priority")
    public Integer priority;
    @Expose
    @SerializedName("text_data")
    public String textData;
    @Expose
    @SerializedName("result_id")
    public Long resultId;
    @Expose
    @SerializedName("request_back")
    public Boolean requestBack;
    @Expose
    @SerializedName("drop_tickets_cnt")
    public Boolean dropTicketsCounter;
    /**
     * В качестве признака персональности едет ID юзера, чей отложенный должен быть, иначе null для общедоступности.
     */
    @Expose
    @SerializedName("is_only_mine")
    public Long isMine;
    @Expose
    @SerializedName("strict")
    public Boolean strict;
    @Expose
    @SerializedName("coeff")
    public Integer coeff;
    @Expose
    @SerializedName("date")
    public Long date;
    @Expose
    @SerializedName("customer_id")
    public Long customerId;
    @Expose
    @SerializedName("response_id")
    public Long responseId;
    @Expose
    @SerializedName("client_auth_id")
    public String clientAuthId;
    @Expose
    @SerializedName("info_item_name")
    public String infoItemName;
    @Expose
    @SerializedName("postponed_period")
    public Integer postponedPeriod;
    @Expose
    @SerializedName("comments")
    public String comments;
    @Expose
    @SerializedName("first_in_roll")
    public Integer first;
    @Expose
    @SerializedName("lastt_in_roll")
    public Integer last;
    @Expose
    @SerializedName("currentt_in_roll")
    public Integer current;
    @Expose
    @SerializedName("lng")
    public String language;
    /**
     * услуги, в которые пытаемся встать. Требует уточнения что это за трехмерный массив. Это пять списков. Первый это вольнопоследовательные услуги. Остальные
     * четыре это зависимопоследовательные услуги, т.е. пока один не закончится на другой не переходить. Что такое элемент списка. Это тоже список. Первый
     * элемент это та самая комплексная услуга(ID). А остальные это зависимости, т.е. если есть еще не оказанные услуги но назначенные, которые в зависимостях,
     * то их надо оказать.
     */
    @Expose
    @SerializedName("complex_id")
    public LinkedList<LinkedList<LinkedList<Long>>> complexId; // NOSONAR

    /**
     * Просто ассоциированный массив строк для передачи каких-то параметров. Пригодится для плагинов.
     */
    @Expose
    @SerializedName("strings_map")
    public Map<String, String> stringsMap;

    /**
     * Это список свойств для сохранения или инита на сервере.
     */
    @Expose
    @SerializedName("properties")
    public List<QProperty> properties;

    @SuppressWarnings("squid:S3776")
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("^?");
        final Field[] fs = getClass().getDeclaredFields();
        try {
            for (Field field : fs) {
                if (!(Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()))
                    && field.get(this) != null) {

                    field.setAccessible(true);//NOSONAR

                    if (field.getType().getSimpleName().contains("List")) {
                        List list = (List) field.get(this);
                        if (!list.isEmpty() && list.get(0) instanceof QProperty) {
                            sb.append("&").append(field.getName()).append("=");
                            list.stream().forEach(object -> {
                                try {
                                    sb.append(URLEncoder.encode("{" + object.toString() + "}", UTF_8.name()));
                                } catch (UnsupportedEncodingException ex) {
                                    log().error(ex);
                                }
                            });
                        }
                    } else {
                        if (field.getType().getSimpleName().endsWith("Map")) {
                            final Map map = (Map) field.get(this);
                            if (map.size() > 0) {
                                sb.append("&").append(field.getName()).append("=");
                                map.keySet().stream().forEach(object -> {
                                    try {
                                        sb.append(URLEncoder.encode("{" + object.toString() + "->" + map.get(object) + "}", UTF_8.name()));
                                    } catch (UnsupportedEncodingException ex) {
                                        log().error(ex);
                                    }
                                });
                            }
                        } else {

                            switch (field.getType().getSimpleName().toLowerCase(Locale.US)) {
                                case "int":
                                    sb.append("&").append(field.getName()).append("=").append(field.get(this));
                                    break;
                                case "integer":
                                    sb.append("&").append(field.getName()).append("=").append(field.get(this));
                                    break;
                                case "string":
                                    sb.append("&").append(field.getName()).append("=").append(URLEncoder.encode((String) field.get(this), UTF_8.name()));
                                    break;
                                case "boolean":
                                    sb.append("&").append(field.getName()).append("=").append(field.get(this));
                                    break;
                                case "long":
                                    sb.append("&").append(field.getName()).append("=").append(field.get(this));
                                    break;
                                default:
                                    throw new AssertionError();
                            }
                        }
                    }
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException | UnsupportedEncodingException ex) {
            log().error(ex);
        }

        final String st = sb.toString().replaceFirst("^\\^\\?\\&", "");
        sb.setLength(0);
        return st.length() < 3 ? "" : st;
    }

    /**
     * Загрузим из строки.
     *
     * @param params строка с параметрами.
     */
    @SuppressWarnings("squid:S3776")
    public final void initFromString(String params) {
        if (params == null || params.isEmpty()) {
            return;
        }
        for (String str : params.split("&")) {
            final String[] pp = str.split("=");

            final Field[] fs = getClass().getDeclaredFields();
            try {
                for (Field field : fs) {

                    if (!(Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()))
                        && pp[0].equals(field.getName())) {

                        field.setAccessible(true);//NOSONAR
                        if ("properties".equals(field.getName())) {
                            final List<QProperty> list = new LinkedList<>();
                            String lst = URLDecoder.decode(pp[1], UTF_8.name());
                            lst = lst.substring(1, lst.length() - 1);
                            final String[] ll = lst.split("\\}\\{");
                            for (String el : ll) {
                                final String[] ss = el.split("\\[|\\]|:");
                                if (ss.length == 4) {
                                    list.add(new QProperty(ss[1], ss[2], ss[3]));
                                }
                            }
                            field.set(this, list);//NOSONAR

                        } else {
                            switch (field.getType().getSimpleName().toLowerCase(Locale.US)) {
                                case "int":
                                    field.set(this, Integer.parseInt(pp[1]));//NOSONAR
                                    break;
                                case "integer":
                                    field.set(this, Integer.parseInt(pp[1]));//NOSONAR
                                    break;
                                case "string":
                                    field.set(this, pp.length == 1 ? "" : URLDecoder.decode(pp[1], UTF_8.name()));//NOSONAR
                                    break;
                                case "boolean":
                                    field.set(this, Boolean.parseBoolean(pp[1]));//NOSONAR
                                    break;
                                case "long":
                                    field.set(this, Long.parseLong(pp[1]));//NOSONAR
                                    break;
                                case "map":
                                    final HashMap<String, String> map = new HashMap<>();
                                    final String s = URLDecoder.decode(pp[1], UTF_8.name());
                                    final String[] ss = s.split("\\{|\\}");
                                    for (String s1 : ss) {
                                        final String[] ss1 = s1.split("->");
                                        if (ss1.length == 2) {
                                            map.put(ss1[0], ss1[1]);
                                        }
                                    }
                                    field.set(this, map);//NOSONAR
                                    break;
                                default:
                                    throw new AssertionError();
                            }
                        }
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException | UnsupportedEncodingException ex) {
                log().error(ex);
            }
        }
    }
}
