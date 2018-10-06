package xyz.phanta.sor.core.log;

public enum LogLevel {

    DEBUG,
    INFO,
    WARN,
    ERROR;

    public boolean canPrintIn(LogLevel level) {
        return level.ordinal() <= ordinal();
    }

}
