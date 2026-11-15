package logmonitor.parser;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.StringTokenizer;

import logmonitor.model.LogEntry;
import logmonitor.model.LogLevel;
import logmonitor.model.LogParseException;

/**
 * Alternative parser that splits the line into tokens with StringTokenizer.
 * It is interchangeable with RegexLogParser because both are AbstractLogParser
 * (runtime polymorphism / dynamic method dispatch).
 * Note: repeated spaces inside the message are collapsed to one space.
 */
public class TokenLogParser extends AbstractLogParser {

    public TokenLogParser() {
        super("TokenLogParser");
    }

    @Override
    protected LogEntry doParse(String line) throws LogParseException {
        StringTokenizer tokens = new StringTokenizer(line, " ");
        if (tokens.countTokens() < 6) {
            throw new LogParseException("Too few fields in line", line);
        }
        String date = tokens.nextToken();
        String time = tokens.nextToken();
        String levelToken = tokens.nextToken();
        String source = tokens.nextToken();
        String dash = tokens.nextToken();

        if (!levelToken.startsWith("[") || !levelToken.endsWith("]")) {
            throw new LogParseException("Level must be written as [LEVEL]", line);
        }
        if (!"-".equals(dash)) {
            throw new LogParseException("Expected '-' after the source name", line);
        }

        StringBuilder message = new StringBuilder();
        while (tokens.hasMoreTokens()) {
            if (message.length() > 0) {
                message.append(' ');
            }
            message.append(tokens.nextToken());
        }

        try {
            LocalDateTime stamp = LocalDateTime.parse(date + " " + time, LogEntry.FORMAT);
            String levelText = levelToken.substring(1, levelToken.length() - 1);
            return new LogEntry(stamp, LogLevel.fromString(levelText), source, message.toString());
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            throw new LogParseException("Invalid date or level: " + ex.getMessage(), line, ex);
        }
    }
}
