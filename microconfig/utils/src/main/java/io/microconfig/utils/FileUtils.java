package io.microconfig.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Stream;

import static io.microconfig.utils.Logger.warn;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.stream;
import static java.util.stream.Stream.of;


public class FileUtils {
    public static final String LINES_SEPARATOR = File.separator;

    public static File userHome() {
        return new File(userHomeString());
    }

    public static String userHomeString() {
        return System.getProperty("user.home");
    }

    public static void truncate(File dir) {
        delete(dir);
        createDir(dir);
    }

    public static File createDir(File dir) {
        if (dir.exists()) return dir;

        try {
            createDirectories(dir.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return dir;
    }

    public static File createFile(File file) {
        if (file.exists()) return file;

        createDir(file.getParentFile());
        try {
            Files.createFile(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }

    public static void write(File file, String content) {
        write(file.toPath(), content);
    }

    public static void write(Path file, String content, OpenOption... options) {
        try {
            createDirectories(file.getParent());
            Files.write(file, content.getBytes(), options);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void write(Path path, Collection<String> lines) {
        try {
            createDirectories(path.getParent());
            Files.write(path, lines);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void delete(File... files) {
        if (files != null) {
            stream(files).forEach(FileUtils::delete);
        }
    }

    public static void delete(File path) {
        if (!path.exists()) return;

        if (path.isFile()) {
            if (!path.delete()) {
                warn("Can't delete file " + path);
            }
        } else {
            if (!deleteDir(path)) {
                warn("Can't delete dir " + path);
            }
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            of(dir.listFiles()).forEach(FileUtils::deleteDir);
        }
        return dir.delete();
    }

    public static void copy(File from, File to) {
        copy(from.toPath(), to.toPath());
    }

    public static void copy(Path src, Path dest) {
        if (!exists(src)) {
            delete(dest.toFile());
            return;
        }

        try {
            Files.copy(src, dest, REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static File canonical(File repoDir) {
        try {
            return repoDir.getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean dirNotEmpty(File destination, int minFileCount) {
        return destination.exists() && destination.list().length >= minFileCount;
    }

    public static Stream<Path> walk(Path path) {
        try {
            return Files.walk(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}