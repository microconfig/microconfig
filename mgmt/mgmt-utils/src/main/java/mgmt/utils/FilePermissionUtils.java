package mgmt.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static io.microconfig.utils.FileUtils.write;
import static io.microconfig.utils.Logger.error;
import static java.nio.file.Files.setPosixFilePermissions;
import static mgmt.utils.OsUtil.isWindows;

public class FilePermissionUtils {
    public static void copyPermissions(Path from, Path to) {
        if (isWindows()) return;

        try {
            setPosixFilePermissions(to, Files.getPosixFilePermissions(from));
        } catch (IOException e) {
            error(String.format("Cannot copy file permissions from %s to %s", from, to), e);
        }
    }

    public static void allowExecution(Path file) {
        if (isWindows()) return;

        try {
            setPosixFilePermissions(file, Set.of(PosixFilePermission.values()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeExecutable(File file, String content) {
        write(file, content);
        allowExecution(file.toPath());
    }
}
