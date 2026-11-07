package io.microconfig.core.properties.io.yaml.impl

import io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders
import java.io.StringReader
import java.util.IdentityHashMap
import org.yaml.snakeyaml.DumperOptions.ScalarStyle
import org.yaml.snakeyaml.error.Mark
import org.yaml.snakeyaml.error.MarkedYAMLException
import org.yaml.snakeyaml.events.AliasEvent
import org.yaml.snakeyaml.events.CommentEvent
import org.yaml.snakeyaml.events.MappingEndEvent
import org.yaml.snakeyaml.events.MappingStartEvent
import org.yaml.snakeyaml.events.ScalarEvent
import org.yaml.snakeyaml.events.SequenceEndEvent
import org.yaml.snakeyaml.events.SequenceStartEvent
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.SequenceNode
import org.yaml.snakeyaml.nodes.Tag

/** Adapts Microconfig statements to YAML while retaining offsets in the original file. */
internal class YamlSource(val original: String) {
    private val prepared = StringBuilder(original)
    private val offsets = (0..original.length).toMutableList()
    private val statements = linkedMapOf<String, MaskedStatement>()
    private val markersByStatement = mutableMapOf<String, String>()
    private val splices = mutableSetOf<Int>()
    private val possibleContinuations = linkedMapOf<Int, Int>()
    private var markerIndex = 0
    private val varMarker = nextMarker()
    private val events by lazy {
        SnakeYaml.parser(true).parse(StringReader(prepared.toString())).toList()
    }
    private val explicitTags by lazy {
        events
            .filterIsInstance<ScalarEvent>()
            .filter { it.tag != null }
            .map { it.startMark.index }
            .toSet()
    }
    private val lines by lazy { original.split(lineBreak) }

    init {
        maskStatements()
        adaptOneLineVars()
    }

    private fun maskStatements() {
        var position = 0
        while (position + 1 < original.length) {
            if (
                (original[position] == '$' || original[position] == '#') &&
                    original[position + 1] == '{'
            ) {
                val end = statementEnd(position)
                if (end > position) {
                    mask(position, end)
                    position = end
                    continue
                }
            }
            position++
        }
    }

    private fun statementEnd(start: Int): Int {
        if (original[start] == '$') {
            val placeholder = PlaceholderBorders.findPlaceholderIn(original, start).orElse(null)
            if (placeholder?.startIndex == start) return placeholder.endIndex
        }
        var depth = 1
        var quote: Char? = null
        var position = start + 2
        while (position < original.length) {
            val character = original[position]
            if (quote != null) {
                if (character == quote) {
                    if (position + 1 < original.length && original[position + 1] == quote)
                        position++
                    else quote = null
                }
            } else
                when (character) {
                    '\'',
                    '"' -> if (original[start] == '#') quote = character
                    '{' -> depth++
                    '}' -> if (--depth == 0) return position + 1
                }
            position++
        }
        return -1
    }

    private fun mask(start: Int, end: Int) {
        val statement = original.substring(start, end)
        val marker =
            markersByStatement.getOrPut(statement) {
                nextMarker().also {
                    statements[it] = MaskedStatement(statement, statementPattern(it, statement))
                }
            }
        prepared.setCharAt(start, marker[0])
        prepared.setCharAt(start + 1, marker[1])
        var indentation = false
        for (index in start + 2 until end) {
            val character = prepared[index]
            if (character in "\n\r\u0085\u2028\u2029") indentation = true
            else if (!indentation || character != ' ' && character != '\t') {
                prepared.setCharAt(index, 'x')
                indentation = false
            }
        }
    }

    private fun statementPattern(marker: String, statement: String): Regex =
        Regex(
            statement
                .split(lineBreak)
                .mapIndexed { index, line ->
                    Regex.escape(
                        if (index == 0) marker + "x".repeat(line.length - 2)
                        else "x".repeat(line.trimStart(' ', '\t').length)
                    )
                }
                .joinToString("\\s+")
        )

    private fun nextMarker(): String {
        val alphabetSize = 0xF8FF - 0xE000 + 1
        while (markerIndex < alphabetSize * alphabetSize) {
            val index = markerIndex++
            val marker =
                "${(0xE000 + index / alphabetSize).toChar()}${(0xE000 + index % alphabetSize).toChar()}"
            if (marker !in original) return marker
        }
        error("Too many Microconfig statements")
    }

    private fun adaptOneLineVars() {
        val declarations = oneLineVar.findAll(original).toList()
        if (declarations.isEmpty()) return
        val continuations = scalarContinuationLines()
        val lineEnds = lineBreak.findAll(original).map { it.range.last }.toList()
        var line = 0
        declarations.forEach { match ->
            while (line < lineEnds.size && lineEnds[line] < match.range.first) line++
            if (line in continuations) return@forEach
            val start = match.range.last - 4
            if (prepared.substring(start, start + 5) != ".var ") return@forEach
            val lineEnd =
                prepared.indexOfAny(lineBreakCharacters, start).let {
                    if (it < 0) prepared.length else it
                }
            if (
                start > match.range.first &&
                    !mappingSeparator.containsMatchIn(prepared.substring(start, lineEnd))
            )
                possibleContinuations[start] = line
            prepared.setCharAt(start, '#')
            prepared.setCharAt(start + 1, varMarker[0])
            prepared.setCharAt(start + 2, varMarker[1])
        }
    }

    private fun scalarContinuationLines(text: CharSequence = prepared): Set<Int> {
        val protected = mutableSetOf<Int>()
        val preparedLines = text.split(lineBreak)
        val scopes = mutableListOf<MappingPosition?>()
        try {
            SnakeYaml.parser().parse(StringReader(text.toString())).forEach { event ->
                when (event) {
                    is MappingStartEvent -> {
                        consumePosition(scopes.lastOrNull())
                        scopes.add(MappingPosition())
                    }
                    is SequenceStartEvent -> {
                        consumePosition(scopes.lastOrNull())
                        scopes.add(null)
                    }
                    is MappingEndEvent,
                    is SequenceEndEvent -> scopes.removeAt(scopes.lastIndex)
                    is AliasEvent -> consumePosition(scopes.lastOrNull())
                    is ScalarEvent -> {
                        val position = scopes.lastOrNull()
                        if (position?.expectsKey == true) {
                            position.key = event
                            position.expectsKey = false
                        } else {
                            if (scopes.isNotEmpty() && !isVariableScope(scopes))
                                protectScalarLines(event, preparedLines, protected)
                            consumePosition(position)
                        }
                    }
                }
            }
        } catch (_: MarkedYAMLException) {
            // List insertions are adapted during composition, after this initial event scan.
        }
        return protected
    }

    private fun consumePosition(position: MappingPosition?) {
        if (position == null) return
        if (position.expectsKey) position.key = null
        position.expectsKey = !position.expectsKey
    }

    private fun isVariableScope(scopes: List<MappingPosition?>): Boolean {
        val key = scopes.lastOrNull()?.key ?: return false
        if (key.scalarStyle != ScalarStyle.PLAIN) return false
        if (scopes.size == 1) return key.value == ".var" || key.value.startsWith('+')
        return scopes.size == 2 &&
            key.value == ".var" &&
            scopes.first()?.key?.let {
                it.scalarStyle == ScalarStyle.PLAIN && it.value.startsWith('+')
            } == true
    }

    private fun protectScalarLines(
        event: ScalarEvent,
        lines: List<String>,
        protected: MutableSet<Int>,
    ) {
        if (event.scalarStyle != ScalarStyle.PLAIN) return
        val firstLine = lines[event.startMark.line]
        val startsOnOwnLine =
            event.startMark.column == firstLine.takeWhile { it == ' ' || it == '\t' }.length
        val first = event.startMark.line + if (startsOnOwnLine) 0 else 1
        val endLine = lines[event.endMark.line]
        val endColumn = endLine.offsetByCodePoints(0, event.endMark.column)
        val trailing = endLine.substring(endColumn).trimStart()
        val last = event.endMark.line - if (trailing.isEmpty() || trailing.startsWith('#')) 0 else 1
        protected.addAll(first..last)
    }

    fun compose(): Node? {
        while (true) {
            restorePlainContinuations()
            try {
                val node = SnakeYaml.parser().compose(StringReader(prepared.toString()))
                if (
                    node != null &&
                        adaptFoldedSplice(
                            node,
                            java.util.Collections.newSetFromMap(IdentityHashMap()),
                        )
                )
                    continue
                return if (node != null && (splices.isNotEmpty() || explicitTags.isNotEmpty()))
                    prepareStatements(node, IdentityHashMap())
                else node
            } catch (error: MarkedYAMLException) {
                if (
                    !adaptListSplice(error.problemMark) &&
                        !adaptListSplice(error.contextMark) &&
                        !adaptPreviousSplice(error.problemMark)
                )
                    throw error
            }
        }
    }

    private fun restorePlainContinuations() {
        possibleContinuations.toMap().forEach { (originalStart, line) ->
            val start = offsets.indexOf(originalStart)
            val candidate = StringBuilder(prepared).replace(start, start + 3, ".va")
            if (line in scalarContinuationLines(candidate)) {
                prepared.replace(start, start + 3, ".va")
                possibleContinuations.remove(originalStart)
            }
        }
    }

    private fun adaptFoldedSplice(node: Node, visited: MutableSet<Node>): Boolean {
        if (!visited.add(node)) return false
        return when (node) {
            is MappingNode ->
                node.flowStyle == org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK &&
                    node.value.any { adaptFoldedSplice(it.valueNode, visited) }
            is SequenceNode ->
                node.flowStyle == org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK &&
                    node.value.any { adaptFoldedSplice(it, visited) }
            is ScalarNode -> adaptFoldedSplice(node)
            else -> false
        }
    }

    private fun adaptFoldedSplice(node: ScalarNode): Boolean {
        if (node.scalarStyle != ScalarStyle.PLAIN || node.startMark.line == node.endMark.line)
            return false
        val preparedLines = prepared.split(lineBreak)
        val indentation = " ".repeat(node.startMark.column)
        val following =
            preparedLines.subList(
                node.startMark.line + 1,
                (node.endMark.line + 1).coerceAtMost(preparedLines.size),
            )
        if (
            following.none {
                it.startsWith("$indentation- ") ||
                    it == "$indentation-" ||
                    it.takeWhile(Char::isWhitespace) == indentation && standaloneMarker(it) != null
            }
        )
            return false
        return adaptListSplice(node.startMark)
    }

    private fun prepareStatements(node: Node, preparedNodes: MutableMap<Node, Node>): Node {
        preparedNodes[node]?.let {
            return it
        }
        preparedNodes[node] = node
        when (node) {
            is ScalarNode -> {
                if (originalIndex(node.startMark) in splices) node.tag = spliceTag
                if (
                    node.tag == Tag.STR &&
                        node.scalarStyle == ScalarStyle.PLAIN &&
                        hasExplicitTag(node) &&
                        node.value.windowedSequence(2).any(statements::containsKey)
                ) {
                    val quoted =
                        ScalarNode(
                            Tag.STR,
                            node.value,
                            node.startMark,
                            node.endMark,
                            ScalarStyle.DOUBLE_QUOTED,
                        )
                    quoted.anchor = node.anchor
                    preparedNodes[node] = quoted
                    return quoted
                }
            }
            is SequenceNode -> node.value.replaceAll { prepareStatements(it, preparedNodes) }
            is MappingNode ->
                node.value.replaceAll {
                    NodeTuple(
                        prepareStatements(it.keyNode, preparedNodes),
                        prepareStatements(it.valueNode, preparedNodes),
                    )
                }
        }
        return node
    }

    private fun originalIndex(mark: Mark): Int = offsets[prepared.offsetByCodePoints(0, mark.index)]

    private fun adaptListSplice(mark: Mark?): Boolean {
        if (mark == null) return false
        val position = prepared.offsetByCodePoints(0, mark.index)
        return adaptSpliceAt(position)
    }

    private fun adaptSpliceAt(position: Int): Boolean {
        val lineStart =
            prepared.lastIndexOfAny(lineBreakCharacters, (position - 1).coerceAtLeast(0)) + 1
        val lineEnd =
            prepared.indexOfAny(lineBreakCharacters, lineStart).let {
                if (it < 0) prepared.length else it
            }
        val line = prepared.substring(lineStart, lineEnd)
        val marker = standaloneMarker(line) ?: return false
        val start = lineStart + line.indexOf(marker)
        splices.add(offsets[start])
        prepared.insert(start, "- ")
        offsets.add(start, offsets[start])
        offsets.add(start, offsets[start])
        return true
    }

    private fun standaloneMarker(line: String): String? {
        val value = line.trim()
        val marker = value.take(2).takeIf { it in statements } ?: return null
        val tail = value.drop(2).dropWhile { it == 'x' }.trimStart()
        return marker.takeIf { tail.isEmpty() || tail.startsWith("#") }
    }

    private fun adaptPreviousSplice(mark: Mark?): Boolean {
        if (mark == null || mark.line == 0) return false
        val currentLines = prepared.split(lineBreak)
        val previous =
            (mark.line - 1 downTo 0).firstOrNull {
                currentLines[it].isNotBlank() && !currentLines[it].trimStart().startsWith("#")
            } ?: return false
        if (
            currentLines[previous].takeWhile(Char::isWhitespace) !=
                currentLines[mark.line].takeWhile(Char::isWhitespace)
        )
            return false
        if (standaloneMarker(currentLines[previous]) == null) return false
        val start =
            if (previous == 0) 0
            else lineBreak.findAll(prepared).elementAt(previous - 1).range.last + 1
        return adaptSpliceAt(start)
    }

    fun restore(value: String): String {
        val text = value.replace("#$varMarker", ".va")
        val output = StringBuilder()
        var copied = 0
        markerPattern.findAll(text).forEach { token ->
            val statement = statements[token.groupValues[1]] ?: return@forEach
            val match = statement.pattern.matchAt(text, token.range.first) ?: return@forEach
            output.append(text, copied, match.range.first)
            output.append(statement.text)
            copied = match.range.last + 1
        }
        return output.append(text, copied, text.length).toString()
    }

    fun isSplice(node: Node): Boolean = node.tag == spliceTag

    fun hasExplicitTag(node: Node): Boolean = node.startMark.index in explicitTags

    fun fragment(node: Node): String {
        val value =
            if (node is ScalarNode)
                ScalarNode(node.tag, node.value, node.startMark, node.endMark, node.scalarStyle)
            else node
        return restore(SnakeYaml.fragment(value))
    }

    fun comments(): List<CommentEvent> = events.filterIsInstance<CommentEvent>()

    fun line(number: Int): String = lines[number].trim { it <= ' ' }

    fun indentation(number: Int): Int = lines[number].takeWhile(Char::isWhitespace).length

    companion object {
        // Carries a Microconfig list insertion through SnakeYAML until placeholders resolve.
        private val spliceTag = Tag("!microconfig-splice")
        private val lineBreak = Regex("\\r\\n|[\\n\\r\u0085\u2028\u2029]")
        private val lineBreakCharacters = charArrayOf('\n', '\r', '\u0085', '\u2028', '\u2029')
        private val markerPattern = Regex("(?=([\\uE000-\\uF8FF]{2}))")
        private val oneLineVar = Regex("(?m)^[\\t ]*\\.var ")
        private val mappingSeparator = Regex(":(?:\\s|$)")
    }

    private data class MaskedStatement(val text: String, val pattern: Regex)

    private class MappingPosition(var expectsKey: Boolean = true, var key: ScalarEvent? = null)
}
