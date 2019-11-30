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
package ru.apertum.qsystem.reports.formirovators;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import net.sf.jasperreports.engine.JRDataSource;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.HttpRequest;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.exceptions.ReportException;
import ru.apertum.qsystem.reports.common.RepResBundle;
import ru.apertum.qsystem.reports.common.Response;
import ru.apertum.qsystem.reports.net.NetUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Формирует источник данных для отчета.
 * Сделан для удобства. чтоб не делать каждый раз ненужные методы.
 *
 * @author Evgeniy Egorov
 */
public abstract class AFormirovator implements IFormirovator {

    /**
     * Формат даты.
     */
    protected final SimpleDateFormat formatDdMmYyyy = new SimpleDateFormat("dd.MM.yyyy");

    /**
     * Получение источника данных для отчета.
     *
     * @return Готовая структура для компилирования в документ.
     */
    @Override
    public JRDataSource getDataSource(String driverClassName, String url, String username, String password, HttpRequest request) {
        return null;
    }

    /**
     * Метод формирования параметров для отчета.
     * В отчет нужно передать некие параметры. Они упаковываются в Мар.
     * Если параметры не нужны, то сформировать пустой Мар. Иначе перекрыть и сформировать Map с параметрами.
     * Перекрыть при необходимости.
     *
     * @return
     */
    @Override
    public Map getParameters(String driverClassName, String url, String username, String password, HttpRequest request) {
        return new HashMap();
    }

    /**
     * Метод получения коннекта к базе если отчет строится через коннект.
     * Если отчет строится не через коннект, а формироватором, то выдать null.
     * Перекрыть при необходимости.
     *
     * @return коннект соединения к базе или null.
     */
    @Override
    public Connection getConnection(String driverClassName, String url, String username, String password, HttpRequest request) {
        return null;
    }

    /**
     * Типо если просто нужно отдать страницу.
     *
     * @param htmlFilePath шаблон
     * @param request      запрос из брайзера
     * @param errorMessage ошибка если че
     * @return готовая загруженная страница
     */
    protected Response getDialog(String htmlFilePath, HttpRequest request, String errorMessage) {

        // вставим необходимую ссылку на отчет в форму ввода
        // и выдадим ее клиенту на заполнение.
        // после заполнения вызовется нужный отчет с введенными параметрами и этот метод вернет null,
        // что продолжет генерить отчет методом getDataSource с нужными параметрами.
        // А здесь мы просто знаем какой формироватор должен какие формы выдавать пользователю. На то он и формироватор, индивидуальный для каждого отчета.
        // get_period_for_statistic_services.html
        byte[] result;
        try (final InputStream inStream = getClass().getResourceAsStream(htmlFilePath)) {
            result = Uses.readInputStream(inStream);
        } catch (IOException ex) {
            throw new ReportException("Ошибка чтения ресурса для диалогового ввода периода. " + ex);
        }
        if (errorMessage == null) {
            errorMessage = "";
        }
        Response res = null;
        try {
            res = new Response(RepResBundle.getInstance().prepareString(new String(result, "UTF-8")
                    .replaceFirst(Uses.ANCHOR_DATA_FOR_REPORT, NetUtil.getUrl(request))
                    .replaceFirst(Uses.ANCHOR_ERROR_INPUT_DATA, errorMessage)
                    .replaceFirst(Uses.ANCHOR_PROJECT_NAME_FOR_REPORT, Uses.getLocaleMessage("project.name")))
                    .getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new ReportException("Ошибка преобразования строки. " + ex);
        }
        return res;
    }

    /**
     * Метод получения коннекта к базе если отчет строится через коннект.
     * Если отчет строится не через коннект, а формироватором, то выдать null.
     *
     * @return коннект соединения к базе или null.
     */
    protected Connection getCntByUrl(String driverClassName, String url, String username, String password) {
        final Connection connection;
        try {
            Class.forName(driverClassName);
            connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException | ClassNotFoundException ex) {
            throw new ReportException(StatisticServices.class.getName() + " " + ex);
        }
        return connection;
    }

    /**
     * Для параметров.
     */
    protected final HashMap<String, Object> paramMap = new HashMap<>();

    protected String validateStartEndDates(String driverClassName, String url, String username, String password, HttpRequest request, Map<String, String> params) {
        //sd=20.01.2009&ed=28.01.2009
        // проверка на корректность введенных параметров
        QLog.l().logger().trace("Принятые параметры \"" + params.toString() + "\".");
        if (params.size() == 2) {
            Date sd;
            Date fd;
            Date fd1;
            try {
                sd = formatDdMmYyyy.parse(params.get("sd"));
                fd = formatDdMmYyyy.parse(params.get("ed"));
                fd1 = DateUtils.addDays(formatDdMmYyyy.parse(params.get("ed")), 1);
            } catch (ParseException ex) {
                return "<br>Ошибка ввода параметров! Не все параметры введены корректно(дд.мм.гггг).";
            }
            if (!sd.after(fd)) {
                paramMap.put("sd", sd);
                paramMap.put("ed", fd);
                paramMap.put("ed1", fd1);
            } else {
                return "<br>Ошибка ввода параметров! Дата начала больше даты завершения.";
            }
        } else {
            return "<br>Ошибка ввода параметров!";
        }
        return null;// все нормально
    }

    protected String validateStartEndUserValidate(String driverClassName, String url, String username, String password, HttpRequest request, Map<String, String> params) {
        // проверка на корректность введенных параметров
        QLog.l().logger().trace("Принятые параметры \"" + params.toString() + "\".");
        if (params.size() == 4) {
            // sd/ed/userId/user
            Date sd;
            Date fd;
            String sdate;
            String fdate;
            long userId;
            String user;
            try {
                sd = formatDdMmYyyy.parse(params.get("sd"));
                fd = formatDdMmYyyy.parse(params.get("ed"));
                sdate = (new java.text.SimpleDateFormat("yyyy-MM-dd")).format(sd);
                fdate = (new java.text.SimpleDateFormat("yyyy-MM-dd")).format(fd);
                userId = Long.parseLong(params.get("user_id"));
                user = params.get("user");
            } catch (ParseException | NumberFormatException ex) {
                return "<br>Ошибка ввода параметров! Не все параметры введены корректно (дд.мм.гггг).";
            }
            paramMap.put("sdate", sdate);
            paramMap.put("fdate", fdate);
            paramMap.put("sd", sd);
            paramMap.put("fd", fd);
            paramMap.put("user_id", userId);
            paramMap.put("user", user);
        } else {
            return "<br>Ошибка ввода параметров!";
        }
        return null;
    }

    /**
     * Ксли ничего дополнительного не требуется то метод и так вернет null.
     * При необходимости перекрыть
     *
     * @param driverClassName драйвер БД
     * @param url             урл подключения БД
     * @param username        пользователь
     * @param password        пароль
     * @param request         запрос из браузера.
     * @return данные. которые будут отосланы пользователю, т.к. этого не требуется то для удобства null чтобы постоянно его не реализовывать
     */
    @Override
    public Response preparationReport(String driverClassName, String url, String username, String password, HttpRequest request) {
        return null;
    }
}
