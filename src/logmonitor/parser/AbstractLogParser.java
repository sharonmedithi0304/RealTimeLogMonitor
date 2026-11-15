package logmonitor.parser;

import logmonitor.model.LogEntry;
import logmonitor.model.LogParseException;

/**
 * Template for the parsers. The common work (empty-line check and counting)
 * lives here; each subclass only supplies doParse().
 */
public abstract class AbstractLogParser implements LogParser {

    private final String name;
    private int totalLines;
    private int parsedLines;
    private int failedLines;

    protected AbstractLogParser(String name) {
        this.name = name;
    }

    /** final: subclasses must not change the counting logic. */
    @Override
    public final LogEntry parse(String line) throws LogParseException {
        try {
            if (line == null || line.trim().isEmpty()) {
                throw new LogParseException("Empty line", line);
            }
            LogEntry entry = doParse(line.trim());
            record(true);
            return entry;
        } catch (LogParseException ex) {
            record(false);
            throw ex;
        } finally {
            countLine();
        }
    }

    /** Each concrete parser implements its own technique. */
    protected abstract LogEntry doParse(String line) throws LogParseException;

    // synchronized: the monitor thread writes these counters while the UI thread reads them.
    private synchronized void record(boolean success) {
        if (success) {
            parsedLines++;
        } else {
            failedLines++;
        }
    }

    private synchronized void countLine() {
        totalLines++;
    }

    public synchronized int getTotalLines() {
        return totalLines;
    }

    public synchronized int getParsedLines() {
        return parsedLines;
    }

    public synchronized int getFailedLines() {
        return failedLines;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + " (parsed=" + getParsedLines() + ", invalid=" + getFailedLines() + ")";
    }
}
