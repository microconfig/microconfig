package io.microconfig.utils;

import lombok.RequiredArgsConstructor;

import java.io.*;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.Collections.emptyList;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class IoUtils {
    public static String readFully(File file) {
        try {
            return readFully(new FileInputStream(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] readAllBytes(File file) {
        try {
            return readAllBytes(new FileInputStream(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readClasspathResource(String file) {
        return readFully(IoUtils.class.getClassLoader().getResourceAsStream(file));
    }

    public static String readFully(InputStream is) {
        return doRead(is).toString();
    }

    public static byte[] readAllBytes(InputStream is) {
        return doRead(is).toByteArray();
    }

    private static ByteArrayOutputStream doRead(InputStream is) {
        try (InputStream input = is) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024 * 4];
            int n;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }

            return output;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> readLines(File file) {
        if (!file.exists()) return emptyList();

        try {
            return readLinesUTF8(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> readLinesUTF8(Path file) throws IOException {
        try {
            return Files.readAllLines(file);
        } catch (MalformedInputException e) {
            return readLinesISO(file);
        }

    }

    private static List<String> readLinesISO(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.ISO_8859_1);
    }
}