/*
 * Copyright (C) 2010 {Apertum}Projects. web: www.apertum.ru email: info@apertum.ru
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
package ru.apertum.qsystem.common;

import ru.apertum.qsystem.common.exceptions.ServerException;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.NewsAddress;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;

/**
 * Отослать почту.
 *
 * @author Evgeniy Egorov
 */
@SuppressWarnings("squid:S4797")
public class Mailer {

    private Mailer() {
        //for Sonar
    }

    /**
     * Отослать почту в фоне.
     */
    public static void sendReporterMailAtFon(String subject, String content, String addrsTo, final String attachment) {
        final Thread t = new Thread(() -> {
            final File attach = new File(attachment);
            try {
                sendReporterMail(subject, content, addrsTo, attach.exists() ? attach : null);
            } catch (MessagingException | UnsupportedEncodingException ex) {
                throw new ServerException("Рассылка не произошла.", ex);
            }
        });
        t.start();
    }

    /**
     * Отослать почту синхронно.
     */
    @SuppressWarnings({"squid:S3776", "squid:S4784"})
    public static void sendReporterMail(String subject, String content, String addrsTo, File attachment) throws MessagingException, UnsupportedEncodingException {
        Transport.send(prepareMessage(subject, content, addrsTo, attachment));
    }

    private static MimeMessage prepareMessage(String subject, String content, String addrsTo, File attachment) throws MessagingException, UnsupportedEncodingException {
        Properties props = fetchConfig();

        final Authenticator auth = new MyAuthenticator(props.getProperty("mail.smtp.user"), props.getProperty("mail.password"));
        final Session session = Session.getDefaultInstance(props, auth);

        final MimeMessage msg = new MimeMessage(session);
        String to = addrsTo == null ? props.getProperty("mail.smtp.to") : addrsTo;
        to = to.replaceAll("  ", " ").replaceAll(" ;", ";").replaceAll(" ,", ",").replaceAll(", ", ",")
                .replaceAll("; ", ",").replaceAll(";", ",").replaceAll(" ", ",").replaceAll(",,", ",");
        final String[] ss = to.split(",");
        final ArrayList<InternetAddress> adresses = new ArrayList<>();
        for (String str : ss) {
            if (!"".equals(str.trim())) {
                adresses.add(new InternetAddress(str.trim()));
            }
        }

        msg.setSender(new InternetAddress(props.getProperty("mail.smtp.from"), props.getProperty("mail.smtp.fromTitle")));
        msg.setFrom(new InternetAddress(props.getProperty("mail.smtp.from"), props.getProperty("mail.smtp.fromTitle")));
        msg.setRecipients(Message.RecipientType.TO, adresses.toArray(new InternetAddress[0]));
        msg.setHeader("Content-Type", "text/html;charset=\"UTF-8\"");
        msg.setSubject(subject == null ? props.getProperty("mail.subject") : subject, StandardCharsets.UTF_8.name());

        final Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(getContent(content, props.getProperty("mail.content")));

        if (attachment != null) {
            final MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            final DataSource source = new FileDataSource(attachment);
            attachmentBodyPart.setDataHandler(new DataHandler(source));
            attachmentBodyPart.setFileName(MimeUtility.encodeText(source.getName()));
            multipart.addBodyPart(attachmentBodyPart);
        }

        msg.setContent(multipart);

        return msg;
    }

    private static BodyPart getContent(String content, String mailContentProp) throws MessagingException {
        final BodyPart messageBodyPart = new MimeBodyPart();
        File f = new File(mailContentProp);
        if (f.exists()) {
            final StringBuilder sb = new StringBuilder();
            try (final Scanner s = new Scanner(f, StandardCharsets.UTF_8.name())) {
                while (s.hasNext()) {
                    sb.append(s.next());
                }
            } catch (FileNotFoundException ex) {
                throw new ServerException(ex);
            }
            messageBodyPart.setContent(content == null ? sb.toString() : content, "text/html; charset=\"UTF-8\"");
            sb.setLength(0);
        } else {
            messageBodyPart.setContent(content == null ? mailContentProp : content, "text/plain; charset=\"UTF-8\"");
        }
        return messageBodyPart;
    }

    /**
     * Open a specific text file containing mail server parameters, and populate a corresponding Properties object.
     *
     * @return props настройки.
     */
    public static synchronized Properties fetchConfig() {
        if (fMailServerConfig == null) {
            fMailServerConfig = new Properties();
        } else {
            return fMailServerConfig;
        }
        try (InputStream input = new FileInputStream("config/reporter.properties");
             final InputStreamReader inR = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            //If possible, one should try to avoid hard-coding a path in this
            //manner; in a web application, one should place such a file in
            //WEB-INF, and access it using ServletContext.getResourceAsStream.
            //Another alternative is Class.getResourceAsStream.
            //This file contains the javax.mail config properties mentioned above.
            fMailServerConfig.load(inR);
        } catch (IOException ex) {
            throw new ServerException("Cannot open and load mail server properties file.", ex);
        }
        return fMailServerConfig;
    }

    private static Properties fMailServerConfig;


    /**
     * Аутентификация. Логин пароль на почтовом серваке.
     */
    static class MyAuthenticator extends Authenticator {

        private final String user;
        private final String password;

        MyAuthenticator(String user, String password) {
            this.user = user;
            this.password = password;
        }

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(user, password);
        }
    }
}
