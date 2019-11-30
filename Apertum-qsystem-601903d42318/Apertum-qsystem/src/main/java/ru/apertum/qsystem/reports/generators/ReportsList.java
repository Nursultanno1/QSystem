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
package ru.apertum.qsystem.reports.generators;

import net.sf.jasperreports.engine.JRDataSource;
import org.apache.http.HttpRequest;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.exceptions.ReportException;
import ru.apertum.qsystem.reports.common.RepResBundle;
import ru.apertum.qsystem.reports.common.Response;
import ru.apertum.qsystem.reports.model.QReportsList;
import ru.apertum.qsystem.reports.net.NetUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.HashMap;

/**
 * Список отчетов.
 *
 * @author Evgeniy Egorov
 */
public class ReportsList extends AGenerator {

    public ReportsList(String href, String resourceNameTemplate) {
        super(href, resourceNameTemplate);
    }

    @Override
    protected JRDataSource getDataSource(HttpRequest request) {
        throw new ReportException("Ошибочное  обращение к методу.");
    }

    @Override
    @SuppressWarnings("squid:S1192")
    protected Response preparationReport(HttpRequest request) {
        // в запросе должен быть пароль и пользователь, если нету, то отказ на вход
        String entityContent = NetUtil.getEntityContent(request);
        QLog.l().logger().trace("Принятые параметры \"" + entityContent + "\".");
        // ресурс для выдачи в браузер. это либо список отчетов при корректном логининге или отказ на вход
        String res = "/ru/apertum/qsystem/reports/web/error_login.html";
        String usr = "err";
        String pwd = request.toString();
        // разбирем параметры
        final HashMap<String, String> logonPostParams = NetUtil.getCookie(entityContent, "&");
        if (logonPostParams.containsKey("username") && logonPostParams.containsKey("parol")
                && QReportsList.getInstance().isTrueUser(logonPostParams.get("username"), logonPostParams.get("parol"))) {
            res = "/ru/apertum/qsystem/reports/web/reportList.html";
            usr = logonPostParams.get("username");
            pwd = logonPostParams.get("parol");
        }
        final InputStream inStream = getClass().getResourceAsStream(res);
        byte[] result = null;
        try {
            result = RepResBundle.getInstance().prepareString(
                    new String(Uses.readInputStream(inStream), StandardCharsets.UTF_8.name()))
                    .replaceFirst(Uses.ANCHOR_PROJECT_NAME_FOR_REPORT, Uses.getLocaleMessage("project.name"))
                    .getBytes(StandardCharsets.UTF_8.name());
            if ("/ru/apertum/qsystem/reports/web/reportList.html".equals(res)) {
                // добавим список аналитических отчетов
                result = new String(result, StandardCharsets.UTF_8.name())
                        .replaceFirst(Uses.ANCHOR_REPORT_LIST, QReportsList.getInstance().getHtmlRepList()).getBytes(StandardCharsets.UTF_8.name());
                // Добавим кукисы сессии
                //<META HTTP-EQUIV="Set-Cookie" CONTENT="NAME=value; EXPIRES=date; DOMAIN=domain_name; PATH=path; SECURE">
                final String cookie = "<META HTTP-EQUIV=\"Set-Cookie\" CONTENT=\"username=" + URLEncoder.encode(usr, StandardCharsets.UTF_8.name())
                        + "\">\n<META HTTP-EQUIV=\"Set-Cookie\" CONTENT=\"parol=" + URLEncoder.encode(pwd, StandardCharsets.UTF_8.name()) + "\">";
                result = new String(result, StandardCharsets.UTF_8.name()).replaceFirst(Uses.ANCHOR_COOCIES, cookie).getBytes(StandardCharsets.UTF_8.name());
            }
        } catch (IOException ex) {
            throw new ReportException("Ошибка чтения ресурса для диалогового выбора отчета. " + ex);
        }
        return new Response(result, logonPostParams);
    }

    @Override
    protected HashMap getParameters(HttpRequest request) {
        throw new ReportException("Ошибочное обращение  к методу.");
    }

    @Override
    protected Connection getConnection(HttpRequest request) {
        throw new ReportException("Ошибочное обращение к методу.");
    }

    @Override
    protected Response getDialog(HttpRequest request, String errorMessage) {
        throw new ReportException("Ошибочное обращение к методу.");
    }

    @Override
    protected String validate(HttpRequest request, HashMap<String, String> params) {
        return null;
    }
}
