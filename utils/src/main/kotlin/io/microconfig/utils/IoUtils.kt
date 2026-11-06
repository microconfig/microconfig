package io.microconfig.utils

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.UncheckedIOException

object IoUtils {
    fun readFully(file: File): String = readFile { readFully(file.inputStream()) }

    fun readAllBytes(file: File): ByteArray = readFile { readAllBytes(file.inputStream()) }

    fun readClasspathResource(file: String): String =
        readFully(
            requireNotNull(IoUtils::class.java.classLoader.getResourceAsStream(file)) {
                "Missing classpath resource '$file'"
            }
        )

    fun readFully(input: InputStream): String = readAllBytes(input).toString(Charsets.UTF_8)

    fun readAllBytes(input: InputStream): ByteArray = readFile { input.use { it.readBytes() } }

    fun readLines(file: File): List<String> = readFile {
        if (file.exists()) file.readLines(Charsets.UTF_8) else emptyList()
    }

    private inline fun <T> readFile(read: () -> T): T =
        try {
            read()
        } catch (error: IOException) {
            throw UncheckedIOException(error)
        }
}
