package io.microconfig.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.stream.Stream;

import static io.microconfig.utils.Logger.error;
import static io.microconfig.utils.Logger.warn;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.setPosixFilePermissions;
import static java.util.Arrays.stream;
import static java.util.stream.Stream.of;


public class FileUtils {
    public static final String LINES_SEPARATOR = System.getProperty("line.separator");

    public static void copyPermissions(Path from, Path to) {
        if (Os.isWindows()) return;

        try {
            setPosixFilePermissions(to, Files.getPosixFilePermissions(from));
        } catch (IOException e) {
            error(String.format("Cannot copy file permissions from %s to %s", from, to), e);
        }
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

    public static void delete(File... paths) {
        stream(paths).forEach(FileUtils::delete);
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

    public static File canonical(File repoDir) {
        try {
            return repoDir.getCanonicalFile();
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

    public static String getExtension(File file) {
        int extIndex = extensionIndex(file);
        return extIndex < 0 ? "" : file.getName().substring(extIndex);

    }

    public static String getName(File file) {
        int extIndex = extensionIndex(file);
        return extIndex < 0 ? file.getName() : file.getName().substring(0, extIndex);
    }

    private static int extensionIndex(File file) {
        return file.getName().lastIndexOf('.');
    }
}