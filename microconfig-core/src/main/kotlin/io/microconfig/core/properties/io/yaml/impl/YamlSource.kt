package io.microconfig.core.properties.io.yaml.impl

import io.microconfig.core.properties.resolvers.expression.impl.ExpressionResolver.Companion.expressionEnd
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders
import org.yaml.snakeyaml.DumperOptions.ScalarStyle
import org.yaml.snakeyaml.error.Mark
import org.yaml.snakeyaml.error.MarkedYAMLException
import org.yaml.snakeyaml.events.*
import org.yaml.snakeyaml.nodes.*
import java.io.StringReader
import java.util.*

/** Adapts Microconfig statements to YAML while retaining offsets in the original file. */
internal class YamlSource(val original: String) {
    private val prepared = StringBuilder(original)
    private val offsets by lazy { (0..original.length).toMutableList() }
    private val statements = linkedMapOf<String, MaskedStatement>()
    private val markersByStatement = mutableMapOf<String, String>()
    private val splices = mutableSetOf<Int>()
    private val possibleContinuations = linkedMapOf<Int, Int>()
    private var markerIndex = 0
    private val varMarker = nextMarker()
    private var events: List<Event> = emptyList()
    private val explicitTags by lazy {
        if (prepared.indexOf("!") < 0) emptySet()
        else events
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
        if (original[start] == '#') return expressionEnd(original, start)
        return PlaceholderBorders.endOfPlaceholder(original, start)
    }

    private fun mask(start: Int, end: Int) {
        val statement = original.substring(start, end)
        val marker =
            markersByStatement.getOrPut(statement) {
                nextMarker().also {
                    statements[it] = MaskedStatement(statement, it)
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
                            if (
                                scopes.isNotEmpty() && !isVariableScope(scopes) &&
                                event.scalarStyle == ScalarStyle.PLAIN
                            )
                                protected.addAll(scalarLines(event, preparedLines))
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

    private fun scalarLines(
        event: ScalarEvent,
        lines: List<String>,
    ): IntRange {
        val firstLine = lines[event.startMark.line]
        val startsOnOwnLine =
            event.startMark.column == firstLine.takeWhile { it == ' ' || it == '\t' }.length
        val first = event.startMark.line + if (startsOnOwnLine) 0 else 1
        val endLine = lines[event.endMark.line]
        val endColumn = endLine.offsetByCodePoints(0, event.endMark.column)
        val trailing = endLine.substring(endColumn).trimStart()
        // The end mark can already point into the indentation before the next YAML token.
        val last = event.endMark.line - if (trailing.isEmpty() || trailing.startsWith('#')) 0 else 1
        return first..last
    }

    fun compose(): Node? {
        while (true) {
            restorePlainContinuations()
            try {
                val parsedEvents = mutableListOf<Event>()
                val node = SnakeYaml.compose(prepared.toString(), parsedEvents)
                if (
                    node != null &&
                    adaptFoldedSplice(
                        node,
                        java.util.Collections.newSetFromMap(IdentityHashMap()),
                    )
                )
                    continue
                events = parsedEvents
                return if (node != null && (splices.isNotEmpty() || explicitTags.isNotEmpty()))
                    prepareStatements(node, IdentityHashMap())
                else node
            } catch (error: MarkedYAMLException) {
                if (!adaptStandaloneSplice()) throw error
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
        check(splices.add(offsets[start])) { "List splice was already adapted" }
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

    private fun adaptStandaloneSplice(): Boolean {
        val currentLines = prepared.split(lineBreak)
        val protected = protectedSpliceLines()
        val starts = listOf(0) + lineBreak.findAll(prepared).map { it.range.last + 1 }.toList()
        currentLines.forEachIndexed { index, line ->
            if (index !in protected && standaloneMarker(line) != null &&
                hasSequenceNeighbor(currentLines, index)
            ) return adaptSpliceAt(starts[index])
        }
        return false
    }

    private fun protectedSpliceLines(): Set<Int> {
        val protected = mutableSetOf<Int>()
        val lines = prepared.split(lineBreak)
        var flowDepth = 0
        try {
            SnakeYaml.parser().parse(StringReader(prepared.toString())).forEach { event ->
                when (event) {
                    is MappingStartEvent -> if (event.flowStyle == org.yaml.snakeyaml.DumperOptions.FlowStyle.FLOW) flowDepth++
                    is SequenceStartEvent -> if (event.flowStyle == org.yaml.snakeyaml.DumperOptions.FlowStyle.FLOW) flowDepth++
                    is MappingEndEvent, is SequenceEndEvent -> if (flowDepth > 0) flowDepth--
                    is ScalarEvent ->
                        if (
                            flowDepth > 0 || event.scalarStyle != ScalarStyle.PLAIN ||
                            standaloneMarker(lines[event.startMark.line]) == null
                        )
                            protected.addAll(scalarLines(event, lines))
                }
            }
        } catch (_: MarkedYAMLException) {
            // Completed events describe scalar and flow boundaries; error locations are irrelevant.
        }
        return protected
    }

    private fun hasSequenceNeighbor(lines: List<String>, index: Int): Boolean {
        val indent = lines[index].takeWhile(Char::isWhitespace).length
        return listOf(index - 1 downTo 0, index + 1 until lines.size).any { direction ->
            val neighbor = direction.firstOrNull {
                val line = lines[it]
                line.isNotBlank() && !line.trimStart().startsWith('#') &&
                        line.takeWhile(Char::isWhitespace).length <= indent
            } ?: return@any false
            val line = lines[neighbor]
            line.takeWhile(Char::isWhitespace).length == indent &&
                    (line.trimStart().let { it == "-" || it.startsWith("- ") } || standaloneMarker(line) != null)
        }
    }

    fun restore(value: String): String {
        if (value.none { it in '\uE000'..'\uF8FF' }) return value
        val text = value.replace("#$varMarker", ".va")
        val output = StringBuilder()
        var copied = 0
        markerPattern.findAll(text).forEach { token ->
            val statement = statements[token.groupValues[1]] ?: return@forEach
            val end = statement.endIn(text, token.range.first) ?: return@forEach
            output.append(text, copied, token.range.first)
            output.append(statement.text)
            copied = end
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

    fun comments(): List<CommentEvent> =
        if (prepared.indexOf("#") < 0) emptyList() else events.filterIsInstance<CommentEvent>()

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

    private inner class MaskedStatement(val text: String, marker: String) {
        private val singleLine = text.none { it in lineBreakCharacters }
        private val mask = if (singleLine) marker + "x".repeat(text.length - 2) else ""
        private val pattern = if (singleLine) null else statementPattern(marker, text)

        fun endIn(value: String, start: Int): Int? =
            if (singleLine) (start + mask.length).takeIf { value.startsWith(mask, start) }
            else pattern?.matchAt(value, start)?.range?.last?.plus(1)
    }

    private class MappingPosition(var expectsKey: Boolean = true, var key: ScalarEvent? = null)
}
