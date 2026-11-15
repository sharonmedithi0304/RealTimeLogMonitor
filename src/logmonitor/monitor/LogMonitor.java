package logmonitor.monitor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;

import logmonitor.model.LogEntry;
import logmonitor.model.LogParseException;
import logmonitor.parser.LogParser;

/**
 * Background thread that "tails" a log file: it remembers how far it has read
 * and, every poll interval, reads only the bytes added since then.
 * Reading uses NIO: a FileChannel and a ByteBuffer.
 *
 * Thread life cycle: NEW (constructed) -> RUNNABLE (start()) -> TIMED_WAITING
 * (between polls) -> TERMINATED (after stopMonitoring()).
 */
public class LogMonitor extends Thread {

    private static final int MAX_CHUNK = 1 << 20;        // read at most 1 MB per poll

    private final Path file;
    private final LogParser parser;
    private final LogListener listener;
    private final long pollMillis;
    private final Object lock = new Object();
    private final AtomicLong linesRead = new AtomicLong();

    private volatile boolean running = true;
    private volatile long position;                      // next byte to read

    public LogMonitor(Path file, LogParser parser, LogListener listener) {
        this(file, parser, listener, 500L, 0L);
    }

    /**
     * @param startPosition byte offset to start from; 0 = read the whole file,
     *                      a negative value = start at the current end (new lines only)
     */
    public LogMonitor(Path file, LogParser parser, LogListener listener,
                      long pollMillis, long startPosition) {
        super("LogMonitor-Thread");                      // name shown in debuggers
        this.file = file;
        this.parser = parser;
        this.listener = listener;
        this.pollMillis = pollMillis;
        this.position = startPosition;
        setDaemon(true);                                 // does not keep the JVM alive
        setPriority(Thread.NORM_PRIORITY + 1);           // a hint to the scheduler
    }

    @Override
    public void run() {
        listener.onStatusChange("Monitoring started: " + file);
        try {
            waitForFile();
            if (!running) {
                return;
            }
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
                if (position < 0) {
                    position = channel.size();
                }
                while (running) {
                    readNewData(channel);
                    pause();
                }
            }
        } catch (IOException ex) {
            listener.onStatusChange("I/O error: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();          // keep the interrupt status
        } finally {
            listener.onStatusChange("Monitoring stopped");
        }
    }

    /** Reads whatever was appended since the last poll. */
    private void readNewData(FileChannel channel) throws IOException {
        long size = channel.size();
        if (size < position) {                           // file was truncated or replaced
            position = 0;
            listener.onStatusChange("Log file was reset - reading from the start");
        }
        long available = size - position;
        if (available <= 0) {
            return;
        }

        ByteBuffer buffer = ByteBuffer.allocate((int) Math.min(available, MAX_CHUNK));
        int bytesRead = channel.read(buffer, position);  // positional read
        if (bytesRead <= 0) {
            return;
        }
        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        // Only complete lines are processed; a half-written last line waits for the next poll.
        int lastNewline = -1;
        for (int i = data.length - 1; i >= 0; i--) {
            if (data[i] == '\n') {
                lastNewline = i;
                break;
            }
        }
        int usable = lastNewline >= 0 ? lastNewline + 1
                : (data.length == MAX_CHUNK ? data.length : 0);
        if (usable == 0) {
            return;
        }

        String text = new String(data, 0, usable, StandardCharsets.UTF_8);
        position += usable;
        for (String line : text.split("\\r?\\n")) {
            processLine(line);
        }
    }

    private void processLine(String line) {
        if (line.trim().isEmpty()) {
            return;
        }
        linesRead.incrementAndGet();
        try {
            LogEntry entry = parser.parse(line);
            listener.onEntry(entry);
        } catch (LogParseException ex) {
            listener.onParseError(ex);
        }
    }

    /** Sleeps for one poll interval, but wakes at once if stopMonitoring() is called. */
    private void pause() throws InterruptedException {
        synchronized (lock) {
            if (running) {
                lock.wait(pollMillis);
            }
        }
    }

    private void waitForFile() throws InterruptedException {
        while (running && !Files.exists(file)) {
            pause();
        }
    }

    public void stopMonitoring() {
        synchronized (lock) {
            running = false;
            lock.notifyAll();
        }
    }

    public long getPosition() {
        return position;
    }

    public long getLinesRead() {
        return linesRead.get();
    }
}
