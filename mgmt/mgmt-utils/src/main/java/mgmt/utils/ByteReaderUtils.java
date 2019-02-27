package mgmt.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.function.ObjIntConsumer;

public class ByteReaderUtils {
    public static long copyWithFlush(InputStream input, OutputStream output) {
        long count = 0;
        int n;
        byte[] buffer = new byte[10 * 1024];
        try {
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                output.flush();
                count += n;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return count;
    }

    public static void readChunked(InputStream inputStream, ObjIntConsumer<byte[]> consumer) {
        byte[] bytes = new byte[50 * 1024];

        try {
            while (true) {
                int read = inputStream.read(bytes);
                if (read <= 0) break;

                consumer.accept(bytes, read);
                if (read < bytes.length) break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] readAllBytes(File file) {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
