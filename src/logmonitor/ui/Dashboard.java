package logmonitor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import logmonitor.analysis.LogAnalyzer;
import logmonitor.analysis.LogFilter;
import logmonitor.model.LogEntry;
import logmonitor.model.LogLevel;
import logmonitor.monitor.LogBuffer;
import logmonitor.monitor.LogGenerator;
import logmonitor.monitor.LogListener;
import logmonitor.monitor.LogMonitor;
import logmonitor.parser.AbstractLogParser;
import logmonitor.parser.RegexLogParser;
import logmonitor.util.AsyncExporter;
import logmonitor.util.LogArchiver;
import logmonitor.util.ReportWriter;
import logmonitor.util.SessionStore;

/**
 * Main window of the application (Swing).
 *
 * Data flow:
 *   log file --(LogMonitor thread)--> LogBuffer --(Swing timer, every 300 ms)--> LogAnalyzer + table
 * The monitor thread never touches Swing components; the timer runs on the
 * event-dispatch thread, so every UI update is thread-safe.
 */
public class Dashboard extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final int MAX_ROWS = 1000;            // oldest rows are dropped after this
    private static final int BUFFER_CAPACITY = 5000;

    private final Path logFile;
    private final Path outputDir;

    private final AbstractLogParser parser;              // polymorphism: any AbstractLogParser works
    private final LogAnalyzer analyzer = new LogAnalyzer();
    private final LogBuffer buffer = new LogBuffer(BUFFER_CAPACITY);

    private LogMonitor monitor;
    private long resumePosition = 0;                     // where a restarted monitor continues
    private LogGenerator generator;
    private Thread generatorThread;

    private final DefaultTableModel tableModel =
            new DefaultTableModel(new String[] {"Time", "Level", "Source", "Message"}, 0) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isCellEditable(int row, int column) {   // anonymous inner class
                    return false;
                }
            };
    private final JTable table = new JTable(tableModel);

    private JLabel totalLabel;
    private JLabel debugLabel;
    private JLabel infoLabel;
    private JLabel warnLabel;
    private JLabel errorLabel;
    private JLabel alertLabel;
    private JLabel statusLabel;
    private JLabel threadLabel;
    private JComboBox<String> levelCombo;
    private JTextField searchField;
    private JCheckBox autoScroll;
    private JTextArea statsArea;
    private JButton monitorButton;
    private JButton generatorButton;

    private int tick = 0;
    private final javax.swing.Timer refreshTimer = new javax.swing.Timer(300, e -> refresh());

    /** Constructor overloading: the regex parser is the default. */
    public Dashboard(Path logFile, Path outputDir) {
        this(logFile, outputDir, new RegexLogParser());
    }

    public Dashboard(Path logFile, Path outputDir, AbstractLogParser parser) {
        super("Real-Time Log Monitor | Java Swing Dashboard");
        this.logFile = logFile;
        this.outputDir = outputDir;
        this.parser = parser;

        buildUI();

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                shutdown();
            }
        });

        setSize(1250, 740);
        setLocationRelativeTo(null);
        refreshTimer.start();
        startMonitoring();
    }

    // ------------------------------------------------------------------ UI construction

    private static final Color BG = new Color(11, 17, 29);
    private static final Color PANEL = new Color(18, 28, 45);
    private static final Color PANEL_ALT = new Color(22, 35, 55);
    private static final Color LINE = new Color(42, 59, 82);
    private static final Color TEXT = new Color(229, 237, 247);
    private static final Color MUTED = new Color(145, 164, 188);
    private static final Color CYAN = new Color(56, 189, 248);

    private void buildUI() {
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(12, 12));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(16, 16, 12, 16));

        JLabel eyebrow = new JLabel("SYSTEM OBSERVABILITY  /  LIVE TELEMETRY");
        eyebrow.setForeground(CYAN);
        eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 11));

        JLabel title = new JLabel("Log intelligence dashboard");
        title.setForeground(TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));

        JLabel subtitle = new JLabel("Real-time file monitoring, severity analysis and event diagnostics");
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel heading = new JPanel(new BorderLayout(0, 4));
        heading.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(3, 1, 0, 1));
        titles.setOpaque(false);
        titles.add(eyebrow);
        titles.add(title);
        titles.add(subtitle);
        heading.add(titles, BorderLayout.WEST);

        JPanel counters = new JPanel(new GridLayout(1, 6, 10, 0));
        counters.setOpaque(false);
        totalLabel = addCounter(counters, "TOTAL EVENTS", new Color(226, 232, 240));
        debugLabel = addCounter(counters, "DEBUG", new Color(148, 163, 184));
        infoLabel = addCounter(counters, "INFO", new Color(56, 189, 248));
        warnLabel = addCounter(counters, "WARN", new Color(251, 191, 36));
        errorLabel = addCounter(counters, "ERROR", new Color(248, 113, 113));
        alertLabel = addCounter(counters, "SYSTEM STATUS", new Color(74, 222, 128));
        alertLabel.setText("NORMAL");
        alertLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JPanel north = new JPanel(new BorderLayout(0, 14));
        north.setOpaque(false);
        north.add(heading, BorderLayout.NORTH);
        north.add(counters, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLogPanel(), buildStatsPanel());
        split.setResizeWeight(0.68);
        split.setDividerLocation(900);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setOpaque(false);
        split.setDividerSize(8);
        add(split, BorderLayout.CENTER);

        add(buildBottomPanel(), BorderLayout.SOUTH);
        setMinimumSize(new java.awt.Dimension(1120, 720));
    }

    private JLabel addCounter(JPanel parent, String title, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE, 1), new EmptyBorder(12, 14, 12, 14)));
        JLabel caption = new JLabel(title);
        caption.setForeground(MUTED);
        caption.setFont(new Font("Segoe UI", Font.BOLD, 10));
        JLabel value = new JLabel("0");
        value.setForeground(color);
        value.setFont(new Font("Segoe UI", Font.BOLD, 25));
        card.add(caption, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        parent.add(card);
        return value;
    }

    private JPanel titledPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE, 1), new EmptyBorder(13, 13, 13, 13)));
        JLabel heading = new JLabel(title);
        heading.setForeground(TEXT);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(heading, BorderLayout.NORTH);
        return panel;
    }

    private void styleButton(JButton button, boolean primary) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setForeground(primary ? new Color(7, 19, 32) : TEXT);
        button.setBackground(primary ? CYAN : PANEL_ALT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? CYAN : LINE), new EmptyBorder(8, 13, 8, 13)));
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    private JPanel buildLogPanel() {
        JPanel panel = titledPanel("LIVE EVENT STREAM");
        levelCombo = new JComboBox<>(new String[] {"ALL", "DEBUG", "INFO", "WARN", "ERROR", "WARN+"});
        levelCombo.setToolTipText("WARN+ shows WARN and ERROR");
        levelCombo.addActionListener(e -> rebuildTable());
        styleInput(levelCombo);

        searchField = new JTextField(17);
        searchField.addActionListener(e -> rebuildTable());
        styleInput(searchField);

        JButton applyButton = new JButton("Apply filters");
        styleButton(applyButton, true);
        applyButton.addActionListener(e -> rebuildTable());
        JButton resetButton = new JButton("Reset");
        styleButton(resetButton, false);
        resetButton.addActionListener(e -> {
            searchField.setText("");
            levelCombo.setSelectedIndex(0);
            rebuildTable();
        });
        autoScroll = new JCheckBox("Auto-scroll", true);
        autoScroll.setForeground(MUTED);
        autoScroll.setBackground(PANEL);
        autoScroll.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        filterBar.setOpaque(false);
        JLabel levelLabel = new JLabel("SEVERITY");
        levelLabel.setForeground(MUTED);
        levelLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        JLabel searchLabel = new JLabel("SEARCH");
        searchLabel.setForeground(MUTED);
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        filterBar.add(levelLabel);
        filterBar.add(levelCombo);
        filterBar.add(searchLabel);
        filterBar.add(searchField);
        filterBar.add(applyButton);
        filterBar.add(resetButton);
        filterBar.add(autoScroll);

        table.setDefaultRenderer(Object.class, new LevelCellRenderer());
        table.setRowHeight(29);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setGridColor(LINE);
        table.setBackground(PANEL);
        table.setForeground(TEXT);
        table.setSelectionBackground(new Color(26, 75, 105));
        table.setSelectionForeground(Color.WHITE);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(PANEL_ALT);
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 10));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINE));
        table.getColumnModel().getColumn(0).setPreferredWidth(145);
        table.getColumnModel().getColumn(1).setPreferredWidth(75);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(420);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(PANEL);
        scroll.setBorder(BorderFactory.createLineBorder(LINE));
        panel.add(filterBar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void styleInput(javax.swing.JComponent component) {
        component.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        component.setForeground(TEXT);
        component.setBackground(PANEL_ALT);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(6, 8, 6, 8)));
    }

    private JPanel buildStatsPanel() {
        JPanel panel = titledPanel("LIVE ANALYTICS");
        statsArea = new JTextArea();
        statsArea.setEditable(false);
        statsArea.setLineWrap(false);
        statsArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        statsArea.setForeground(new Color(190, 211, 232));
        statsArea.setBackground(new Color(9, 15, 26));
        statsArea.setBorder(new EmptyBorder(12, 12, 12, 12));
        JScrollPane scroll = new JScrollPane(statsArea);
        scroll.setBorder(BorderFactory.createLineBorder(LINE));
        scroll.getViewport().setBackground(new Color(9, 15, 26));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBottomPanel() {
        monitorButton = new JButton("Stop monitoring");
        styleButton(monitorButton, true);
        monitorButton.addActionListener(e -> toggleMonitoring());
        generatorButton = new JButton("Start log generator");
        styleButton(generatorButton, false);
        generatorButton.addActionListener(e -> toggleGenerator());

        JButton clearButton = new JButton("Clear view");
        styleButton(clearButton, false);
        clearButton.addActionListener(e -> clearAll());
        JButton reportButton = new JButton("Export report");
        styleButton(reportButton, false);
        reportButton.addActionListener(e -> exportReport());
        JButton csvButton = new JButton("Export CSV");
        styleButton(csvButton, false);
        csvButton.addActionListener(e -> exportCsv());
        JButton saveButton = new JButton("Save session");
        styleButton(saveButton, false);
        saveButton.addActionListener(e -> saveSession());
        JButton loadButton = new JButton("Load session");
        styleButton(loadButton, false);
        loadButton.addActionListener(e -> loadSession());
        JButton historyButton = new JButton("Report history");
        styleButton(historyButton, false);
        historyButton.addActionListener(e -> viewReports());
        JButton archiveButton = new JButton("Archive log");
        styleButton(archiveButton, false);
        archiveButton.addActionListener(e -> archiveLog());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setOpaque(false);
        actions.add(monitorButton);
        actions.add(generatorButton);
        actions.add(clearButton);
        actions.add(reportButton);
        actions.add(csvButton);
        actions.add(saveButton);
        actions.add(loadButton);
        actions.add(historyButton);
        actions.add(archiveButton);

        statusLabel = new JLabel("  INITIALIZING MONITOR");
        statusLabel.setForeground(new Color(125, 211, 252));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        threadLabel = new JLabel(" ");
        threadLabel.setForeground(MUTED);
        threadLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setOpaque(false);
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, LINE));
        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(threadLabel, BorderLayout.EAST);

        JPanel bottom = new JPanel(new BorderLayout(0, 7));
        bottom.setOpaque(false);
        bottom.add(actions, BorderLayout.CENTER);
        bottom.add(statusBar, BorderLayout.SOUTH);
        return bottom;
    }

    // ------------------------------------------------------------------ threads

    private void toggleMonitoring() {
        if (monitor != null && monitor.isAlive()) {
            stopMonitoring();
        } else {
            startMonitoring();
        }
    }

    private void startMonitoring() {
        monitor = new LogMonitor(logFile, parser, new DashboardListener(), 400L, resumePosition);
        monitor.start();
        monitorButton.setText("Stop Monitoring");
    }

    private void stopMonitoring() {
        if (monitor != null) {
            monitor.stopMonitoring();
            try {
                monitor.join(1000);                      // wait for the thread to finish
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            resumePosition = monitor.getPosition();      // a restart continues from here
        }
        monitorButton.setText("Start Monitoring");
    }

    private void toggleGenerator() {
        if (generatorThread != null && generatorThread.isAlive()) {
            generator.stop();
            generatorThread.interrupt();
            generatorButton.setText("Start Log Generator");
            setStatus("Log generator stopped");
        } else {
            generator = new LogGenerator(logFile, 700L);
            generatorThread = new Thread(generator, "LogGenerator-Thread");
            generatorThread.setDaemon(true);
            generatorThread.setPriority(Thread.NORM_PRIORITY - 1);
            generatorThread.start();
            generatorButton.setText("Stop Log Generator");
            setStatus("Log generator started - appending to " + logFile);
        }
    }

    private void shutdown() {
        refreshTimer.stop();
        if (monitor != null) {
            monitor.stopMonitoring();
        }
        if (generator != null) {
            generator.stop();
            generatorThread.interrupt();
        }
        dispose();
        System.exit(0);
    }

    // ------------------------------------------------------------------ refreshing the view

    /** Runs on the event-dispatch thread every 300 ms. */
    private void refresh() {
        List<LogEntry> batch = buffer.drain();
        if (!batch.isEmpty()) {
            LogFilter filter = currentFilter();
            for (LogEntry entry : batch) {
                analyzer.add(entry);
                if (filter.matches(entry)) {
                    appendRow(entry);
                }
            }
            while (tableModel.getRowCount() > MAX_ROWS) {
                tableModel.removeRow(0);
            }
            if (autoScroll.isSelected()) {
                scrollToBottom();
            }
        }
        updateCounters();
        tick++;
        if (tick % 3 == 0) {                              // about once per second
            updateStats();
        }
        updateThreadLabel();
    }

    private void updateCounters() {
        totalLabel.setText(String.valueOf(analyzer.size()));
        debugLabel.setText(String.valueOf(analyzer.count(LogLevel.DEBUG)));
        infoLabel.setText(String.valueOf(analyzer.count(LogLevel.INFO)));
        warnLabel.setText(String.valueOf(analyzer.count(LogLevel.WARN)));
        errorLabel.setText(String.valueOf(analyzer.count(LogLevel.ERROR)));
        if (analyzer.hasErrorBurst()) {
            alertLabel.setText("ERROR BURST");
            alertLabel.setForeground(new Color(190, 20, 20));
        } else {
            alertLabel.setText("NORMAL");
            alertLabel.setForeground(new Color(0, 130, 60));
        }
    }

    private void updateStats() {
        statsArea.setText(analyzer.summaryReport()
                + "\nInvalid lines skipped : " + parser.getFailedLines()
                + "\nParser in use         : " + parser + "\n");
        statsArea.setCaretPosition(0);
    }

    private void updateThreadLabel() {
        String monitorState = (monitor == null) ? "NOT STARTED" : monitor.getState().toString();
        String generatorState = (generatorThread != null && generatorThread.isAlive()) ? "RUNNING" : "OFF";
        threadLabel.setText("Monitor thread: " + monitorState
                + "   |   Generator: " + generatorState
                + "   |   Lines processed: " + parser.getTotalLines()
                + "   |   Invalid: " + parser.getFailedLines() + " ");
    }

    private void appendRow(LogEntry entry) {
        tableModel.addRow(new Object[] {
            entry.getTimestamp().format(LogEntry.FORMAT),
            entry.getLevel().name(),
            entry.getSource(),
            entry.getMessage()
        });
    }

    private void scrollToBottom() {
        int last = table.getRowCount() - 1;
        if (last >= 0) {
            table.scrollRectToVisible(table.getCellRect(last, 0, true));
        }
    }

    /** Builds a filter from the controls by combining lambda filters with and(). */
    private LogFilter currentFilter() {
        LogFilter filter = LogFilter.all();
        String selected = (String) levelCombo.getSelectedItem();
        if ("WARN+".equals(selected)) {
            filter = filter.and(LogFilter.minLevel(LogLevel.WARN));
        } else if (selected != null && !"ALL".equals(selected)) {
            filter = filter.and(LogFilter.byLevel(LogLevel.fromString(selected)));
        }
        String text = searchField.getText().trim();
        if (!text.isEmpty()) {
            filter = filter.and(LogFilter.containsText(text));
        }
        return filter;
    }

    private void rebuildTable() {
        List<LogEntry> matches = analyzer.filter(currentFilter());
        int start = Math.max(0, matches.size() - MAX_ROWS);
        tableModel.setRowCount(0);
        for (LogEntry entry : matches.subList(start, matches.size())) {
            appendRow(entry);
        }
        if (autoScroll.isSelected()) {
            scrollToBottom();
        }
        setStatus("Showing " + matches.size() + " matching entries");
    }

    private void clearAll() {
        buffer.clear();
        analyzer.clear();
        tableModel.setRowCount(0);
        updateCounters();
        updateStats();
        setStatus("Cleared. Monitoring continues with new lines.");
    }

    private void setStatus(String text) {
        statusLabel.setText(" " + text);
    }

    // ------------------------------------------------------------------ file operations

    private void exportReport() {
        if (analyzer.size() == 0) {
            showInfo("There are no log entries to export yet.");
            return;
        }
        try {
            Path file = ReportWriter.writeTextReport(outputDir, analyzer.summaryReport());
            setStatus("Report saved: " + file);
            JOptionPane.showMessageDialog(this, "Report saved to:\n" + file.toAbsolutePath(),
                    "Report Exported", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showError("Unable to save the report", ex);
        }
    }

    private void exportCsv() {
        if (analyzer.size() == 0) {
            showInfo("There are no log entries to export yet.");
            return;
        }
        Path file = outputDir.resolve("logs_" + ReportWriter.timestamp() + ".csv");
        try {
            AsyncExporter.writeAsync(file, ReportWriter.toCsv(analyzer.snapshot()),
                    message -> SwingUtilities.invokeLater(() -> setStatus(message)));
            setStatus("Exporting CSV in the background...");
        } catch (IOException ex) {
            showError("Unable to start the CSV export", ex);
        }
    }

    private void saveSession() {
        Path file = outputDir.resolve("session.dat");
        try {
            SessionStore.save(analyzer.snapshot(), file);
            setStatus("Session saved: " + analyzer.size() + " entries -> " + file);
        } catch (IOException ex) {
            showError("Unable to save the session", ex);
        }
    }

    private void loadSession() {
        Path file = outputDir.resolve("session.dat");
        if (!Files.exists(file)) {
            showInfo("No saved session found. Use Save Session first.");
            return;
        }
        try {
            List<LogEntry> loaded = SessionStore.load(file);
            buffer.clear();
            analyzer.clear();
            analyzer.addAll(loaded);
            rebuildTable();
            updateCounters();
            updateStats();
            setStatus("Session restored: " + loaded.size() + " entries");
        } catch (IOException | ClassNotFoundException ex) {
            showError("Unable to load the session", ex);
        }
    }

    private void viewReports() {
        try {
            List<Path> reports = ReportWriter.listReports(outputDir);
            if (reports.isEmpty()) {
                showInfo("No saved reports yet. Use Export Report first.");
                return;
            }
            String[] names = new String[reports.size()];
            for (int i = 0; i < names.length; i++) {
                names[i] = reports.get(i).getFileName().toString();
            }
            Object choice = JOptionPane.showInputDialog(this, "Select a report to open:",
                    "Report History", JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
            if (choice == null) {
                return;
            }
            Path chosen = outputDir.resolve(choice.toString());
            JTextArea area = new JTextArea(ReportWriter.readText(chosen), 24, 64);
            area.setEditable(false);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            JOptionPane.showMessageDialog(this, new JScrollPane(area),
                    chosen.getFileName().toString(), JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showError("Unable to open the report", ex);
        }
    }

    private void archiveLog() {
        try {
            Path copy = LogArchiver.archive(logFile, outputDir);
            setStatus("Log archived to " + copy);
        } catch (IOException ex) {
            showError("Unable to archive the log file", ex);
        }
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message, Exception ex) {
        JOptionPane.showMessageDialog(this, message + ":\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ------------------------------------------------------------------ inner classes

    /** Receives events on the monitor thread and hands entries to the shared buffer. */
    private class DashboardListener implements LogListener {
        @Override
        public void onEntry(LogEntry entry) {
            try {
                buffer.put(entry);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void onStatusChange(String status) {
            SwingUtilities.invokeLater(() -> setStatus(status));   // UI work goes to the EDT
        }
    }

    /** Colours each row by log level. */
    private class LevelCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value,
                boolean selected, boolean focused, int row, int column) {
            Component cell = super.getTableCellRendererComponent(
                    tbl, value, selected, focused, row, column);
            if (!selected) {
                String level = String.valueOf(tableModel.getValueAt(row, 1));
                cell.setForeground(TEXT);
                cell.setBackground(row % 2 == 0 ? PANEL : PANEL_ALT);
                if (column == 1) {
                    switch (level) {
                        case "ERROR": cell.setForeground(new Color(248, 113, 113)); break;
                        case "WARN": cell.setForeground(new Color(251, 191, 36)); break;
                        case "INFO": cell.setForeground(new Color(56, 189, 248)); break;
                        case "DEBUG": cell.setForeground(MUTED); break;
                        default: cell.setForeground(TEXT);
                    }
                }
            }
            return cell;
        }
    }
}
