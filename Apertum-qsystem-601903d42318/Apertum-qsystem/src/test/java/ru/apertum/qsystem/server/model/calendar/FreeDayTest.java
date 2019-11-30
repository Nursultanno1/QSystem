package ru.apertum.qsystem.server.model.calendar;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class FreeDayTest {

    QCalendar calendar;
    QCalendar calendar2;

    @BeforeMethod
    public void setUp() {
        calendar = new QCalendar(1L);
        calendar2 = new QCalendar(2L);
    }

    @Test
    public void testSameDay() {
        Date date = new Date();

        FreeDay freeDay = new FreeDay(date, calendar);
        assertTrue(freeDay.sameDay(date));
        assertFalse(freeDay.sameDay(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)));
        assertTrue(freeDay.sameDay(new Date(System.currentTimeMillis() - 60 * 1000L)));
    }

    @Test(dependsOnMethods = {"testSameDay"})
    public void testEquals() {
        Date date = new Date();
        FreeDay freeDay = new FreeDay(date, calendar);
        assertTrue(freeDay.equals(new FreeDay(date, calendar)));
        assertFalse(freeDay.equals(new FreeDay(date, calendar2)));
        assertFalse(freeDay.equals(new FreeDay(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L), calendar)));
        assertTrue(freeDay.equals(new FreeDay(new Date(System.currentTimeMillis() - 60 * 1000L), calendar)));

        HashMap<FreeDay, Integer> map = new HashMap<>();
        map.put(freeDay, 1);
        assertEquals(map.size(), 1);

        map.put(new FreeDay(date, calendar), 2);
        assertEquals(map.size(), 1);

        map.put(new FreeDay(date, calendar2), 3);
        assertEquals(map.size(), 2);

        map.put(new FreeDay(date, calendar2), 4);
        assertEquals(map.size(), 2);

        map.put(new FreeDay(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L), calendar), 5);
        assertEquals(map.size(), 3);

        map.put(new FreeDay(new Date(System.currentTimeMillis() - 60 * 1000L), calendar), 6);
        assertEquals(map.size(), 3);

        assertEquals(map.get(new FreeDay(new Date(System.currentTimeMillis() - 60 * 1000L), calendar2)), Integer.valueOf(4));
        assertEquals(map.get(new FreeDay(new Date(System.currentTimeMillis() - 26 * 60 * 60 * 1000L), calendar)), Integer.valueOf(5));
        assertEquals(map.get(new FreeDay(date, calendar)), Integer.valueOf(6));

        assertEquals(map.get(new FreeDay(new Date(System.currentTimeMillis() - 50 * 60 * 60 * 1000L), calendar)), null);
        assertEquals(map.get(new FreeDay(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L), calendar2)), null);
    }
}