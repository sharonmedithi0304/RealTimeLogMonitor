package logmonitor.monitor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import logmonitor.model.LogEntry;

/**
 * Bounded producer-consumer buffer shared by two threads:
 *   producer = LogMonitor thread (calls put)
 *   consumer = dashboard timer (calls drain)
 * synchronized methods give mutual exclusion; wait() and notifyAll() make the
 * producer pause when the buffer is full until the consumer has emptied it.
 */
public class LogBuffer {

    private final LinkedList<LogEntry> queue = new LinkedList<>();
    private final int capacity;

    public LogBuffer(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void put(LogEntry entry) throws InterruptedException {
        while (queue.size() >= capacity) {
            wait();                       // releases the lock and waits for notifyAll()
        }
        queue.addLast(entry);
    }

    /** Removes and returns everything currently in the buffer (never blocks). */
    public synchronized List<LogEntry> drain() {
        List<LogEntry> batch = new ArrayList<>(queue);
        queue.clear();
        notifyAll();                      // wake a producer that was waiting for space
        return batch;
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized void clear() {
        queue.clear();
        notifyAll();
    }
}
