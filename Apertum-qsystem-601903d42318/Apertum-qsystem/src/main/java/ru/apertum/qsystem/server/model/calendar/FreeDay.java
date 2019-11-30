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
package ru.apertum.qsystem.server.model.calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import java.io.Serializable;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Класс даты календаря в которую неоказывается услуга.
 *
 * @author Evgeniy Egorov
 */
@Entity
@Table(name = "calendar_out_days")
public class FreeDay implements Serializable {

    @Override
    public String toString() {
        return date.toString();
    }

    /**
     * На то что этот выходной день попадает на эту дату.
     *
     * @param day сверяемая дата с выходным днем.
     * @return Если null то false, если год и день в году совпадут, то true.
     */
    public boolean sameDay(Date day) {
        if (day == null) {
            return false;
        }
        final GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(day);
        final GregorianCalendar gc2 = new GregorianCalendar();
        gc2.setTime(getDate());
        return (gc.get(GregorianCalendar.DAY_OF_YEAR) == gc2.get(GregorianCalendar.DAY_OF_YEAR) && gc.get(GregorianCalendar.YEAR) == gc2.get(GregorianCalendar.YEAR));
    }

    /**
     * Зря это. Год и день должен совпасть.
     *
     * @param obj дата.
     * @return да или нет. Год и день должен совпасть.
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof FreeDay && hashCode() == obj.hashCode() && sameDay(((FreeDay) obj).date);
    }

    @Override
    public int hashCode() {
        final GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);
        return (int) (gc.get(GregorianCalendar.YEAR) * 1000 + gc.get(GregorianCalendar.DAY_OF_YEAR) + getCalendarId() * 100000000);
    }

    public FreeDay() {
    }

    public FreeDay(Date date, QCalendar calendar) {
        this.date = date;
        this.calendarId = calendar.getId();
    }

    @Id
    //@GeneratedValue(strategy = GenerationType.AUTO)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "out_day")
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date date;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Column(name = "calendar_id")
    private Long calendarId;

    public Long getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(Long calendarId) {
        this.calendarId = calendarId;
    }
}
