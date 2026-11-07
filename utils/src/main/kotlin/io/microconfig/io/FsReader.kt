package io.microconfig.io

import java.io.File
import java.util.*
import java.util.function.Predicate

interface FsReader {
    fun readFully(file: File): String

    fun readLines(file: File): List<String>

    fun firstLineOf(file: File, predicate: Predicate<String>): Optional<String>
}
