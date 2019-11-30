package ru.apertum.qsystem;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class AboutTest {

    @Test
    public void testMain() {
        About.main(new String[0]);
        assertNotNull(About.getDate());
        assertNotNull(About.getBuild());
        assertNotNull(About.getDb());
        assertNotNull(About.getVer());

        About.main(new String[]{"2019", "2", "22", "0", "1"});
        About.main(new String[]{"2019", "4", "22", "1", "1"});
        About.main(new String[]{"2019", "10", "30", "0", "0"});

        About.main(new String[]{"1"});
        About.main(new String[]{"2"});
        About.main(new String[]{"3"});
        About.main(new String[]{"4"});
        About.main(new String[]{"5"});
        About.main(new String[]{"6"});
        About.main(new String[]{"7"});
        About.main(new String[]{"8"});
        About.main(new String[]{"9"});
        About.main(new String[]{"10"});
        About.main(new String[]{"11"});
        About.main(new String[]{"12"});

        assertNotNull(About.getDate());
        assertNotNull(About.getBuild());
        assertNotNull(About.getDb());
        assertNotNull(About.getVer());
    }
}