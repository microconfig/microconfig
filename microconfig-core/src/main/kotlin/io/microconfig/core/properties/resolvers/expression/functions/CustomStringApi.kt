package io.microconfig.core.properties.resolvers.expression.functions

import java.util.Base64
import java.util.regex.Pattern

fun findGroup(regex: String, line: String): String? = findGroupOrDefault(regex, line, "")

fun findGroupOrDefault(regex: String, line: String, defaultValue: String): String? {
    val matcher = Pattern.compile(regex).matcher(line)
    return if (matcher.find()) matcher.group(minOf(1, matcher.groupCount())) else defaultValue
}

fun base64(line: String): String = bytesBase64(line.toByteArray(Charsets.UTF_8))

fun bytesBase64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

fun delete(line: String, toDelete: String): String = line.replace(toDelete, "")

fun substringAfterFirst(line: String, substring: String): String =
    substringAfter(line, line.indexOf(substring), substring)

private fun substringAfter(line: String, index: Int, substring: String): String =
    if (index < 0) "" else line.substring(index + substring.length)

fun substringAfterLast(line: String, substring: String): String =
    substringAfter(line, line.lastIndexOf(substring), substring)

fun removeLineSeparators(line: String): String = replaceLineSeparators(line, "")

fun replaceLineSeparators(line: String, replacer: String): String = lineSeparators.replace(line) { replacer }

private val lineSeparators = Regex("\\r\\n|[\\r\\n]")
