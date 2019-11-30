/*
 * Copyright (C) 2013 {Apertum}Projects. web: www.apertum.ru email: info@apertum.ru
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
package ru.apertum.qsystem.server.model.schedule;

import ru.apertum.qsystem.server.model.IidGetter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Перерывы в работе для предвариловки.
 *
 * @author Evgeniy Egorov
 */
@Entity
@Table(name = "break")
public class QBreak implements IidGetter, Serializable {

    /**
     * Конструктор перерыва. Вызывается из админки при добавлении.
     *
     * @param fromTime пирерыв от.
     * @param toTime   перерыв до.
     * @param hint     подсказка.
     * @param breaks   в какой список входит.
     */
    public QBreak(Date fromTime, Date toTime, String hint, QBreaks breaks) {
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.hint = hint;
        this.breaks = breaks;
    }

    public QBreak() {
    }

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Override
    public Long getId() {
        return id;
    }

    /**
     * Время начала перерыва.
     */
    @Column(name = "from_time")
    @Temporal(TemporalType.TIME)
    private Date fromTime;

    public Date getFromTime() {
        return fromTime;
    }

    public void setFromTime(Date fromTime) {
        this.fromTime = fromTime;
    }

    /**
     * Время конца перерыва.
     */
    @Column(name = "to_time")
    @Temporal(TemporalType.TIME)
    private Date toTime;

    public Date getToTime() {
        return toTime;
    }

    public void setToTime(Date toTime) {
        this.toTime = toTime;
    }

    /**
     * Время конца перерыва.
     */
    @Column(name = "hint")
    private String hint;

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    /**
     * Формат даты.
     */
    @Transient
    public final SimpleDateFormat formatHhMm = new SimpleDateFormat("HH:mm");

    @Override
    public String getName() {
        return formatHhMm.format(fromTime) + "-" + formatHhMm.format(toTime);
    }

    @Override
    public String toString() {
        return formatHhMm.format(fromTime) + "-" + formatHhMm.format(toTime);
    }

    @ManyToOne
    @JoinColumn(name = "breaks_id")
    private QBreaks breaks;

    public QBreaks getBreaks() {
        return breaks;
    }

    public void setBreaks(QBreaks breaks) {
        this.breaks = breaks;
    }

    public long diff() {
        return getToTime().getTime() - getFromTime().getTime();
    }

    /**
     * Проверить, не попало ли время сейчас в перерыв. Смотрим только на часы и минуты.
     *
     * @return Перерыв или нет.
     */
    public boolean isNow() {
        return isNow(new Date());
    }

    /**
     * Проверить, не попало ли время из даты в перерыв. Смотрим только на часы и минуты.
     *
     * @param date Смотрим только на часы и минуты.
     * @return Перерыв или нет.
     */
    public boolean isNow(Date date) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);
        int now = gc.get(GregorianCalendar.HOUR_OF_DAY) * 60 + gc.get(GregorianCalendar.MINUTE);
        gc.setTime(fromTime);
        int from = gc.get(GregorianCalendar.HOUR_OF_DAY) * 60 + gc.get(GregorianCalendar.MINUTE);
        gc.setTime(toTime);
        int to = gc.get(GregorianCalendar.HOUR_OF_DAY) * 60 + gc.get(GregorianCalendar.MINUTE);
        return from <= now && now <= to;
    }

}
