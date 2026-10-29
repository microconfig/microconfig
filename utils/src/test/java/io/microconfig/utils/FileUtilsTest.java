package io.microconfig.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static io.microconfig.utils.FileUtils.getExtension;
import static io.microconfig.utils.FileUtils.getName;
import static java.nio.file.StandardOpenOption.APPEND;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FileUtilsTest {
    @TempDir
    Path directory;

    @Test
    void writeFileInCurrentDirectory() throws IOException {
        Path file = Path.of("file-utils-" + UUID.randomUUID() + ".txt");
        try {
            FileUtils.write(file, "content");

            assertEquals("content", Files.readString(file));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void createParentDirectoriesWhenWritingFile() throws IOException {
        Path file = directory.resolve("nested/output.txt");

        FileUtils.write(file, "content");

        assertEquals("content", Files.readString(file));
    }

    @Test
    void overwriteExistingFileByDefault() throws IOException {
        Path file = directory.resolve("output.txt");
        Files.writeString(file, "previous content");

        FileUtils.write(file, "new");

        assertEquals("new", Files.readString(file));
    }

    @Test
    void respectAppendOptionWhenWritingFile() throws IOException {
        Path file = directory.resolve("output.txt");
        Files.writeString(file, "previous");

        FileUtils.write(file, " appended", APPEND);

        assertEquals("previous appended", Files.readString(file));
    }

    @Test
    void nameAndExtension() {
        File f1 = new File("file.yaml");
        File f2 = new File("file.file2.yaml");
        File f3 = new File("file");

        assertEquals("file", getName(f1));
        assertEquals("file.file2", getName(f2));
        assertEquals("file", getName(f3));

        assertEquals(".yaml", getExtension(f1));
        assertEquals(".yaml", getExtension(f2));
        assertEquals("", getExtension(f3));
    }
}
