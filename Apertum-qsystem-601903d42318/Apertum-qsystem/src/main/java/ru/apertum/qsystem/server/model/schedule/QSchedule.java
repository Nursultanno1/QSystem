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
package ru.apertum.qsystem.server.model.schedule;

import java.util.Calendar;
import ru.apertum.qsystem.common.exceptions.ServerException;
import ru.apertum.qsystem.server.model.IidGetter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Класс плана для расписания.
 *
 * @author Evgeniy Egorov
 */
@Entity
@Table(name = "schedule")
public class QSchedule implements IidGetter, Serializable {

    public QSchedule() {
        // for Hibernate.
    }

    @Id
    @Column(name = "id")
    //@GeneratedValue(strategy = GenerationType.AUTO)
    private Long id = new Date().getTime();

    /**
     * Наименование плана.
     */
    @Column(name = "name")
    private String name;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof QSchedule)) {
            throw new TypeNotPresentException("Неправильный тип сравнения для", new ServerException("Неправильный для сравнения тип"));
        }
        return id.equals(((QSchedule) o).id);
    }

    @Override
    public int hashCode() {
        return (int) (this.id != null ? this.id : 0);
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Наименование плана.
     */
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Тип плана 0 - недельный 1 - четные/нечетные дни.
     */
    @Column(name = "type")
    private Integer type;

    /**
     * Тип плана 0 - недельный 1 - четные/нечетные дни.
     *
     * @return Тип плана.
     */
    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    /**
     * Начало и конец рабочего дня, к примеру.
     */
    public static class Interval {

        public final Date start;
        public final Date finish;
        public final int startHours;
        public final int startMin;
        public final int finishHours;
        public final int finishMin;

        /**
         * Начало и конец рабочего дня, к примеру.
         */
        public Interval(Date start, Date finish) {
            if (start == null || finish == null) {
                this.start = null;
                this.finish = null;
                startHours = -1;
                startMin = -1;
                finishHours = -1;
                finishMin = -1;
            } else {
                if (finish.before(start)) {
                    throw new ServerException("Finish date " + finish + " before than start date " + start);
                }
                this.start = start;
                this.finish = finish;
                final GregorianCalendar gc = new GregorianCalendar();
                gc.setTime(start);
                startHours = gc.get(Calendar.HOUR_OF_DAY);
                startMin = gc.get(Calendar.MINUTE);
                gc.setTime(finish);
                finishHours = gc.get(Calendar.HOUR_OF_DAY);
                finishMin = gc.get(Calendar.MINUTE);
            }
        }

        public int diff() {
            return finishHours * 60 + finishMin - startHours * 60 - startMin;
        }

        @Override
        public String toString() {
            return startHours + ":" + startMin + "-" + finishHours + ":" + finishMin;
        }

        @Override
        public int hashCode() {
            return startHours + startMin + finishHours + finishMin + diff();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (!(o instanceof Interval)) {
                throw new TypeNotPresentException("Неправильный тип для сравнения", new ServerException("Неправильный тип для сравнения"));
            }
            return startHours == ((Interval) o).startHours
                    && startMin == ((Interval) o).startMin
                    && finishHours == ((Interval) o).finishHours
                    && finishMin == ((Interval) o).finishMin;
        }
    }

    /**
     * Определим время начала и kонца работы на этот день.
     *
     * @param date на этот день
     * @return время начала и kонца работы
     */
    public Interval getWorkInterval(Date date) {
        // Определим время начала и kонца работы на этот день
        final GregorianCalendar gcDay = new GregorianCalendar();
        gcDay.setTime(date);
        final Interval in;
        if (getType() == 1) {
            if (0 == (gcDay.get(GregorianCalendar.DAY_OF_MONTH) % 2)) {
                in = new Interval(getTimeBegin1(), getTimeEnd1());
            } else {
                in = new Interval(getTimeBegin2(), getTimeEnd2());
            }
        } else {
            switch (gcDay.get(GregorianCalendar.DAY_OF_WEEK)) {
                case 2:
                    in = new Interval(getTimeBegin1(), getTimeEnd1());
                    break;
                case 3:
                    in = new Interval(getTimeBegin2(), getTimeEnd2());
                    break;
                case 4:
                    in = new Interval(getTimeBegin3(), getTimeEnd3());
                    break;
                case 5:
                    in = new Interval(getTimeBegin4(), getTimeEnd4());
                    break;
                case 6:
                    in = new Interval(getTimeBegin5(), getTimeEnd5());
                    break;
                case 7:
                    in = new Interval(getTimeBegin6(), getTimeEnd6());
                    break;
                case 1:
                    in = new Interval(getTimeBegin7(), getTimeEnd7());
                    break;
                default:
                    throw new ServerException("32-е мая!");
            }
        }// Определили начало и конец рабочего дня на сегодня
        final GregorianCalendar gc = new GregorianCalendar();
        if (in.start == null || in.finish == null) {
            return in;
        } else {
            gc.setTime(in.start);
            gcDay.set(GregorianCalendar.HOUR_OF_DAY, gc.get(GregorianCalendar.HOUR_OF_DAY));
            gcDay.set(GregorianCalendar.MINUTE, gc.get(GregorianCalendar.MINUTE));
            gcDay.set(GregorianCalendar.SECOND, 0);
            final Date ds = gcDay.getTime();
            gc.setTime(in.finish);
            gcDay.setTime(date);
            gcDay.set(GregorianCalendar.HOUR_OF_DAY, gc.get(GregorianCalendar.HOUR_OF_DAY));
            gcDay.set(GregorianCalendar.MINUTE, gc.get(GregorianCalendar.MINUTE));
            gcDay.set(GregorianCalendar.SECOND, 0);
            return new Interval(ds, gcDay.getTime());
        }
    }

    /**
     * Проверка на перерыв. К примеру. В перерывах нет возможности записываться, по этому это время не поедет в пункт регистрации. Есть расписание, у него на
     * каждый день список перерывов. Папало ли время в перерыв на ту дату
     *
     * @param date проверка этой даты на перерыв.
     * @return да или нет.
     */
    public boolean inBreak(Date date) {
        // Проверка на перерыв. В перерывах нет возможности записываться, по этому это время не поедет в пункт регистрации
        final GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);
        int ii = gc.get(GregorianCalendar.DAY_OF_WEEK) - 1;
        if (ii < 1) {
            ii = 7;
        }
        final QBreaks breaks;
        switch (ii) {
            case 1:
                breaks = getBreaks1();
                break;
            case 2:
                breaks = getBreaks2();
                break;
            case 3:
                breaks = getBreaks3();
                break;
            case 4:
                breaks = getBreaks4();
                break;
            case 5:
                breaks = getBreaks5();
                break;
            case 6:
                breaks = getBreaks6();
                break;
            case 7:
                breaks = getBreaks7();
                break;
            default:
                throw new AssertionError();
        }
        return breaks != null && breaks.isNow(date) != null;// может вообще перерывов нет
    }

    /**
     * Проверка на перерыв. К примеру. В перерывах нет возможности записываться, по этому это время не поедет в пункт регистрации. Есть расписание, у него на
     * каждый день список перерывов. Папал ли интервал(пересечение) в перерыв на ту дату
     *
     * @param interval проверка этго интервала на перерыв
     * @return да или нет
     */
    public boolean inBreak(Interval interval) {
        final GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(interval.finish);
        gc.add(GregorianCalendar.SECOND, -3);
        return inBreak(interval.start) || inBreak(gc.getTime());
    }

    /**
     * Проверка на перерыв. К примеру. В перерывах нет возможности записываться, по этому это время не поедет в пункт регистрации. Есть расписание, у него на
     * каждый день список перерывов. Папал ли интервал(пересечение) в перерыв на ту дату
     *
     * @param start  начало этoго интервала на перерыв
     * @param finish конец этoго интервала на перерыв
     * @return да или нет
     */
    public boolean inBreak(Date start, Date finish) {
        final GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(finish);
        gc.add(GregorianCalendar.SECOND, -3);
        return inBreak(start) || inBreak(gc.getTime());
    }

    /**
     * Время начала работы в первый день недели или в нечетный день, зависит от type.
     */
    @Column(name = "time_begin_1")
    @Temporal(TemporalType.TIME)
    private Date timeBegin1;

    public Date getTimeBegin1() {
        return timeBegin1;
    }

    public void setTimeBegin1(Date timeBegin1) {
        this.timeBegin1 = timeBegin1;
    }

    /**
     * Время завершения работы в первый день недели или в нечетный день, зависит от type.
     */
    @Column(name = "time_end_1")
    @Temporal(TemporalType.TIME)
    private Date timeEnd1;

    public Date getTimeEnd1() {
        return timeEnd1;
    }

    public void setTimeEnd1(Date timeEnd1) {
        this.timeEnd1 = timeEnd1;
    }

    @Column(name = "time_begin_2")
    @Temporal(TemporalType.TIME)
    private Date timeBegin2;

    public Date getTimeBegin2() {
        return timeBegin2;
    }

    public void setTimeBegin2(Date timeBegin2) {
        this.timeBegin2 = timeBegin2;
    }

    @Column(name = "time_end_2")
    @Temporal(TemporalType.TIME)
    private Date timeEnd2;

    public Date getTimeEnd2() {
        return timeEnd2;
    }

    public void setTimeEnd2(Date timeEnd2) {
        this.timeEnd2 = timeEnd2;
    }

    @Column(name = "time_begin_3")
    @Temporal(TemporalType.TIME)
    private Date timeBegin3;

    public Date getTimeBegin3() {
        return timeBegin3;
    }

    public void setTimeBegin3(Date timeBegin3) {
        this.timeBegin3 = timeBegin3;
    }

    @Column(name = "time_end_3")
    @Temporal(TemporalType.TIME)
    private Date timeEnd3;

    public Date getTimeEnd3() {
        return timeEnd3;
    }

    public void setTimeEnd3(Date timeEnd3) {
        this.timeEnd3 = timeEnd3;
    }

    @Column(name = "time_begin_4")
    @Temporal(TemporalType.TIME)
    private Date timeBegin4;

    public Date getTimeBegin4() {
        return timeBegin4;
    }

    public void setTimeBegin4(Date timeBegin4) {
        this.timeBegin4 = timeBegin4;
    }

    @Column(name = "time_end_4")
    @Temporal(TemporalType.TIME)
    private Date timeEnd4;

    public Date getTimeEnd4() {
        return timeEnd4;
    }

    public void setTimeEnd4(Date timeEnd4) {
        this.timeEnd4 = timeEnd4;
    }

    @Column(name = "time_begin_5")
    @Temporal(TemporalType.TIME)
    private Date timeBegin5;

    public Date getTimeBegin5() {
        return timeBegin5;
    }

    public void setTimeBegin5(Date timeBegin5) {
        this.timeBegin5 = timeBegin5;
    }

    @Column(name = "time_end_5")
    @Temporal(TemporalType.TIME)
    private Date timeEnd5;

    public Date getTimeEnd5() {
        return timeEnd5;
    }

    public void setTimeEnd5(Date timeEnd5) {
        this.timeEnd5 = timeEnd5;
    }

    @Column(name = "time_begin_6")
    @Temporal(TemporalType.TIME)
    private Date timeBegin6;

    public Date getTimeBegin6() {
        return timeBegin6;
    }

    public void setTimeBegin6(Date timeBegin6) {
        this.timeBegin6 = timeBegin6;
    }

    @Column(name = "time_end_6")
    @Temporal(TemporalType.TIME)
    private Date timeEnd6;

    public Date getTimeEnd6() {
        return timeEnd6;
    }

    public void setTimeEnd6(Date timeEnd6) {
        this.timeEnd6 = timeEnd6;
    }

    @Column(name = "time_begin_7")
    @Temporal(TemporalType.TIME)
    private Date timeBegin7;

    public Date getTimeBegin7() {
        return timeBegin7;
    }

    public void setTimeBegin7(Date timeBegin7) {
        this.timeBegin7 = timeBegin7;
    }

    @Column(name = "time_end_7")
    @Temporal(TemporalType.TIME)
    private Date timeEnd7;

    public Date getTimeEnd7() {
        return timeEnd7;
    }

    public void setTimeEnd7(Date timeEnd7) {
        this.timeEnd7 = timeEnd7;
    }

    @OneToOne
    @JoinColumn(name = "breaks_id1")
    private QBreaks breaks1;

    public QBreaks getBreaks1() {
        return breaks1;
    }

    public void setBreaks1(QBreaks breaks1) {
        this.breaks1 = breaks1;
    }

    /**
     * Перерывы.
     *
     * @return Синглтон с перерывами.
     */
    @Transient
    public QBreaks getBreaks() {
        GregorianCalendar gc = new GregorianCalendar();
        final int day = gc.get(GregorianCalendar.DAY_OF_WEEK) - 1 < 1 ? 7 : (gc.get(GregorianCalendar.DAY_OF_WEEK) - 1);
        switch (day) {
            case 1:
                return getBreaks1();
            case 2:
                return getBreaks2();
            case 3:
                return getBreaks3();
            case 4:
                return getBreaks4();
            case 5:
                return getBreaks5();
            case 6:
                return getBreaks6();
            case 7:
                return getBreaks7();
            default:
                throw new AssertionError(day);
        }
    }

    @ManyToOne
    @JoinColumn(name = "breaks_id2")
    private QBreaks breaks2;
    @ManyToOne
    @JoinColumn(name = "breaks_id3")
    private QBreaks breaks3;
    @ManyToOne
    @JoinColumn(name = "breaks_id4")
    private QBreaks breaks4;
    @ManyToOne
    @JoinColumn(name = "breaks_id5")
    private QBreaks breaks5;
    @ManyToOne
    @JoinColumn(name = "breaks_id6")
    private QBreaks breaks6;
    @ManyToOne
    @JoinColumn(name = "breaks_id7")
    private QBreaks breaks7;

    public QBreaks getBreaks2() {
        return breaks2;
    }

    public void setBreaks2(QBreaks breaks2) {
        this.breaks2 = breaks2;
    }

    public QBreaks getBreaks3() {
        return breaks3;
    }

    public void setBreaks3(QBreaks breaks3) {
        this.breaks3 = breaks3;
    }

    public QBreaks getBreaks4() {
        return breaks4;
    }

    public void setBreaks4(QBreaks breaks4) {
        this.breaks4 = breaks4;
    }

    public QBreaks getBreaks5() {
        return breaks5;
    }

    public void setBreaks5(QBreaks breaks5) {
        this.breaks5 = breaks5;
    }

    public QBreaks getBreaks6() {
        return breaks6;
    }

    public void setBreaks6(QBreaks breaks6) {
        this.breaks6 = breaks6;
    }

    public QBreaks getBreaks7() {
        return breaks7;
    }

    public void setBreaks7(QBreaks breaks7) {
        this.breaks7 = breaks7;
    }
}
