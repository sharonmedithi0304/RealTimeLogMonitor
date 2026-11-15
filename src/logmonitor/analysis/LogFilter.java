package logmonitor.analysis;

import logmonitor.model.LogEntry;
import logmonitor.model.LogLevel;

/**
 * Functional interface: exactly one abstract method, so a lambda expression
 * can be used wherever a LogFilter is needed, e.g.
 *     LogFilter errorsOnly = e -> e.getLevel() == LogLevel.ERROR;
 * Default and static methods (Java 8) let filters be combined.
 */
@FunctionalInterface
public interface LogFilter {

    boolean matches(LogEntry entry);

    default LogFilter and(LogFilter other) {
        return entry -> matches(entry) && other.matches(entry);
    }

    default LogFilter or(LogFilter other) {
        return entry -> matches(entry) || other.matches(entry);
    }

    default LogFilter negate() {
        return entry -> !matches(entry);
    }

    static LogFilter all() {
        return entry -> true;
    }

    static LogFilter byLevel(LogLevel level) {
        return entry -> entry.getLevel() == level;
    }

    /** Matches entries whose severity is at least the given level. */
    static LogFilter minLevel(LogLevel level) {
        return entry -> entry.getLevel().getSeverity() >= level.getSeverity();
    }

    /** Case-insensitive search in the source name and the message. */
    static LogFilter containsText(String text) {
        final String needle = text.toLowerCase();
        return entry -> entry.getMessage().toLowerCase().contains(needle)
                || entry.getSource().toLowerCase().contains(needle);
    }
}
