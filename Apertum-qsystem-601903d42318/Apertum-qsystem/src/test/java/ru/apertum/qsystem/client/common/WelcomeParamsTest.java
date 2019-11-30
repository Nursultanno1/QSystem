package ru.apertum.qsystem.client.common;

import java.io.File;
import java.util.LinkedHashMap;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import ru.apertum.qsystem.client.QProperties;
import ru.apertum.qsystem.server.ServerProps;
import ru.apertum.qsystem.server.model.QProperty;

import static org.testng.Assert.*;
import static ru.apertum.qsystem.client.common.WelcomeBGparams.BKG_PIC;
import static ru.apertum.qsystem.client.common.WelcomeBGparams.SCREEN;

public class WelcomeParamsTest {

    String backgroundImgOrig;

    @BeforeTest
    public void init() {
        backgroundImgOrig = WelcomeParams.getInstance().backgroundImg;
    }

    @BeforeMethod
    public void setUp() {
        WelcomeParams.getInstance().backgroundImg = backgroundImgOrig;
    }

    @AfterTest
    public void done() {
        WelcomeParams.getInstance().backgroundImg = backgroundImgOrig;
    }

    @Test
    public void testGetBackgroundImg() {
        assertEquals(WelcomeParams.getInstance().backgroundImg, backgroundImgOrig);

        LinkedHashMap<String, QProperty> properties = new LinkedHashMap<>();
        properties.put(SCREEN + "_" + BKG_PIC + "_" + 123456L, new QProperty(QProperties.SECTION_WELCOME, SCREEN + "_" + BKG_PIC + "_" + 123456L, "newImg"));

        ServerProps.Section section = new ServerProps.Section(QProperties.SECTION_WELCOME, properties);

        QProperties.get().addSection(section);
        assertEquals(WelcomeParams.getInstance().getBackgroundImg(123456L), backgroundImgOrig);

        File file = new File("gradle.properties");
        properties.put(SCREEN + "_" + BKG_PIC, new QProperty(QProperties.SECTION_WELCOME, SCREEN + "_" + BKG_PIC, file.getAbsolutePath()));
        assertEquals(WelcomeParams.getInstance().getBackgroundImg(123456L), file.getAbsolutePath());

        file = new File("build.gradle");
        properties.put(SCREEN + "_" + BKG_PIC + "_" + 123456L, new QProperty(QProperties.SECTION_WELCOME, SCREEN + "_" + BKG_PIC + "_" + 123456L, file.getAbsolutePath()));
        assertEquals(WelcomeParams.getInstance().getBackgroundImg(123456L), file.getAbsolutePath());
    }
}