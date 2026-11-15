package logmonitor.util;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Byte-stream example: copies a file with InputStream and OutputStream. */
public final class LogArchiver {

    private LogArchiver() {
    }

    public static Path archive(Path source, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        Path target = outputDir.resolve(
                "archive_" + ReportWriter.timestamp() + "_" + source.getFileName());
        byte[] buffer = new byte[4096];
        try (InputStream in = new FileInputStream(source.toFile());
             OutputStream out = new BufferedOutputStream(new FileOutputStream(target.toFile()))) {
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
        }
        return target;
    }
}
