package io.microconfig.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

public class IoUtils {
    public static String readFullyOrEmpty(File file) {
        return file.exists() ? readFully(file) : "";
    }

    public static String readFully(File file) {
        try {
            return readFully(new FileInputStream(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readFully(InputStream is) {
        try (InputStream input = is) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024 * 4];
            int n;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }

            return output.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String firstLineOrEmpty(File file) {
        String line = firstLine(file);
        return line == null ? "" : line;
    }

    public static String firstLine(File file) {
        if (!file.exists()) return null;

        try (Stream<String> strings = lines(file.toPath())) {
            return strings.findFirst()
                    .map(String::trim)
                    .orElse("");
        }
    }

    public static Stream<String> lines(Path path) {
        try {
            return Files.lines(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> readLines(File file) {
        if (!file.exists()) return emptyList();

        try {
            return Files.readAllLines(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}