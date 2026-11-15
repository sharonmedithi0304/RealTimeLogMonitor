package logmonitor.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import logmonitor.model.LogEntry;

/**
 * Object serialization / deserialization of an analysis session.
 * The loaded data is validated before it is used, and an input filter
 * only allows the classes we expect to be read from the file.
 */
public final class SessionStore {

    private SessionStore() {
    }

    public static void save(List<LogEntry> entries, Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(file)))) {
            out.writeObject(new ArrayList<>(entries));
        }
    }

    public static List<LogEntry> load(Path file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(file)))) {
            in.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                    "java.util.ArrayList;java.time.*;java.lang.*;logmonitor.model.*;!*"));
            Object loaded = in.readObject();
            if (!(loaded instanceof ArrayList<?>)) {
                throw new IOException("Invalid session file");
            }
            List<LogEntry> restored = new ArrayList<>();
            for (Object item : (ArrayList<?>) loaded) {
                if (!(item instanceof LogEntry)) {
                    throw new IOException("Invalid entry in session file");
                }
                restored.add((LogEntry) item);
            }
            return restored;
        }
    }
}
