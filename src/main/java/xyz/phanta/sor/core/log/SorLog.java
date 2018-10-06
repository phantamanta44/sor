package xyz.phanta.sor.core.log;

import java.time.Instant;

public class SorLog {

    private static LogLevel currentLevel = LogLevel.INFO;

    public static void setLevel(LogLevel level) {
        currentLevel = level;
    }

    public static void debug(String format, Object... arguments) {
        print(format, arguments, LogLevel.DEBUG);
    }

    public static void info(String format, Object... arguments) {
        print(format, arguments, LogLevel.INFO);
    }

    public static void warn(String format, Object... arguments) {
        print(format, arguments, LogLevel.WARN);
    }

    public static void error(String format, Object... arguments) {
        print(format, arguments, LogLevel.ERROR);
    }

    private static void print(String msg, Object[] args, LogLevel level) {
        if (level.canPrintIn(currentLevel)) {
            Thread thread = Thread.currentThread();
            String className = thread.getStackTrace()[3].getClassName();
            System.out.printf(Instant.now() + " " // timestamp
                    + level.name() + " : " // log level
                    + thread.getName() + "/" // thread name
                    + className.substring(className.lastIndexOf(".") + 1) + " -- " // class name
                    + msg + "\n", args); // message
        }
    }

}
