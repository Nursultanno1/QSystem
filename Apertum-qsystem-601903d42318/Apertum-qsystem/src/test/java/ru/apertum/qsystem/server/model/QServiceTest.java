package ru.apertum.qsystem.server.model;

import org.powermock.reflect.Whitebox;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.server.ServerProps;

import static org.testng.Assert.assertEquals;

public class QServiceTest {

    QService service;

    @BeforeMethod
    public void setUp() {
        Whitebox.setInternalState(QService.class, "lastStNumber", Integer.MIN_VALUE);
        service = new QService();
        service.setId(100500L);
    }

    @Test
    public void testGetNextNumberSeparate() {
        QService service2 = new QService();
        service2.setId(2L);
        service2.setStatus(1);

        QService service1 = new QService();
        service1.setId(100500L);
        service1.setStatus(1);

        ServerProps.getInstance().getProps().setNumering(true);
        ServerProps.getInstance().getProps().setFirstNumber(7);
        ServerProps.getInstance().getProps().setLastNumber(70);
        for (int i = 7; i <= 70; i++) {
            assertEquals(service1.getNextNumber(), i);
            for (int j = 7; j <= 70; j++) {
                assertEquals(service2.getNextNumber(), j);
            }
        }
        for (int i = 7; i <= 70; i++) {
            assertEquals(service1.getNextNumber(), i);
        }
        for (int i = 7; i <= 70; i++) {
            assertEquals(service1.getNextNumber(), i);
        }
    }

    @Test
    public void testGetNextNumberCross() {
        QService service2 = new QService();
        service2.setId(2L);
        service2.setStatus(1);

        QService service1 = new QService();
        service1.setId(100500L);
        service1.setStatus(1);

        ServerProps.getInstance().getProps().setNumering(false);
        Integer s = 7;
        ServerProps.getInstance().getProps().setFirstNumber(s);
        Integer f = 70;
        ServerProps.getInstance().getProps().setLastNumber(f);
        int n = s;
        for (int i = 1; i <= 100; i++) {
            assertEquals(service1.getNextNumber(), n);
            n = n == f ? s : ++n;
            assertEquals(service2.getNextNumber(), n);
            n = n == f ? s : ++n;
            assertEquals(service2.getNextNumber(), n);
            n = n == f ? s : ++n;
        }
    }

    @Test
    public void testGetNextNumberPersonal() {
        QService service2 = new QService();
        service2.setId(2L);
        service2.setStatus(1);

        QService service1 = new QService();
        service1.setId(7234856238475L);
        service1.setStatus(1);

        ServerProps.getInstance().getProps().setNumering(true);
        ServerProps.getInstance().getProps().setFirstNumber(7);
        ServerProps.getInstance().getProps().setLastNumber(70);

        ServerProps.Section section = ServerProps.getInstance().addSection(service1.getSectionName());
        section.addProperty(Uses.KEY_TICKET_NUMBERING, "from 13 to 130", "comment1");

        for (int i = 13; i <= 130; i++) {
            assertEquals(service1.getNextNumber(), i);
            for (int j = 7; j <= 70; j++) {
                assertEquals(service2.getNextNumber(), j);
            }
        }
        for (int i = 13; i <= 130; i++) {
            assertEquals(service1.getNextNumber(), i);
        }
        for (int i = 13; i <= 130; i++) {
            assertEquals(service1.getNextNumber(), i);
        }
        for (int j = 7; j <= 70; j++) {
            assertEquals(service2.getNextNumber(), j);
        }
    }

    @Test
    public void testGetNextNumberRulon() {
        QService service2 = new QService();
        service2.setId(2L);
        service2.setStatus(1);

        QService service1 = new QService();
        service1.setId(56572456233L);
        service1.setStatus(5);

        ServerProps.getInstance().getProps().setNumering(false);
        ServerProps.getInstance().getProps().setFirstNumber(7);
        ServerProps.getInstance().getProps().setLastNumber(70);

        ServerProps.Section section = ServerProps.getInstance().addSection(service1.getSectionName());
        section.addProperty(Uses.KEY_TICKET_NUMBERING, "from 13 to 130", "comment1");

        Whitebox.setInternalState(QService.class, "lastStNumber", Integer.MIN_VALUE);

        for (int i = 13; i <= 130; i++) {
            assertEquals(service1.getNextNumber(), i);
            for (int j = 7; j <= 70; j++) {
                assertEquals(service2.getNextNumber(), j);
            }
        }
        for (int i = 13; i <= 130; i++) {
            assertEquals(service1.getNextNumber(), i);
        }
        for (int i = 13; i <= 130; i++) {
            assertEquals(service1.getNextNumber(), i);
        }
        for (int j = 7; j <= 70; j++) {
            assertEquals(service2.getNextNumber(), j);
        }
    }
}