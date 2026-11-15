package logmonitor.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

/**
 * Asynchronous I/O (Java NIO.2): the write is started and the calling thread
 * continues immediately. A CompletionHandler is called when the write finishes.
 */
public final class AsyncExporter {

    private AsyncExporter() {
    }

    public static void writeAsync(Path target, String content, Consumer<String> onFinish)
            throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(target,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
        ByteBuffer buffer = ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));

        channel.write(buffer, 0, 0L, new CompletionHandler<Integer, Long>() {
            @Override
            public void completed(Integer written, Long position) {
                long next = position + written;
                if (buffer.hasRemaining()) {          // write the rest if needed
                    channel.write(buffer, next, next, this);
                } else {
                    close(channel);
                    onFinish.accept("Export complete: " + target.getFileName()
                            + " (" + next + " bytes)");
                }
            }

            @Override
            public void failed(Throwable error, Long position) {
                close(channel);
                onFinish.accept("Export failed: " + error.getMessage());
            }
        });
    }

    private static void close(AsynchronousFileChannel channel) {
        try {
            channel.close();
        } catch (IOException ignored) {
            // nothing more can be done
        }
    }
}
