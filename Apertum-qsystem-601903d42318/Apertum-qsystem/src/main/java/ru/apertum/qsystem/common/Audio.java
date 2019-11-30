package ru.apertum.qsystem.common;

import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Semaphore;

import static ru.apertum.qsystem.common.QLog.log;

@SuppressWarnings({"squid:S1192", "squid:S4797"})
public class Audio {

    /**
     * Classic.
     *
     * @param resourceName file with sound.
     * @deprecated что-то плохо работает, убираем и оставляем другие реализации.
     */
    public static synchronized void doSoundClassic(String resourceName) {
        log().debug("Try to play sound \"" + resourceName + "\"");
        AudioInputStream ais = null;
        try {
            final URL url = Object.class.getResource(resourceName);
            ais = url == null ? AudioSystem.getAudioInputStream(new File(resourceName)) : AudioSystem.getAudioInputStream(url);
            //get the AudioFormat for the AudioInputStream
            AudioFormat audioformat = ais.getFormat();
            //printAudioFormatInfo(audioformat)
            //ULAW & ALAW format to PCM format conversion
            if ((audioformat.getEncoding() == AudioFormat.Encoding.ULAW)
                    || (audioformat.getEncoding() == AudioFormat.Encoding.ALAW)) {
                AudioFormat newformat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        audioformat.getSampleRate(),
                        audioformat.getSampleSizeInBits() * 2,
                        audioformat.getChannels(),
                        audioformat.getFrameSize() * 2,
                        audioformat.getFrameRate(),
                        true);
                ais = AudioSystem.getAudioInputStream(newformat, ais);
                audioformat = newformat;
            }
            //checking for a supported output line
            DataLine.Info datalineinfo = new DataLine.Info(SourceDataLine.class, audioformat);
            if (AudioSystem.isLineSupported(datalineinfo)) {
                byte[] sounddata;
                try (SourceDataLine sourcedataline = (SourceDataLine) AudioSystem.getLine(datalineinfo)) {
                    sourcedataline.open(audioformat);
                    sourcedataline.start();
                    int framesizeinbytes = audioformat.getFrameSize();
                    int bufferlengthinframes = sourcedataline.getBufferSize() / 8;
                    int bufferlengthinbytes = bufferlengthinframes * framesizeinbytes;
                    sounddata = new byte[bufferlengthinbytes];
                    int numberofbytesread;
                    while ((numberofbytesread = ais.read(sounddata)) != -1) {
                        sourcedataline.write(sounddata, 0, numberofbytesread);
                    }
                    int frPos = -1;
                    while (frPos != sourcedataline.getFramePosition()) {
                        frPos = sourcedataline.getFramePosition();
                        Audio.class.wait(50);
                    }
                }
            }
        } catch (InterruptedException ex) {
            log().error("InterruptedException: ", ex);
            Thread.currentThread().interrupt();
        } catch (LineUnavailableException lue) {
            log().error("LineUnavailableException: " + lue.toString());
        } catch (UnsupportedAudioFileException uafe) {
            log().error("UnsupportedAudioFileException: " + uafe.toString());
        } catch (IOException ioe) {
            log().error("IOException: " + ioe.toString());
        } finally {
            try {
                if (ais != null) {
                    ais.close();
                }
            } catch (IOException ex) {
                log().error("IOException при освобождении входного потока медиаресурса: ", ex);
            }
        }
    }

    /**
     * Neoclassic.
     *
     * @param filename the name of the file that is going to be played.
     */
    public static synchronized void playSound(String filename) {
        final int bufferSize = 128000;
        final AudioInputStream audioStream;
        try {
            final File soundFile = new File(filename);
            final URL url = Object.class.getResource(filename);
            audioStream = url == null
                    ? AudioSystem.getAudioInputStream(soundFile)
                    : AudioSystem.getAudioInputStream(url);
        } catch (Exception e) {
            log().error("Playback \"" + filename + "\" error", e);
            return;
        }
        final AudioFormat audioFormat = audioStream.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        final SourceDataLine sourceLine;
        try {
            sourceLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceLine.open(audioFormat);
        } catch (Exception e) {
            log().error("Playback \"" + filename + "\" error", e);
            return;
        }

        sourceLine.start();

        int nBytesRead = 0;
        byte[] abData = new byte[bufferSize];
        while (nBytesRead != -1) {
            try {
                nBytesRead = audioStream.read(abData, 0, abData.length);
            } catch (IOException e) {
                log().error("Playback \"" + filename + "\" error", e);
            }
            if (nBytesRead >= 0) {
                sourceLine.write(abData, 0, nBytesRead);
            }
        }

        sourceLine.drain();
        sourceLine.close();
    }


    private enum Position {
        LEFT, RIGHT, NORMAL
    }


    /**
     * Some new version.
     *
     * @param filename the name of the file that is going to be played.
     */
    @SuppressWarnings("squid:S3776")
    public static synchronized void playWave(String filename) {
        final int externalBufferSize = 524288; // 128Kb
        final Position curPosition = Position.NORMAL;

        final URL url = Object.class.getResource(filename);
        if (!Paths.get(filename).toFile().exists() && url == null) {
            log().error("Playback \"" + filename + "\" error. File not found.");
            return;
        }

        final AudioInputStream audioInputStream;
        try {
            audioInputStream = url == null
                    ? AudioSystem.getAudioInputStream(Paths.get(filename).toFile())
                    : AudioSystem.getAudioInputStream(url);
        } catch (Exception e) {
            log().error("Playback \"" + filename + "\" error", e);
            return;
        }

        AudioFormat format = audioInputStream.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        final SourceDataLine auline;
        try {
            auline = (SourceDataLine) AudioSystem.getLine(info);
            auline.open(format);
        } catch (Exception e) {
            log().error("Playback \"" + filename + "\" error", e);
            return;
        }

        if (auline.isControlSupported(FloatControl.Type.PAN)) {
            FloatControl pan = (FloatControl) auline
                    .getControl(FloatControl.Type.PAN);
            if (curPosition == Position.RIGHT) {
                pan.setValue(1.0f);
            } else {
                if (curPosition == Position.LEFT) {
                    pan.setValue(-1.0f);
                }
            }
        }

        auline.start();
        int nBytesRead = 0;
        byte[] abData = new byte[externalBufferSize];

        try {
            while (nBytesRead != -1) {
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
                if (nBytesRead >= 0) {
                    auline.write(abData, 0, nBytesRead);
                }
            }
        } catch (IOException e) {
            log().error("Playback \"" + filename + "\" error", e);
        } finally {
            auline.drain();
            auline.close();
        }
    }


    /**
     * Sound from lib.
     *
     * @param filename sound file.
     */
    public static synchronized void playBasic(String filename) {

        final Semaphore semaphore = new Semaphore(0);

        final BasicPlayer player = new BasicPlayer();
        player.addBasicPlayerListener(new BasicPlayerListener() {
            @Override
            public void opened(Object o, Map map) {
                // for nothing
            }

            @Override
            public void progress(int i, long l, byte[] bytes, Map map) {
                // for nothing
            }

            @Override
            public void stateUpdated(BasicPlayerEvent basicPlayerEvent) {
                if (basicPlayerEvent.getCode() == 0) {
                    log().trace("Try to play: " + basicPlayerEvent.toString());
                }
                if (basicPlayerEvent.getCode() == 3) {
                    semaphore.release();
                }
            }

            @Override
            public void setController(BasicController basicController) {
                // for nothing
            }
        });
        try {
            final URL url = Object.class.getResource(filename);
            File soundFile = new File(filename);
            if (url == null) {
                player.open(soundFile);
            } else {
                player.open(url);
            }
            player.play();
        } catch (BasicPlayerException e) {
            log().error("Playback \"" + filename + "\" error", e);
            return;
        }
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log().error("Playback \"" + filename + "\" error", e);
            Thread.currentThread().interrupt();
        }
    }
}
