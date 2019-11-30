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
package ru.apertum.qsystem.reports.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.exceptions.ReportException;

/**
 * Раскопать http протокол в ручную.
 *
 * @author Evgeniy Egorov
 */
@SuppressWarnings("squid:S1319")
public class NetUtil {

    private NetUtil() {
    }

    private static final HashMap<HttpRequest, String> MAP_EC = new HashMap<>();

    /**
     * Достать тело из запроса браузера.
     *
     * @param request запрос браузера.
     * @return тело в виде строки.
     */
    public static synchronized String getEntityContent(HttpRequest request) {
        String result = MAP_EC.get(request);
        if (result == null) {

            if (request instanceof HttpEntityEnclosingRequest) {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                try {
                    result = EntityUtils.toString(entity);
                } catch (IOException | ParseException ex) {
                    throw new ReportException(ex.toString());
                }
            } else {
                result = "";
            }
            try {
                result = URLDecoder.decode(result, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException ex) {
                throw new ReportException(ex.toString());
            }
            MAP_EC.put(request, result);
        } else {
            QLog.lgRep().warn("-------------------------------");
        }
        return result;
    }

    public static synchronized void freeEntityContent(HttpRequest request) {
        MAP_EC.remove(request);
    }

    /**
     * Достать куки из запроса браузера.
     *
     * @param data      строка с куками.
     * @param delimiter разделитель кук.
     * @return мапа с куками.
     */
    public static synchronized HashMap<String, String> getCookie(String data, String delimiter) {
        final HashMap<String, String> res = new HashMap<>();
        final String[] ss = data.split(delimiter);
        for (String str : ss) {
            final String[] ss0 = str.split("=");
            try {
                res.put(URLDecoder.decode(ss0[0], "utf-8"), URLDecoder.decode(ss0.length == 1 ? "" : ss0[1], "utf-8"));
            } catch (UnsupportedEncodingException ex) {
                QLog.l().logRep().error(ss0[1], ex);
            }
        }
        return res;
    }

    /**
     * Достать УРЛ из запроса браузера.
     *
     * @param request запрос браузера.
     * @return УРЛ.
     */
    public static synchronized String getUrl(HttpRequest request) {
        final String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
        if (method.equals("GET")) {
            return request.getRequestLine().getUri().split("\\?")[0];
        } else {
            return request.getRequestLine().getUri();
        }
    }

    /**
     * Достать параметры из запроса браузера.
     *
     * @param request запрос браузера.
     * @return мапа с параметрами.
     */
    public static synchronized HashMap<String, String> getParameters(HttpRequest request) {
        final String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
        final HashMap<String, String> res = new HashMap<>();
        final String data;
        if (method.equals("GET")) {
            String[] ss = request.getRequestLine().getUri().split("\\?");
            if (ss.length == 2) {
                try {
                    data = URLDecoder.decode(request.getRequestLine().getUri().split("\\?")[1], StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException ex) {
                    throw new ReportException(ex.toString());
                }
            } else {
                data = "";
            }
        } else {
            data = getEntityContent(request);
        }
        String[] ss = data.split("&");
        for (String str : ss) {
            String[] ss1 = str.split("=");
            if (!ss1[0].isEmpty()) {
                res.put(ss1[0], ss1.length == 1 ? "" : ss1[1]);
            }
        }
        return res;
    }
}
