package ru.apertum.qsystem;


import org.apache.logging.log4j.core.lookup.MainMapLookup;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.QModule;
import ru.apertum.qsystem.server.ChangeContext;

import java.io.File;
import java.nio.file.Files;

import static com.google.common.io.Files.copy;

public class SuiteListenerForDb implements ISuiteListener {

    /**
     * Файл настроек с описаниями подключения к БД. Это вообще-то константа.
     */
    private static final String FILE_PATH = "config/asfb_UT.dat";

    @Override
    public void onStart(ISuite suite) {
        final String[] args = {"-ide", "-np"};
        QLog.initial(args, QModule.unknown);
        MainMapLookup.setMainArguments(args);
        try {
            Files.deleteIfExists(new File("db/QSystemUT.mv.db").toPath());
            copy(new File("installation/resource/bin/QSystemDB_UT.mv.db"), new File("db/QSystemUT.mv.db"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ChangeContext.setFilePath(FILE_PATH);
    }

    @Override
    public void onFinish(ISuite suite) {
        ChangeContext.setFilePath(null);
    }
}
