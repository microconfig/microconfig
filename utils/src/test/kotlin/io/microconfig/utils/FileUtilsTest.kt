package io.microconfig.utils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.APPEND
import java.nio.file.attribute.PosixFilePermissions
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FileUtilsTest {
    @TempDir lateinit var directory: Path

    @Test
    fun writeFileInCurrentDirectory() {
        val file: Path = Path.of("file-utils-" + UUID.randomUUID() + ".txt")
        try {
            FileUtils.write(file, "content")
            assertEquals("content", Files.readString(file))
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun createParentDirectoriesWhenWritingFile() {
        val file: Path = directory.resolve("nested/output.txt")
        FileUtils.write(file, "content")
        assertEquals("content", Files.readString(file))
    }

    @Test
    fun overwriteExistingFileByDefault() {
        val file: Path = directory.resolve("output.txt")
        Files.writeString(file, "previous content")
        FileUtils.write(file, "new")
        assertEquals("new", Files.readString(file))
    }

    @Test
    fun respectAppendOptionWhenWritingFile() {
        val file: Path = directory.resolve("output.txt")
        Files.writeString(file, "previous")
        FileUtils.write(file, " appended", APPEND)
        assertEquals("previous appended", Files.readString(file))
    }

    @Test
    fun copyPermissionsWhenTheDestinationTraversesAMissingDirectory() {
        assumeTrue(Files.getFileStore(directory).supportsFileAttributeView("posix"))
        val source = Files.writeString(directory.resolve("source"), "content")
        val permissions = PosixFilePermissions.fromString("rwx------")
        Files.setPosixFilePermissions(source, permissions)
        val destination = directory.resolve("missing/../target")

        FileUtils.writeWithPermissions(destination, Files.readAllBytes(source), source)

        assertEquals(permissions, Files.getPosixFilePermissions(directory.resolve("target")))
        assertFalse(Files.exists(directory.resolve("missing")))
    }
}
