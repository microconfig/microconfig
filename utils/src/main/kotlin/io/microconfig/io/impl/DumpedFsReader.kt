package io.microconfig.io.impl

import io.microconfig.io.FsReader
import io.microconfig.utils.IoUtils
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.util.Optional
import java.util.function.Predicate

class DumpedFsReader : FsReader {
    override fun readFully(file: File): String = IoUtils.readFully(file)

    override fun readLines(file: File): List<String> = IoUtils.readLines(file)

    override fun firstLineOf(file: File, predicate: Predicate<String>): Optional<String> =
        try {
            Files.lines(file.toPath()).use { it.filter(predicate).findFirst() }
        } catch (error: IOException) {
            throw UncheckedIOException(error)
        }
}
