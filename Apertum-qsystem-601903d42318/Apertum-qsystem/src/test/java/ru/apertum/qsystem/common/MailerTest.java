package ru.apertum.qsystem.common;

import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Properties;


public class MailerTest {

    @BeforeMethod
    public void setUp() {
    }

    @Test
    public void testFetchConfig() throws Exception {
        Properties config = Mailer.fetchConfig();
        Assert.assertNotNull(config);
        Assert.assertNotNull(config.getProperty("mailing"));
    }


    @Test
    public void testSendReporterMail() throws Exception {
        Method method = Whitebox.getMethod(Mailer.class, "prepareMessage", String.class, String.class, String.class, File.class);
        MimeMessage message = (MimeMessage) method.invoke(Mailer.class, "", "", "", new File("asd.pdf"));
        Assert.assertNotNull(message);
    }
}