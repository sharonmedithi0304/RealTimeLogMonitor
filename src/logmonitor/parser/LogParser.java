package logmonitor.parser;

import logmonitor.model.LogEntry;
import logmonitor.model.LogParseException;

/**
 * Contract for anything that can turn one line of text into a LogEntry.
 * Expected line format:
 *   2026-11-15 17:08:01 [ERROR] AuthService - Login failed for user admin
 */
public interface LogParser {

    LogEntry parse(String line) throws LogParseException;

    String getName();

    /** Default method (Java 8): lets every parser check a line without handling exceptions. */
    default boolean isValid(String line) {
        try {
            parse(line);
            return true;
        } catch (LogParseException ex) {
            return false;
        }
    }
}
