package logmonitor.parser;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import logmonitor.model.LogEntry;
import logmonitor.model.LogLevel;
import logmonitor.model.LogParseException;

/**
 * Parses a line with a regular expression.
 * final class: it cannot be extended (preventing inheritance).
 */
public final class RegexLogParser extends AbstractLogParser {

    // group 1 = timestamp, 2 = level, 3 = source, 4 = message
    private static final Pattern PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\s+"
          + "\\[(\\w+)\\]\\s+"
          + "([\\w.\\-]+)\\s+-\\s+"
          + "(.+)$");

    public RegexLogParser() {
        super("RegexLogParser");          // super(...) calls the parent constructor
    }

    @Override
    protected LogEntry doParse(String line) throws LogParseException {
        Matcher matcher = PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new LogParseException("Line does not match the log format", line);
        }
        try {
            LocalDateTime time = LocalDateTime.parse(matcher.group(1), LogEntry.FORMAT);
            LogLevel level = LogLevel.fromString(matcher.group(2));
            return new LogEntry(time, level, matcher.group(3), matcher.group(4));
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            throw new LogParseException("Invalid date or level: " + ex.getMessage(), line, ex);
        }
    }

    @Override
    public String toString() {
        return "Regex-based " + super.toString();   // super.method() calls the parent version
    }
}
