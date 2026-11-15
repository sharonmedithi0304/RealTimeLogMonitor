package logmonitor.model;

/**
 * Severity levels used in the log files.
 * An enum is a special class with a fixed set of constants.
 */
public enum LogLevel {
    DEBUG(1), INFO(2), WARN(3), ERROR(4);

    private final int severity;

    LogLevel(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }

    /** Converts text such as "error" or "WARNING" to a LogLevel. */
    public static LogLevel fromString(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Level is null");
        }
        String cleaned = text.trim().toUpperCase();
        if (cleaned.equals("WARNING")) {
            cleaned = "WARN";
        }
        return LogLevel.valueOf(cleaned);   // throws IllegalArgumentException if unknown
    }
}
