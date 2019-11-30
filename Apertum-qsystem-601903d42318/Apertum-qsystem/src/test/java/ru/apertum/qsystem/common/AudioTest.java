package ru.apertum.qsystem.common;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

@Ignore
public class AudioTest {

    @BeforeMethod
    public void setUp() {
    }

    @Test
    public void doSoundClassic() {
        Audio.doSoundClassic("/1.wav");
        Audio.doSoundClassic("/3.wav");
        Audio.doSoundClassic("/2.wav");
    }

    @Test
    public void testMakeSound() {
        Audio.playSound("/1.wav");
        Audio.playSound("/3.wav");
        Audio.playSound("/2.wav");
    }

    @Test
    public void testAePlayWave() {
        Audio.playWave("/1.wav");
        Audio.playWave("/3.wav");
        Audio.playWave("/2.wav");
    }

    @Test
    public void playBasic() {
        Audio.playBasic("/1.wav");
        Audio.playBasic("/3.wav");
        Audio.playBasic("/2.wav");
    }
}