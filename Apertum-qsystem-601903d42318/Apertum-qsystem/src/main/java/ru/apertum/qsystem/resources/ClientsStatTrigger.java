/*
 * Copyright (C) 2014 Evgeniy Egorov
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
package ru.apertum.qsystem.resources;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Триггеры для Н2.
 *
 * @author Evgeniy Egorov
 */
public class ClientsStatTrigger implements org.h2.api.Trigger {

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type) throws SQLException {
        // not in use.
    }

    /**
     * Что-то старенькое.
     * SET @finish_start= TIMEDIFF(NEW.finish_time, NEW.start_time);
     * SET @start_starnd = TIMEDIFF(NEW.start_time, NEW.stand_time);
     * INSERT
     * INTO statistic(state_in, results_id, user_id, client_id, service_id, user_start_time, user_finish_time, client_stand_time, user_work_period, client_wait_period)
     * VALUES
     * (NEW.state_in, NEW.result_id, NEW.user_id, NEW.id, NEW.service_id, NEW.start_time, NEW.finish_time, NEW.stand_time,
     * round(
     * (HOUR(@finish_start) * 60 * 60 +
     * MINUTE(@finish_start) * 60 +
     * SECOND(@finish_start) + 59)/60),
     * round(
     * (HOUR(@start_starnd) * 60 * 60 +
     * MINUTE(@start_starnd) * 60 +
     * SECOND(@start_starnd) + 59)/60)
     * );
     */
    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        // newRow:
        // 0:ID 1:SERVICE_ID  2:USER_ID  3:SERVICE_PREFIX  4:NUMBER 5:STAND_TIME  6:START_TIME 7:FINISH_TIME 8:CLIENTS_AUTHORIZATION_ID 9:RESULT_ID  10:INPUT_DATA  11:STATE_IN
        try (final PreparedStatement prep = getStatement(conn, newRow[9])) {
            int i = 0;
            if (newRow[9] == null) {
                i = 1;
            } else {
                prep.setLong(2, (long) newRow[9]);
            }

            prep.setInt(1, newRow[11] == null ? null : (int) newRow[11]);

            prep.setLong(3 - i, (long) newRow[2]);
            prep.setLong(4 - i, (long) newRow[0]);
            prep.setLong(5 - i, (long) newRow[1]);
            prep.setTimestamp(6 - i, (java.sql.Timestamp) newRow[6]);
            prep.setTimestamp(7 - i, (java.sql.Timestamp) newRow[7]);
            prep.setTimestamp(8 - i, (java.sql.Timestamp) newRow[5]);

            int finishStart = (int) (((java.sql.Timestamp) newRow[7]).getTime() - ((java.sql.Timestamp) newRow[6]).getTime()) / 1000 / 60;
            int startStarnd = (int) (((java.sql.Timestamp) newRow[6]).getTime() - ((java.sql.Timestamp) newRow[5]).getTime()) / 1000 / 60;
            prep.setInt(9 - i, finishStart == 0 ? 1 : finishStart);
            prep.setInt(10 - i, startStarnd == 0 ? 1 : startStarnd);
            prep.execute();
        }

    }

    private PreparedStatement getStatement(Connection conn, Object o) throws SQLException {
        final PreparedStatement prep;
        if (o == null) {
            prep = conn.prepareStatement(
                "INSERT "
                    + " INTO statistic(state_in,             user_id, client_id, service_id, user_start_time, user_finish_time, client_stand_time, "
                    + "user_work_period, client_wait_period)  "
                    + "    VALUES "
                    + "        (?,    ?, ?, ?, ?, ?, ?, ?, ? ); ");
        } else {
            prep = conn.prepareStatement(
                "INSERT "
                    + "INTO statistic(state_in, results_id, user_id, client_id, service_id, user_start_time, user_finish_time, client_stand_time, "
                    + "user_work_period, client_wait_period)  "
                    + "    VALUES "
                    + "        (?, ?, ?, ?, ?, ?, ?, ?, ?, ? ); ");
        }
        return prep;
    }

    @Override
    public void close() throws SQLException {
        // not in use.
    }

    @Override
    public void remove() throws SQLException {
        // not in use.
    }

}
