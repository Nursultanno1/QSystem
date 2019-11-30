package ru.apertum.qsystem.common;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class UsesTest {

    @Test
    public void msecToStr() {
        assertEquals(Uses.msecToStr(0), "0s.");
        assertEquals(Uses.msecToStr(2000), "2s.");
        assertEquals(Uses.msecToStr(2020), "2s.");
        assertEquals(Uses.msecToStr(1 * 60 * 1000), "1m. 0s.");
        assertEquals(Uses.msecToStr(3 * 60 * 1000 + 5 * 1000), "3m. 5s.");
        assertEquals(Uses.msecToStr(8 * 60 * 60 * 1000 + 3 * 60 * 1000 + 5 * 1000), "8h. 3m. 5s.");
        assertEquals(Uses.msecToStr(1 * 24 * 60 * 60 * 1000), "1d. 0h. 0m. 0s.");
        assertEquals(Uses.msecToStr(10 * 24 * 60 * 60 * 1000L + 8 * 60 * 60 * 1000 + 3 * 60 * 1000 + 5 * 1000), "10d. 8h. 3m. 5s.");
        assertEquals(Uses.msecToStr(24 * 24 * 60 * 60 * 1000L + 8 * 60 * 60 * 1000 + 3 * 60 * 1000 + 5 * 1000), "24d. 8h. 3m. 5s.");
        assertEquals(Uses.msecToStr(360 * 24 * 60 * 60 * 1000L + 8 * 60 * 60 * 1000 + 3 * 60 * 1000 + 5 * 1000), "360d. 8h. 3m. 5s.");
        assertEquals(Uses.msecToStr(747 * 24 * 60 * 60 * 1000L + 8 * 60 * 60 * 1000 + 3 * 60 * 1000 + 5 * 1000), "747d. 8h. 3m. 5s.");
    }
}