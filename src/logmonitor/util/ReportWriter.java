package logmonitor.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import logmonitor.model.LogEntry;

/**
 * Character-stream file handling: FileWriter/BufferedWriter to write reports,
 * FileReader/BufferedReader to read them back, and java.nio.file to list them.
 */
public final class ReportWriter {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ReportWriter() {
    }

    public static String timestamp() {
        return LocalDateTime.now().format(STAMP);
    }

    /** Writes the report text to output/report_yyyyMMdd_HHmmss.txt and returns its path. */
    public static Path writeTextReport(Path outputDir, String content) throws IOException {
        Files.createDirectories(outputDir);
        Path target = outputDir.resolve("report_" + timestamp() + ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(target.toFile()))) {
            writer.write(content);
            writer.newLine();
        }
        return target;
    }

    /** Reads a text file line by line. */
    public static String readText(Path file) throws IOException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
        }
        return text.toString();
    }

    /** Lists saved reports, newest first. */
    public static List<Path> listReports(Path outputDir) throws IOException {
        if (!Files.isDirectory(outputDir)) {
            return new ArrayList<>();
        }
        try (Stream<Path> files = Files.list(outputDir)) {
            return files
                    .filter(p -> p.getFileName().toString().startsWith("report_"))
                    .filter(p -> p.getFileName().toString().endsWith(".txt"))
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());
        }
    }

    /** Builds the CSV text for a list of entries (header row first). */
    public static String toCsv(List<LogEntry> entries) {
        StringBuilder csv = new StringBuilder("timestamp,level,source,message\n");
        for (LogEntry entry : entries) {
            csv.append(entry.toCsvRow()).append('\n');
        }
        return csv.toString();
    }
}
