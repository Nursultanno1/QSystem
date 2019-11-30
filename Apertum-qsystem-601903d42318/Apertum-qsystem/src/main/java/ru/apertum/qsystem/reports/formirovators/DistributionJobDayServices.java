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

import org.apache.http.HttpRequest;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.exceptions.ReportException;
import ru.apertum.qsystem.reports.common.Response;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Igor Savin.
 *
 * @author Igor Savin
 */
public class DistributionJobDayServices extends AFormirovator {

    /**
     * Метод формирования параметров для отчета. В отчет нужно передать некие параметры. Они упаковываются в Мар. Если параметры не нужны, то сформировать
     * пустой Мар.
     */
    @Override
    public Map getParameters(String driverClassName, String url, String username, String password, HttpRequest request) {
        return paramMap;
    }

    /**
     * Для параметров.
     */
    private final HashMap<String, Object> paramMap = new HashMap<>();

    /**
     * Метод получения коннекта к базе если отчет строится через коннект. Если отчет строится не через коннект, а формироватором, то выдать null.
     *
     * @return коннект соединения к базе или null.
     */
    @Override
    public Connection getConnection(String driverClassName, String url, String username, String password, HttpRequest request) {
        return getCntByUrl(driverClassName, url, username, password);
    }

    @Override
    public Response getDialog(String driverClassName, String url, String username, String password, HttpRequest request, String errorMessage) {
        final Response result = getDialog("/ru/apertum/qsystem/reports/web/get_date_distribution_services.html", request, errorMessage);
        final StringBuilder servicesSelect = new StringBuilder();
        try (final Connection conn = getConnection(driverClassName, url, username, password, request);
             final Statement stmt = conn.createStatement();
             final ResultSet rs =
                     stmt.executeQuery("SELECT id, name FROM services WHERE id NOT IN (SELECT DISTINCT prent_id FROM services WHERE prent_ID IS NOT NULL) ORDER BY name")) {
            while (rs.next()) {
                servicesSelect.append("<option value=").append(rs.getLong(1)).append(">").append(rs.getString(2)).append("\n");
            }
        } catch (SQLException ex) {
            servicesSelect.setLength(0);
            throw new ReportException("Ошибка выполнения запроса для диалога ввода пользователя. " + ex);
        }
        try {
            result.setData(new String(result.getData(), "UTF-8").replaceFirst("#DATA_FOR_TITLE#", "Распределение нагрузки внутри дня для услуги:")
                    .replaceFirst("#DATA_FOR_SERVICES#", servicesSelect.toString()).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            QLog.l().logger().error(ex);
        }
        servicesSelect.setLength(0);
        return result;
    }

    @Override
    public String validate(String driverClassName, String url, String username, String password, HttpRequest request, Map<String, String> params) {
        // проверка на корректность введенных параметров
        QLog.l().logger().trace("Принятые параметры \"" + params.toString() + "\".");
        if (params.size() == 3) {
            // date/serviceId/service
            Date date;
            String sdate;
            String service;
            long serviceId;
            try {
                date = formatDdMmYyyy.parse(params.get("date"));
                sdate = (new java.text.SimpleDateFormat("yyyy-MM-dd")).format(date);
                serviceId = Long.parseLong(params.get("service_id"));
                service = params.get("service");
            } catch (ParseException | NumberFormatException ex) {
                return "<br>Ошибка ввода параметров! Не все параметры введены корректно (дд.мм.гггг).";
            }
            paramMap.put("sdate", sdate);
            paramMap.put("date", date);
            paramMap.put("service_id", serviceId);
            paramMap.put("service", service);
        } else {
            return "<br>Ошибка ввода параметров!";
        }
        return null;
    }
}
