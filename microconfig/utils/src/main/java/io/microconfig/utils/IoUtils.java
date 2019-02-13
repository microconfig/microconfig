package io.microconfig.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.ObjIntConsumer;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

public class IoUtils {
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

    public static String readNullableFirstLine(File file) {
        String line = readFirstLine(file);
        return line == null ? "" : line;
    }

    public static String readFirstLine(File file) {
        if (!file.exists()) return null;

        try (Stream<String> strings = lines(file.toPath())) {
            return strings.findFirst().map(String::trim).orElse("");
        }
    }

    public static String safeRead(File file) {
        return !file.exists() ? "" : readFully(file);
    }

    public static String readFully(File file) {
        try {
            return readFully(new FileInputStream(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readFully(InputStream is) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream input = is) {
            doCopy(input, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return output.toString();
    }

    private static void doCopy(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
    }

    public static void readChunked(InputStream inputStream, ObjIntConsumer<byte[]> consumer) {
        byte[] bytes = new byte[1024 * 50];

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

    public static List<String> readAllLines(File file) {
        if (!file.exists()) return emptyList();

        try {
            return Files.readAllLines(file.toPath());
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

    public static Stream<Path> walk(Path path) {
        try {
            return Files.walk(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Stream<String> lines(Path path) {
        try {
            return Files.lines(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}