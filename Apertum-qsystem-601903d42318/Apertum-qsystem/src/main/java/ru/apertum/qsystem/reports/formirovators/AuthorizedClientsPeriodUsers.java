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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Igor Savin.
 *
 * @author Igor Savin
 */
public class AuthorizedClientsPeriodUsers extends AFormirovator {

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
        final Response result = getDialog("/ru/apertum/qsystem/reports/web/get_period_clients_users.html", request, errorMessage);
        final StringBuilder usersSelect = new StringBuilder();
        try (final Connection conn = getConnection(driverClassName, url, username, password, request);
             final Statement stmt = conn.createStatement();
             final ResultSet rs = stmt.executeQuery("SELECT id, name FROM users ORDER BY name")) {
            while (rs.next()) {
                usersSelect.append("<option value=").append(rs.getLong(1)).append(">").append(rs.getString(2)).append("\n");
            }
        } catch (SQLException ex) {
            usersSelect.setLength(0);
            throw new ReportException("Ошибка выполнения запроса для диалога ввода пользователя. " + ex);
        }
        try {
            result.setData(new String(result.getData(), "UTF-8").replaceFirst("#DATA_FOR_TITLE#", "Отчет по авторизованным персонам за период для пользователя:").replaceFirst("#DATA_FOR_USERS#", usersSelect.toString()).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            QLog.l().logger().error(ex);
        }
        usersSelect.setLength(0);
        return result;
    }

    @Override
    public String validate(String driverClassName, String url, String username, String password, HttpRequest request, Map<String, String> params) {
        // проверка на корректность введенных параметров
        return validateStartEndUserValidate(driverClassName, url, username, password, request, params);
    }
}
