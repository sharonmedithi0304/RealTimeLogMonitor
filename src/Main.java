import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import logmonitor.parser.AbstractLogParser;
import logmonitor.parser.RegexLogParser;
import logmonitor.parser.TokenLogParser;
import logmonitor.ui.ConsoleApp;
import logmonitor.ui.Dashboard;

/**
 * Entry point. Arguments are optional and can be given in any order:
 *   java -cp out Main                  -> Swing dashboard, watching logs/application.log
 *   java -cp out Main console          -> text mode instead of the dashboard
 *   java -cp out Main token            -> use the StringTokenizer parser instead of regex
 *   java -cp out Main path/to/app.log  -> watch a different log file
 */
public class Main {

    public static void main(String[] args) {
        boolean consoleMode = false;
        boolean useTokenParser = false;
        String logName = Paths.get("logs", "application.log").toString();

        for (String arg : args) {
            if (arg.equalsIgnoreCase("console")) {
                consoleMode = true;
            } else if (arg.equalsIgnoreCase("gui")) {
                consoleMode = false;
            } else if (arg.equalsIgnoreCase("token")) {
                useTokenParser = true;
            } else {
                logName = arg;
            }
        }
        final Path logFile = Paths.get(logName);
        final Path outputDir = Paths.get("output");

        try {
            prepareFolders(logFile, outputDir);
        } catch (IOException ex) {
            System.err.println("Could not prepare the log and output folders: " + ex.getMessage());
            return;
        }

        // The parent type is used, so either parser can be plugged in (polymorphism).
        final AbstractLogParser parser = useTokenParser ? new TokenLogParser() : new RegexLogParser();

        if (consoleMode) {
            new ConsoleApp(logFile, outputDir, parser).run();
        } else {
            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ex) {
                    // keep the default look and feel
                }
                new Dashboard(logFile, outputDir, parser).setVisible(true);
            });
        }
    }

    private static void prepareFolders(Path logFile, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        Path parent = logFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(logFile)) {
            Files.createFile(logFile);
        }
    }
}
