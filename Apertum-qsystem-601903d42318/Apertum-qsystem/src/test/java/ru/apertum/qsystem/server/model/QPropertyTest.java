package ru.apertum.qsystem.server.model;

import java.util.ArrayList;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class QPropertyTest {

    QProperty property;

    @BeforeMethod
    public void setUp() {
        property = new QProperty();
    }

    @Test
    public void testGetValueAsInt() {
        assertEquals(property.getValueAsInt(), null);
        property.setValue("asd");
        assertEquals(property.getValueAsInt(), null);
        property.setValue("123");
        assertEquals(property.getValueAsInt().intValue(), 123);
    }

    @Test
    public void testGetValueAsIntDefault() {
        assertEquals(property.getValueAsInt(123), 123);
        property.setValue("asd");
        assertEquals(property.getValueAsInt(234), 234);
        property.setValue("100500");
        assertEquals(property.getValueAsInt(123), 100500);
    }

    @Test
    public void testGetValueAsLong() {
        assertEquals(property.getValueAsLong(), null);
        property.setValue("asd");
        assertEquals(property.getValueAsLong(), null);
        property.setValue("123");
        assertEquals(property.getValueAsLong().longValue(), 123L);
    }

    @Test
    public void testGetValueAsLongtDefault() {
        assertEquals(property.getValueAsLong(123L), 123L);
        property.setValue("asd");
        assertEquals(property.getValueAsLong(234L), 234L);
        property.setValue("100500");
        assertEquals(property.getValueAsLong(123L), 100500L);
    }

    @Test
    public void testGetValueAsIntArray() {
        assertEquals(property.getValueAsIntArray(null).size(), 0);

        ArrayList<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(10);
        list.add(100);
        list.add(12345);
        property.setValueAsIntArray(list, ";");
        ArrayList<Integer> array = property.getValueAsIntArray(";");
        assertEquals(array, list);
        array = property.getValueAsIntArray(null);
        assertEquals(array, list);


        property.setValue("");
        array = property.getValueAsIntArray(null);
        assertEquals(array.size(), 0);

    }
}