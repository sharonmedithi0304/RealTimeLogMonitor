package logmonitor.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * One parsed line of a log file.
 * Serializable lets a whole analysis session be saved to disk, and
 * Comparable gives entries a natural order (by time, then level, source, message).
 */
public class LogEntry implements Serializable, Comparable<LogEntry> {
    private static final long serialVersionUID = 1L;

    /** Shared by every entry, so it is static and final. */
    public static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LocalDateTime timestamp;
    private final LogLevel level;
    private final String source;
    private final String message;

    /** Constructor used by the parsers. */
    public LogEntry(LocalDateTime timestamp, LogLevel level,
                    String source, String message) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.level = Objects.requireNonNull(level, "level");
        this.source = Objects.requireNonNull(source, "source").trim();
        this.message = Objects.requireNonNull(message, "message").trim();
    }

    /** Overloaded constructor: uses the current time. */
    public LogEntry(LogLevel level, String source, String message) {
        this(LocalDateTime.now().withNano(0), level, source, message);
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }

    public boolean isError() {
        return level == LogLevel.ERROR;
    }

    /** One CSV row; double quotes inside text are doubled as the CSV format requires. */
    public String toCsvRow() {
        return String.join(",",
                timestamp.format(FORMAT),
                level.name(),
                quote(source),
                quote(message));
    }

    private static String quote(String text) {
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    @Override
    public int compareTo(LogEntry other) {
        int result = timestamp.compareTo(other.timestamp);
        if (result == 0) {
            result = level.compareTo(other.level);
        }
        if (result == 0) {
            result = source.compareTo(other.source);
        }
        if (result == 0) {
            result = message.compareTo(other.message);
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LogEntry)) {
            return false;
        }
        return compareTo((LogEntry) obj) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, level, source, message);
    }

    /** Returns the entry in the same format as the log file. */
    @Override
    public String toString() {
        return timestamp.format(FORMAT) + " [" + level + "] " + source + " - " + message;
    }
}
