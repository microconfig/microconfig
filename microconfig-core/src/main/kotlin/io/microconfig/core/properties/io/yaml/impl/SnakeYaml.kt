package io.microconfig.core.properties.io.yaml.impl

import io.microconfig.core.properties.io.yaml.YamlScalars
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.composer.Composer
import org.yaml.snakeyaml.emitter.Emitter
import org.yaml.snakeyaml.events.CommentEvent
import org.yaml.snakeyaml.events.Event
import org.yaml.snakeyaml.nodes.*
import org.yaml.snakeyaml.parser.Parser
import org.yaml.snakeyaml.parser.ParserImpl
import org.yaml.snakeyaml.reader.StreamReader
import org.yaml.snakeyaml.resolver.Resolver
import org.yaml.snakeyaml.serializer.Serializer
import org.yaml.snakeyaml.util.MergeUtils
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter
import java.util.*
import java.util.Collections.newSetFromMap

internal object SnakeYaml {
    private val scalarResolver = Resolver()
    private val plainFragment = Regex("(?:[A-Za-z_1-9][A-Za-z0-9_./-]*|0)")

    fun plainScalar(text: String): ScalarNode? =
        if (YamlScalars.isPlainScalar(text))
            ScalarNode(
                scalarResolver.resolve(NodeId.scalar, text, true), text,
                null, null, DumperOptions.ScalarStyle.PLAIN
            )
        else null

    fun compose(text: String, events: MutableList<Event>): Node? {
        val options = LoaderOptions().apply {
            isProcessComments = true
            isMergeOnCompose = false
        }
        val parser = recordingParser(text, options, events)
        val node = Composer(parser, scalarResolver, options).singleNode ?: return null
        flattenMerges(node, newSetFromMap(IdentityHashMap()))
        return node
    }

    private fun recordingParser(text: String, options: LoaderOptions, events: MutableList<Event>): Parser {
        val parser = ParserImpl(StreamReader(StringReader(text)), options)
        // Retain metadata while composing, so callers do not need a second parse.
        return object : Parser {
            override fun checkEvent(choice: Event.ID): Boolean = peekEvent()?.eventId == choice

            override fun peekEvent(): Event? {
                while (parser.peekEvent() is CommentEvent) events.add(parser.event)
                return parser.peekEvent()
            }

            override fun getEvent(): Event? =
                peekEvent()?.let { parser.event.also(events::add) }
        }
    }

    fun parser(): Yaml =
        object :
            Yaml(
                LoaderOptions().apply {
                    isMergeOnCompose = false
                }
            ) {
            override fun compose(reader: Reader): Node? {
                val node = super.compose(reader) ?: return null
                flattenMerges(node, newSetFromMap(IdentityHashMap()))
                return node
            }
        }

    private fun flattenMerges(node: Node, visited: MutableSet<Node>) {
        if (!visited.add(node)) return
        when (node) {
            is SequenceNode -> node.value.forEach { flattenMerges(it, visited) }
            is MappingNode -> {
                node.value.forEach {
                    flattenMerges(it.keyNode, visited)
                    flattenMerges(it.valueNode, visited)
                }
                if (node.isMerged) flattenMapping(node)
            }
        }
    }

    private fun flattenMapping(node: MappingNode) {
        checkMergeCycles(node, newSetFromMap(IdentityHashMap()))
        node.value = merges.flatten(node)
        node.isMerged = false
    }

    private fun checkMergeCycles(node: MappingNode, visiting: MutableSet<Node>) {
        require(visiting.add(node)) {
            "Recursive YAML merge at line ${(node.startMark?.line ?: 0) + 1}"
        }
        node.value
            .filter { it.keyNode.tag == Tag.MERGE }
            .forEach { entry ->
                val value = entry.valueNode
                val sources = if (value is SequenceNode) value.value else listOf(value)
                sources.forEach { checkMergeCycles(merges.asMappingNode(it), visiting) }
            }
        visiting.remove(node)
    }

    private val merges =
        object : MergeUtils() {
            override fun asMappingNode(node: Node): MappingNode {
                require(node is MappingNode) {
                    "YAML merge must contain a mapping at line ${(node.startMark?.line ?: 0) + 1}"
                }
                return node
            }
        }

    fun parse(text: String): Node =
        plainScalar(text) ?: parser().compose(StringReader(text)) ?: emptyValue()

    fun emptyValue(): ScalarNode =
        ScalarNode(Tag.NULL, "", null, null, DumperOptions.ScalarStyle.PLAIN)

    fun text(
        value: String,
        style: DumperOptions.ScalarStyle = DumperOptions.ScalarStyle.PLAIN,
    ): ScalarNode = ScalarNode(Tag.STR, value, null, null, style)

    fun serialize(node: Node, sectionBreaks: Boolean = false): String {
        val output = StringWriter()
        val options = dumperOptions(sectionBreaks, node is SequenceNode)
        val serializer = Serializer(Emitter(output, options), scalarResolver, options, null)
        serializer.open()
        serializer.serialize(node)
        serializer.close()
        return output.toString()
    }

    private fun dumperOptions(sectionBreaks: Boolean, rootSequence: Boolean): DumperOptions =
        DumperOptions().apply {
            // Regenerate parsed anchors too, avoiding collisions with newly shared nodes.
            var anchorId = 0
            setAnchorGenerator { "id${++anchorId}" }
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            indent = 2
            indicatorIndent = if (rootSequence) 0 else 2
            indentWithIndicator = !rootSequence
            setSplitLines(false)
            isProcessComments = sectionBreaks
            lineBreak = DumperOptions.LineBreak.UNIX
        }

    fun scalar(node: ScalarNode): String = serialize(node).removeSuffix("\n")

    fun fragment(node: Node): String {
        plainFragmentOrNull(node)?.let { return it }
        return serialize(node).let {
            if (lastBlockScalar(node) != null) it else it.removeSuffix("\n")
        }
    }

    fun plainFragmentOrNull(node: Node): String? =
        if (node is ScalarNode && node.scalarStyle == DumperOptions.ScalarStyle.PLAIN &&
            node.anchor == null && plainFragment.matches(node.value) &&
            scalarResolver.resolve(NodeId.scalar, node.value, true) == node.tag
        ) node.value
        else null

    private fun lastBlockScalar(node: Node): ScalarNode? =
        lastScalar(node)?.takeIf {
            it.scalarStyle == DumperOptions.ScalarStyle.LITERAL ||
                    it.scalarStyle == DumperOptions.ScalarStyle.FOLDED
        }

    private fun lastScalar(
        node: Node,
        visited: MutableSet<Node> = newSetFromMap(IdentityHashMap()),
    ): ScalarNode? {
        if (!visited.add(node)) return null
        return when (node) {
            is ScalarNode -> node
            is MappingNode -> node.value.lastOrNull()?.valueNode?.let { lastScalar(it, visited) }
            is SequenceNode -> node.value.lastOrNull()?.let { lastScalar(it, visited) }
            else -> null
        }
    }

    fun keepsTrailingLines(node: Node): Boolean =
        lastBlockScalar(node)?.value?.let {
            it.endsWith("\n\n") || it == "\n"
        } ?: false
}
