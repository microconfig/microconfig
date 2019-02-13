package io.microconfig.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;

import static io.microconfig.utils.FileUtils.write;
import static io.microconfig.utils.Logger.error;
import static java.nio.file.Files.setPosixFilePermissions;
import static java.util.Arrays.asList;

public class FilePermissionUtils {
    public static void copyPermissions(Path from, Path to) {
        if (OsUtil.isWindows()) return;

        try {
            setPosixFilePermissions(to, Files.getPosixFilePermissions(from));
        } catch (IOException e) {
            error(String.format("Cannot copy file permissions from %s to %s", from, to), e);
        }
    }

    public static void allowExecution(Path file) {
        if (OsUtil.isWindows()) return;

        try {
            setPosixFilePermissions(file, new HashSet<>(asList(PosixFilePermission.values())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeExecutable(File file, String content) {
        write(file, content);
        allowExecution(file.toPath());
    }
}
