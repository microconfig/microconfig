package io.microconfig.core.properties.serializers.impl

import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale.ROOT

/** Validates file identities and file/directory conflicts before any output is written. */
internal class OutputDestinations {
    private val files = mutableSetOf<Path>()
    private val parents = mutableSetOf<Path>()
    private val caseSensitiveDirectories = mutableMapOf<Path, Boolean>()

    fun checkName(file: File, path: Path) {
        val caseSensitive = isCaseSensitive(existingDirectory(path))
        require(!isDiffName(file.name, caseSensitive) && !isDiffName(path.fileName.toString(), caseSensitive)) {
            "Reserved diff output '$file'"
        }
    }

    private fun existingDirectory(path: Path): Path {
        var directory = path.parent
        while (!Files.isDirectory(directory)) directory = directory.parent
        return directory
    }

    private fun isCaseSensitive(directory: Path): Boolean =
        try {
            caseSensitiveDirectories.getOrPut(directory) {
                val parent = directory.parent
                if (parent != null && Files.getFileStore(parent) == Files.getFileStore(directory)) {
                    val alternative = alternativeCase(directory)
                    if (alternative != null) return@getOrPut !isSameEntry(directory, alternative)
                    return@getOrPut isCaseSensitive(parent)
                }
                Files.newDirectoryStream(directory).use { entries ->
                    for (entry in entries) {
                        val alternative = alternativeCase(entry) ?: continue
                        return@getOrPut !isSameEntry(entry, alternative)
                    }
                }
                true
            }
        } catch (error: IOException) {
            throw UncheckedIOException("Can't determine output filename rules in '$directory'", error)
        }

    private fun alternativeCase(path: Path): Path? {
        val name = path.fileName?.toString() ?: return null
        val index = name.indexOfFirst { it in 'a'..'z' || it in 'A'..'Z' }
        if (index < 0) return null
        val letter = name[index]
        val changed = if (letter.isLowerCase()) letter.uppercaseChar() else letter.lowercaseChar()
        return path.resolveSibling(name.replaceRange(index, index + 1, changed.toString()))
    }

    private fun isSameEntry(path: Path, alternative: Path): Boolean =
        Files.exists(alternative) && Files.isSameFile(path, alternative)

    private fun isDiffName(name: String, caseSensitive: Boolean): Boolean {
        val compared = if (caseSensitive) name else name.lowercase(ROOT)
        return compared.startsWith("diff-") &&
                (compared.endsWith(".yaml") || compared.endsWith(".properties"))
    }

    fun reserve(file: File, path: Path) {
        checkName(file, path)
        val identities = setOf(path, comparisonPath(path))
        checkAvailable(file, identities)
        remember(identities)
    }

    fun reserveDirectory(path: Path) {
        require(Files.notExists(path) || Files.isDirectory(path)) {
            "Component output directory conflicts with an existing file '$path'"
        }
        setOf(path, comparisonPath(path)).forEach { entry ->
            parents.add(entry)
            parents.addAll(ancestors(entry))
        }
    }

    fun checkDiff(config: File, directory: Path) {
        val path = directory.resolve("diff-${config.name}")
        require(path !in parents && comparisonPath(path) !in parents) {
            "Diff file conflicts with a component output directory '$path'"
        }
    }

    private fun comparisonPath(path: Path): Path {
        if (path.parent == null) return path
        val directory = existingDirectory(path)
        if (isCaseSensitive(directory)) return path
        return directory.resolve(directory.relativize(path).toString().lowercase(ROOT))
    }

    private fun checkAvailable(file: File, identities: Set<Path>) {
        require(identities.none { it in files }) {
            "Several outputs have the same destination '$file'"
        }
        require(identities.none { it in parents || ancestors(it).any(files::contains) }) {
            "Output file conflicts with an output directory '$file'"
        }
    }

    private fun ancestors(path: Path): Sequence<Path> =
        generateSequence(path.parent) { it.parent }

    private fun remember(identities: Set<Path>) {
        files.addAll(identities)
        identities.forEach { parents.addAll(ancestors(it)) }
    }
}
