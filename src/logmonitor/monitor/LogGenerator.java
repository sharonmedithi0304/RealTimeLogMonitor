package logmonitor.monitor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Random;

import logmonitor.model.LogEntry;
import logmonitor.model.LogLevel;

/**
 * Simulates a live application by appending a new log line every few hundred
 * milliseconds. It runs as a second thread (implements Runnable), which makes
 * the "real-time" behaviour easy to demonstrate without a real server.
 */
public class LogGenerator implements Runnable {

    private static final String[] SOURCES = {
        "AuthService", "PaymentService", "OrderProcessor", "DatabasePool",
        "CacheManager", "ApiGateway", "FileUploader", "Scheduler"
    };
    private static final String[] DEBUG_MESSAGES = {
        "Cache lookup for key user_%d", "Query executed in %d ms",
        "Heartbeat received from node-%d", "Session token refreshed for user %d"
    };
    private static final String[] INFO_MESSAGES = {
        "User %d logged in successfully", "Order #%d created",
        "Payment of Rs. %d processed", "Scheduled job completed",
        "Request GET /api/products returned 200", "File upload finished (%d KB)",
        "Configuration reloaded"
    };
    private static final String[] WARN_MESSAGES = {
        "Response time above 2 seconds for request %d",
        "Retrying database connection (attempt %d)", "Memory usage at 80 percent",
        "Deprecated API endpoint /v1/orders called", "Disk space running low on /var/log"
    };
    private static final String[] ERROR_MESSAGES = {
        "Login failed: invalid credentials", "Database connection timed out",
        "Payment gateway returned HTTP 503", "NullPointerException in OrderProcessor.validate()",
        "Unable to write to disk: no space left", "Upstream service not responding"
    };

    private final Path file;
    private final long intervalMillis;
    private final Random random = new Random();
    private volatile boolean running = true;
    private int burstRemaining = 0;

    public LogGenerator(Path file) {
        this(file, 800L);
    }

    public LogGenerator(Path file, long intervalMillis) {
        this.file = file;
        this.intervalMillis = intervalMillis;
    }

    @Override
    public void run() {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            // append mode (true): existing lines are kept
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.toFile(), true))) {
                while (running) {
                    writer.write(nextLine());
                    writer.newLine();
                    writer.flush();                      // make the line visible to the monitor
                    Thread.sleep(intervalMillis);
                }
            }
        } catch (IOException ex) {
            System.err.println("LogGenerator I/O error: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        running = false;
    }

    /** Builds one random log line. About 3% of the time it starts a burst of errors. */
    public String nextLine() {
        LogLevel level;
        String source;
        if (burstRemaining > 0) {
            burstRemaining--;
            level = LogLevel.ERROR;
            source = "DatabasePool";
        } else if (random.nextInt(100) < 3) {
            burstRemaining = 4;
            level = LogLevel.ERROR;
            source = "DatabasePool";
        } else {
            int roll = random.nextInt(100);
            if (roll < 15) {
                level = LogLevel.DEBUG;
            } else if (roll < 70) {
                level = LogLevel.INFO;
            } else if (roll < 88) {
                level = LogLevel.WARN;
            } else {
                level = LogLevel.ERROR;
            }
            source = pick(SOURCES);
        }
        String message = fill(pick(messagesFor(level)));
        return LocalDateTime.now().format(LogEntry.FORMAT)
                + " [" + level + "] " + source + " - " + message;
    }

    private String[] messagesFor(LogLevel level) {
        switch (level) {
            case DEBUG: return DEBUG_MESSAGES;
            case INFO:  return INFO_MESSAGES;
            case WARN:  return WARN_MESSAGES;
            default:    return ERROR_MESSAGES;
        }
    }

    private String pick(String[] options) {
        return options[random.nextInt(options.length)];
    }

    private String fill(String template) {
        return template.contains("%d")
                ? String.format(template, random.nextInt(900) + 100)
                : template;
    }
}
