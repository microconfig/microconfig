package io.microconfig.core.properties.io.yaml.impl

import java.io.Reader
import java.io.StringReader
import java.io.StringWriter
import java.util.Collections
import java.util.IdentityHashMap
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.SequenceNode
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.util.MergeUtils

internal object SnakeYaml {
    fun parser(comments: Boolean = false): Yaml =
        object :
            Yaml(
                LoaderOptions().apply {
                    isProcessComments = comments
                    isMergeOnCompose = false
                }
            ) {
            override fun compose(reader: Reader): Node? {
                val node = super.compose(reader) ?: return null
                flattenMerges(node, Collections.newSetFromMap(IdentityHashMap()))
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
        checkMergeCycles(node, Collections.newSetFromMap(IdentityHashMap()))
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

    fun parse(text: String): Node = parser().compose(StringReader(text)) ?: emptyValue()

    fun emptyValue(): ScalarNode =
        ScalarNode(Tag.NULL, "", null, null, DumperOptions.ScalarStyle.PLAIN)

    fun text(
        value: String,
        style: DumperOptions.ScalarStyle = DumperOptions.ScalarStyle.PLAIN,
    ): ScalarNode = ScalarNode(Tag.STR, value, null, null, style)

    fun serialize(node: Node, sectionBreaks: Boolean = false): String {
        val output = StringWriter()
        emitter(sectionBreaks, node is SequenceNode).serialize(node, output)
        return output.toString()
    }

    private fun emitter(sectionBreaks: Boolean, rootSequence: Boolean): Yaml =
        Yaml(
            DumperOptions().apply {
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                indent = 2
                indicatorIndent = if (rootSequence) 0 else 2
                indentWithIndicator = !rootSequence
                setSplitLines(false)
                isProcessComments = sectionBreaks
                lineBreak = DumperOptions.LineBreak.UNIX
            }
        )

    fun scalar(node: ScalarNode): String = serialize(node).removeSuffix("\n")

    fun fragment(node: Node): String =
        serialize(node).let {
            if (lastBlockScalar(node) != null) it else it.removeSuffix("\n")
        }

    private fun lastBlockScalar(node: Node): ScalarNode? =
        lastScalar(node)?.takeIf {
            it.scalarStyle == DumperOptions.ScalarStyle.LITERAL ||
                it.scalarStyle == DumperOptions.ScalarStyle.FOLDED
        }

    private fun lastScalar(
        node: Node,
        visited: MutableSet<Node> =
            java.util.Collections.newSetFromMap(java.util.IdentityHashMap()),
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
