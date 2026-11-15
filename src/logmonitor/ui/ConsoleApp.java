package logmonitor.ui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import logmonitor.analysis.LogAnalyzer;
import logmonitor.analysis.LogFilter;
import logmonitor.model.LogEntry;
import logmonitor.model.LogLevel;
import logmonitor.model.LogParseException;
import logmonitor.monitor.LogGenerator;
import logmonitor.monitor.LogListener;
import logmonitor.monitor.LogMonitor;
import logmonitor.parser.AbstractLogParser;
import logmonitor.parser.RegexLogParser;
import logmonitor.util.ReportWriter;

/**
 * Text-mode version of the monitor. A Scanner reads commands from the keyboard
 * while the monitor thread prints new log lines as they arrive.
 * Here the monitor thread calls analyzer.add() while the main thread calls
 * analyzer.summaryReport(), which is why LogAnalyzer is synchronized.
 */
public class ConsoleApp {

    private final Path logFile;
    private final Path outputDir;
    private final AbstractLogParser parser;
    private final LogAnalyzer analyzer = new LogAnalyzer();

    private volatile LogFilter printFilter = LogFilter.all();
    private LogGenerator generator;
    private Thread generatorThread;

    public ConsoleApp(Path logFile, Path outputDir) {
        this(logFile, outputDir, new RegexLogParser());
    }

    public ConsoleApp(Path logFile, Path outputDir, AbstractLogParser parser) {
        this.logFile = logFile;
        this.outputDir = outputDir;
        this.parser = parser;
    }

    public void run() {
        System.out.println("=== Real-Time Log Monitor (console mode) ===");
        System.out.println("Watching: " + logFile.toAbsolutePath());
        printHelp();

        LogMonitor monitor = new LogMonitor(logFile, parser, new ConsoleListener(), 400L, 0L);
        monitor.start();

        Scanner scanner = new Scanner(System.in);
        boolean quit = false;
        while (!quit && scanner.hasNextLine()) {
            String[] words = scanner.nextLine().trim().split("\\s+");
            String command = words[0].toLowerCase();
            switch (command) {
                case "help":
                    printHelp();
                    break;
                case "stats":
                    System.out.println(analyzer.summaryReport());
                    break;
                case "level":
                    setLevel(words);
                    break;
                case "tail":
                    tail(words);
                    break;
                case "gen":
                    toggleGenerator(words);
                    break;
                case "export":
                    export();
                    break;
                case "quit":
                case "exit":
                    quit = true;
                    break;
                case "":
                    break;
                default:
                    System.out.println("Unknown command. Type 'help'.");
            }
        }
        shutdown(monitor);
        System.out.println("Bye.");
    }

    private void printHelp() {
        System.out.println("Commands: stats | level <ALL|DEBUG|INFO|WARN|ERROR> | tail <n> | "
                + "gen on | gen off | export | quit");
    }

    private void setLevel(String[] words) {
        if (words.length < 2) {
            System.out.println("Usage: level <ALL|DEBUG|INFO|WARN|ERROR>");
            return;
        }
        if (words[1].equalsIgnoreCase("ALL")) {
            printFilter = LogFilter.all();
            System.out.println("Showing all levels.");
            return;
        }
        try {
            LogLevel level = LogLevel.fromString(words[1]);
            printFilter = LogFilter.minLevel(level);
            System.out.println("Showing " + level + " and above.");
        } catch (IllegalArgumentException ex) {          // unknown level name
            System.out.println("Unknown level: " + words[1]);
        }
    }

    private void tail(String[] words) {
        int count = 5;
        if (words.length > 1) {
            try {
                count = Integer.parseInt(words[1]);
            } catch (NumberFormatException ex) {
                System.out.println("Please give a number, e.g. tail 10");
                return;
            }
        }
        List<LogEntry> latest = analyzer.latest(count);
        for (int i = latest.size() - 1; i >= 0; i--) {   // latest() is newest first
            System.out.println(latest.get(i));
        }
    }

    private void toggleGenerator(String[] words) {
        boolean on = words.length > 1 && words[1].equalsIgnoreCase("on");
        boolean off = words.length > 1 && words[1].equalsIgnoreCase("off");
        if (on && (generatorThread == null || !generatorThread.isAlive())) {
            generator = new LogGenerator(logFile, 700L);
            generatorThread = new Thread(generator, "LogGenerator-Thread");
            generatorThread.setDaemon(true);
            generatorThread.setPriority(Thread.NORM_PRIORITY - 1);
            generatorThread.start();
            System.out.println("Log generator started.");
        } else if (off && generatorThread != null) {
            generator.stop();
            generatorThread.interrupt();
            System.out.println("Log generator stopped.");
        } else {
            System.out.println("Usage: gen on | gen off");
        }
    }

    private void export() {
        try {
            Path file = ReportWriter.writeTextReport(outputDir, analyzer.summaryReport());
            System.out.println("Report saved: " + file.toAbsolutePath());
        } catch (IOException ex) {
            System.out.println("Could not save report: " + ex.getMessage());
        }
    }

    private void shutdown(LogMonitor monitor) {
        monitor.stopMonitoring();
        if (generator != null) {
            generator.stop();
            generatorThread.interrupt();
        }
    }

    /** Inner class: it can use analyzer and printFilter of the enclosing ConsoleApp. */
    private class ConsoleListener implements LogListener {
        @Override
        public void onEntry(LogEntry entry) {
            analyzer.add(entry);
            if (printFilter.matches(entry)) {
                System.out.println(entry);
            }
        }

        @Override
        public void onParseError(LogParseException error) {
            System.out.println("[invalid line skipped] " + error.getMessage()
                    + " -> " + error.getRawLine());
        }
    }
}
