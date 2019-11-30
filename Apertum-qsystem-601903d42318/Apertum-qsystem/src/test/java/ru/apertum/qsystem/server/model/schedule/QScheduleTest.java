package ru.apertum.qsystem.server.model.schedule;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public class QScheduleTest {

    QSchedule schedule;

    @BeforeMethod
    public void setUp() {
        schedule = new QSchedule();
        schedule.setId(1L);
    }

    @Test
    public void testInterval() throws Exception {
        QSchedule.Interval interval1 = new QSchedule.Interval(time("10:10"), time("12:11"));
        QSchedule.Interval interval2 = new QSchedule.Interval(time("10:10"), time("12:11"));
        assertTrue(interval1.equals(interval2));
        assertFalse(interval1.equals(new QSchedule.Interval(time("10:11"), time("12:10"))));
        assertTrue(interval1.equals(new QSchedule.Interval(new SimpleDateFormat("yyyy.MM.dd HH:mm").parse("1985.02.21 10:10"),
                new SimpleDateFormat("yyyy.MM.dd HH:mm").parse("2011.12.12 12:11"))));
    }

    @Test
    public void testEquals() {
        QSchedule scd = new QSchedule();
        assertFalse(schedule.equals(scd));
        assertFalse(schedule.equals(null));

        scd = new QSchedule();
        scd.setId(1L);
        assertTrue(schedule.equals(scd));

        assertThrows(TypeNotPresentException.class, () -> schedule.equals(""));
    }

    @Test
    public void testGetWorkIntervalT1() throws Exception {
        schedule.setType(1);
        schedule.setTimeBegin1(time("02:00"));
        schedule.setTimeEnd1(time("03:00"));
        schedule.setTimeBegin2(time("07:00"));
        schedule.setTimeEnd2(time("08:00"));
        QSchedule.Interval interval = schedule.getWorkInterval(date("2019.01.10"));
        assertEquals(interval, new QSchedule.Interval(schedule.getTimeBegin1(), schedule.getTimeEnd1()));
        interval = schedule.getWorkInterval(date("2019.01.11"));
        assertEquals(interval, new QSchedule.Interval(schedule.getTimeBegin2(), schedule.getTimeEnd2()));
    }

    private Date date(String str) throws ParseException {
        return new SimpleDateFormat("yyyy.MM.dd").parse(str);
    }

    private Date time(String str) throws ParseException {
        return new SimpleDateFormat("HH:mm").parse(str);
    }

    @Test
    public void testGetWorkIntervalT2() throws Exception {
        schedule.setType(2);
        schedule.setTimeBegin1(time("02:00"));
        schedule.setTimeEnd1(time("03:00"));
        schedule.setTimeBegin2(time("07:00"));
        schedule.setTimeEnd2(time("08:00"));
        QSchedule.Interval interval = schedule.getWorkInterval(date("2019.01.07"));
        assertEquals(interval, new QSchedule.Interval(schedule.getTimeBegin1(), schedule.getTimeEnd1()));
        interval = schedule.getWorkInterval(date("2019.01.08"));
        assertEquals(interval, new QSchedule.Interval(schedule.getTimeBegin2(), schedule.getTimeEnd2()));
        interval = schedule.getWorkInterval(date("2019.01.09"));
        assertEquals(interval, new QSchedule.Interval(null, null));
    }

    @Test
    public void testInBreak() throws Exception {
        QBreaks breaks = new QBreaks();
        HashSet<QBreak> breakHashSet = new HashSet<>();
        breakHashSet.add(new QBreak(time("07:00"), time("08:00"), "hint", breaks));
        breakHashSet.add(new QBreak(time("10:00"), time("14:00"), "hint", breaks));
        breakHashSet.add(new QBreak(time("18:00"), time("20:00"), "hint", breaks));
        breaks.setBreaks(breakHashSet);
        schedule.setBreaks1(breaks);
        assertFalse(schedule.inBreak(new SimpleDateFormat("yyyy.MM.dd HH:mm").parse("2019.01.08 12:11")));
        assertTrue(schedule.inBreak(new SimpleDateFormat("yyyy.MM.dd HH:mm").parse("2019.01.07 12:11")));
        assertTrue(schedule.inBreak(new SimpleDateFormat("yyyy.MM.dd HH:mm").parse("2019.01.07 07:40")));
        assertTrue(schedule.inBreak(new SimpleDateFormat("yyyy.MM.dd HH:mm").parse("2019.01.07 19:01")));
        assertTrue(schedule.inBreak(new SimpleDateFormat("yyyy.MM.dd HH:mm").parse("2019.01.07 18:00")));
        assertTrue(schedule.inBreak(new SimpleDateFormat("yyyy.MM.dd HH:mm").parse("2019.01.07 20:00")));
        assertFalse(schedule.inBreak(new SimpleDateFormat("yyyy.MM.dd HH:mm").parse("2019.01.07 20:01")));
        assertFalse(schedule.inBreak(new SimpleDateFormat("yyyy.MM.dd HH:mm").parse("2019.14.07 06:59")));
        assertFalse(schedule.inBreak(new SimpleDateFormat("yyyy.MM.dd HH:mm").parse("2019.01.21 15:59")));
    }

}