package logmonitor.monitor;

import logmonitor.model.LogEntry;
import logmonitor.model.LogParseException;

/**
 * Callback interface (observer pattern). The monitor thread calls these
 * methods; the dashboard or the console application provides the behaviour.
 */
public interface LogListener {

    void onEntry(LogEntry entry);

    /** Default methods mean an implementer only overrides what it needs. */
    default void onParseError(LogParseException error) {
    }

    default void onStatusChange(String status) {
    }
}
