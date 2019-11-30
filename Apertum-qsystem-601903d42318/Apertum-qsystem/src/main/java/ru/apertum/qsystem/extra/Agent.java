package ru.apertum.qsystem.extra;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;
import ru.apertum.qsystem.common.QLog;

/**
 * Загрузка плагинов в classPath. Тут учет jdk8 и jdk9+.
 */
public class Agent {

    private Agent() {
        // сука сонар!
    }

    private static Instrumentation inst = null;

    /**
     * The JRE will call method before launching your main().
     *
     * @param str  Какая-о строка.
     * @param inst Что-то инструментальное для загрузки jars.
     */
    public static void agentmain(final String str, final Instrumentation inst) {
        QLog.l().logger().trace("String {}, Instrumentation {}", str, inst);
        Agent.inst = inst;
    }

    /**
     * За грузим файл в classPath.
     *
     * @param file этот файл грузим.
     * @return false если произошло исключение при загрузке, иначе true.
     */
    public static boolean addClassPath(File file) {
        final ClassLoader sysloader = ClassLoader.getSystemClassLoader();
        try {
            if (sysloader instanceof URLClassLoader) {
                // If Java 8 or below fallback to old method
                final Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                m.setAccessible(true);//NOSONAR
                m.invoke(sysloader, file.toURI().toURL());
            } else {
                // If Java 9 or higher use Instrumentation
                if (inst == null) {
                    QLog.l().logger().error("Instrumentation was not loaded.");
                } else {
                    inst.appendToSystemClassLoaderSearch(new JarFile(file));
                }
            }
        } catch (Exception ex) {
            QLog.l().logger().error("Plugin " + file.getName() + " was NOT loaded.", ex);
            return false;
        }
        return true;
    }
}
