package logmonitor.analysis;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import logmonitor.model.LogEntry;
import logmonitor.model.LogLevel;
import logmonitor.util.Counter;
import logmonitor.util.ListUtils;

/**
 * Keeps every entry received and the statistics calculated from them.
 * Several collection types are used, each for the job it suits best:
 *   ArrayList  - all entries in arrival order
 *   LinkedList - sliding window of the most recent entries (fast add/remove at the ends)
 *   HashSet    - unique source names
 *   TreeSet    - ERROR entries kept sorted by time (uses Comparable)
 *   HashMap/TreeMap (inside Counter) - counts per level, source and error message
 * All public methods are synchronized, so the monitor thread can add entries
 * while another thread reads the statistics.
 */
public class LogAnalyzer {

    public static final int DEFAULT_WINDOW = 10;
    public static final int DEFAULT_ALERT_THRESHOLD = 4;

    private final ArrayList<LogEntry> entries = new ArrayList<>();
    private final LinkedList<LogEntry> recent = new LinkedList<>();
    private final HashSet<String> uniqueSources = new HashSet<>();
    private final TreeSet<LogEntry> errorTimeline = new TreeSet<>();
    private final Counter<LogLevel> levelCounter = new Counter<>();
    private final Counter<String> sourceCounter = new Counter<>();
    private final Counter<String> errorMessageCounter = new Counter<>();

    private final int window;
    private final int alertThreshold;

    public LogAnalyzer() {
        this(DEFAULT_WINDOW, DEFAULT_ALERT_THRESHOLD);
    }

    public LogAnalyzer(int window, int alertThreshold) {
        this.window = window;
        this.alertThreshold = alertThreshold;
    }

    public synchronized void add(LogEntry entry) {
        entries.add(entry);
        levelCounter.increment(entry.getLevel());
        sourceCounter.increment(entry.getSource());
        uniqueSources.add(entry.getSource());

        recent.addLast(entry);
        if (recent.size() > window) {
            recent.removeFirst();
        }
        if (entry.isError()) {
            errorTimeline.add(entry);
            errorMessageCounter.increment(entry.getMessage());
        }
    }

    public synchronized void addAll(Collection<LogEntry> newEntries) {
        for (LogEntry entry : newEntries) {
            add(entry);
        }
    }

    public synchronized void clear() {
        entries.clear();
        recent.clear();
        uniqueSources.clear();
        errorTimeline.clear();
        levelCounter.clear();
        sourceCounter.clear();
        errorMessageCounter.clear();
    }

    public synchronized int size() {
        return entries.size();
    }

    /** A copy of all entries, safe to use while new entries keep arriving. */
    public synchronized List<LogEntry> snapshot() {
        return new ArrayList<>(entries);
    }

    /** Returns the entries accepted by the filter (stream + method reference). */
    public synchronized List<LogEntry> filter(LogFilter filter) {
        return entries.stream()
                .filter(filter::matches)
                .collect(Collectors.toList());
    }

    /** The newest n entries, newest first. */
    public synchronized List<LogEntry> latest(int n) {
        return ListUtils.lastN(entries, n);
    }

    public synchronized List<LogEntry> sortedBySeverity() {
        List<LogEntry> copy = new ArrayList<>(entries);
        copy.sort(Comparator.comparing(LogEntry::getLevel).reversed()
                .thenComparing(Comparator.naturalOrder()));
        return copy;
    }

    public synchronized int count(LogLevel level) {
        return levelCounter.get(level);
    }

    public synchronized int uniqueSourceCount() {
        return uniqueSources.size();
    }

    public synchronized TreeMap<String, Integer> sourceCounts() {
        return sourceCounter.sortedByKey();
    }

    public synchronized List<Map.Entry<String, Integer>> topErrorMessages(int n) {
        return errorMessageCounter.topN(n);
    }

    public synchronized List<Map.Entry<String, Integer>> topSources(int n) {
        return sourceCounter.topN(n);
    }

    /** Percentage of entries that are ERROR. */
    public synchronized double errorRate() {
        if (entries.isEmpty()) {
            return 0.0;
        }
        return 100.0 * levelCounter.get(LogLevel.ERROR) / entries.size();
    }

    /** True when the sliding window holds at least alertThreshold ERROR entries. */
    public synchronized boolean hasErrorBurst() {
        int errors = 0;
        Iterator<LogEntry> iterator = recent.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isError()) {
                errors++;
            }
        }
        return errors >= alertThreshold;
    }

    /** The newest n errors, taken from the sorted TreeSet in descending order. */
    public synchronized List<LogEntry> latestErrors(int n) {
        List<LogEntry> result = new ArrayList<>();
        Iterator<LogEntry> iterator = errorTimeline.descendingIterator();
        while (iterator.hasNext() && result.size() < n) {
            result.add(iterator.next());
        }
        return result;
    }

    public synchronized String summaryReport() {
        StringBuilder report = new StringBuilder();
        report.append("REAL-TIME LOG MONITOR - ANALYSIS REPORT\n");
        report.append("Generated      : ")
              .append(LocalDateTime.now().format(LogEntry.FORMAT)).append('\n');
        report.append("------------------------------------------\n");

        if (entries.isEmpty()) {
            report.append("No log entries received yet.\n");
            return report.toString();
        }

        LocalDateTime first = entries.stream()
                .map(LogEntry::getTimestamp).min(Comparator.naturalOrder()).get();
        LocalDateTime last = entries.stream()
                .map(LogEntry::getTimestamp).max(Comparator.naturalOrder()).get();

        report.append("Period         : ").append(first.format(LogEntry.FORMAT))
              .append("  to  ").append(last.format(LogEntry.FORMAT)).append('\n');
        report.append("Total entries  : ").append(entries.size()).append('\n');
        report.append("Unique sources : ").append(uniqueSources.size()).append('\n');
        report.append(String.format(Locale.US, "Error rate     : %.2f %%%n", errorRate()));
        report.append("Alert status   : ")
              .append(hasErrorBurst() ? "ERROR BURST DETECTED" : "Normal").append('\n');

        report.append("\nENTRIES PER LEVEL\n");
        for (LogLevel level : LogLevel.values()) {
            int count = levelCounter.get(level);
            double percent = 100.0 * count / entries.size();
            report.append(String.format(Locale.US, "  %-6s %5d  (%5.1f %%)%n",
                    level, count, percent));
        }

        report.append("\nENTRIES PER SOURCE (A-Z)\n");
        for (Map.Entry<String, Integer> e : sourceCounter.sortedByKey().entrySet()) {
            report.append(String.format(Locale.US, "  %-18s %5d%n", e.getKey(), e.getValue()));
        }

        report.append("\nTOP 5 ERROR MESSAGES\n");
        List<Map.Entry<String, Integer>> top = errorMessageCounter.topN(5);
        if (top.isEmpty()) {
            report.append("  (no errors)\n");
        }
        int rank = 1;
        for (Map.Entry<String, Integer> e : top) {
            report.append(String.format(Locale.US, "  %d. %s  (%d times)%n",
                    rank++, e.getKey(), e.getValue()));
        }

        report.append("\nLATEST 5 ERRORS\n");
        List<LogEntry> latest = latestErrors(5);
        if (latest.isEmpty()) {
            report.append("  (no errors)\n");
        }
        for (LogEntry e : latest) {
            report.append("  ").append(e).append('\n');
        }
        return report.toString();
    }
}
