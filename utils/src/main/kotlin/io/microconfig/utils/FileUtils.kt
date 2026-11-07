package io.microconfig.utils

import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Stream

object FileUtils {
    @JvmField
    val LINES_SEPARATOR: String = System.lineSeparator()

    fun copyPermissions(from: Path, to: Path) {
        if (Os.isWindows()) return
        try {
            Files.setPosixFilePermissions(to, Files.getPosixFilePermissions(from))
        } catch (error: IOException) {
            Logger.error("Cannot copy file permissions from $from to $to", error)
        }
    }

    fun write(file: File, content: String) = write(file.toPath(), content)

    fun write(file: Path, content: String, vararg options: OpenOption) =
        write(file, content.toByteArray(Charsets.UTF_8), *options)

    fun write(file: Path, content: ByteArray, vararg options: OpenOption) {
        try {
            val target = normalizedPath(file)
            createParentDirectories(target)
            Files.write(target, content, *options)
        } catch (error: IOException) {
            throw UncheckedIOException(error)
        }
    }

    private fun normalizedPath(file: Path): Path =
        if (file.toFile().absolutePath.contains("..")) file.toFile().canonicalFile.toPath()
        else file

    private fun createParentDirectories(file: Path) {
        file.parent?.let { Files.createDirectories(it) }
    }

    fun delete(vararg paths: File) = paths.forEach(::delete)

    fun delete(path: File) {
        try {
            deleteTree(path.toPath())
        } catch (error: IOException) {
            Logger.warn("Can't delete $path: ${error.message}")
        }
    }

    private fun deleteTree(path: Path) {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return
        Files.walkFileTree(
            path,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(
                    file: Path,
                    attributes: BasicFileAttributes,
                ): FileVisitResult {
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(
                    directory: Path,
                    error: IOException?,
                ): FileVisitResult {
                    if (error != null) throw error
                    Files.delete(directory)
                    return FileVisitResult.CONTINUE
                }
            },
        )
    }

    fun walk(path: Path): Stream<Path> =
        try {
            Files.walk(path)
        } catch (error: IOException) {
            throw UncheckedIOException(error)
        }
}
