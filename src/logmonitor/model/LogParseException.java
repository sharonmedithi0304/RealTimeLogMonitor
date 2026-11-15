package logmonitor.model;

/**
 * Checked exception thrown when a line of the log file does not follow
 * the expected format. It keeps the raw line so the caller can report it.
 */
public class LogParseException extends Exception {
    private static final long serialVersionUID = 1L;

    private final String rawLine;

    public LogParseException(String message, String rawLine) {
        super(message);
        this.rawLine = rawLine;
    }

    public LogParseException(String message, String rawLine, Throwable cause) {
        super(message, cause);
        this.rawLine = rawLine;
    }

    public String getRawLine() {
        return rawLine;
    }
}
