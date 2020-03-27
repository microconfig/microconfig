package io.microconfig.utils;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

import static java.util.Collections.emptyList;

public class IoUtils {
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

    public static List<String> readLines(File file) {
        if (!file.exists()) return emptyList();

        try {
            return Files.readAllLines(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}