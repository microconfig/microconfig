package io.microconfig.core.properties.io.yaml.impl

import io.microconfig.core.properties.io.yaml.YamlTree
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.comments.CommentLine
import org.yaml.snakeyaml.comments.CommentType
import org.yaml.snakeyaml.nodes.*
import java.io.StringReader
import java.util.*
import java.util.Collections.newSetFromMap

class SnakeYamlTree(private val sectionBreaks: Boolean) : YamlTree {
    override fun toYaml(flatProperties: Map<String, String>): String {
        if (flatProperties.isEmpty()) return ""
        val builder = PropertyTree()
        val tree = builder.build(flatProperties)
        addSectionBreaks(tree)
        builder.plainYaml(tree)?.let { return if (sectionBreaks) it else it.removeSuffix("\n") }
        return if (sectionBreaks) SnakeYaml.serialize(tree, true) else SnakeYaml.fragment(tree)
    }

    private class PropertyTree {
        private val parser by lazy { SnakeYaml.parser() }
        private val sections = newSetFromMap(IdentityHashMap<MappingNode, Boolean>())
        private val collapsedEntries = newSetFromMap(IdentityHashMap<NodeTuple, Boolean>())

        fun build(properties: Map<String, String>): MappingNode {
            val root = section()
            properties.toSortedMap().forEach { (key, value) ->
                insert(root, YamlPropertyPaths.parts(key), parse(value))
            }
            sort(root)
            return root
        }

        fun plainYaml(root: MappingNode): String? {
            val output = StringBuilder()
            return if (appendPlain(root, "", output)) output.toString() else null
        }

        private fun section(): MappingNode =
            MappingNode(Tag.MAP, mutableListOf(), DumperOptions.FlowStyle.BLOCK).also(sections::add)

        private fun parse(text: String): Node =
            SnakeYaml.plainScalar(text) ?: parser.compose(StringReader(text)) ?: SnakeYaml.emptyValue()

        private fun insert(parent: MappingNode, parts: List<String>, value: Node, collapsed: Boolean = false) {
            if (parts.size == 1) {
                put(parent, parts.first(), value, collapsed)
                return
            }
            val entry = find(parent, parts.first())
            if (entry == null) {
                val child = section()
                put(parent, parts.first(), child)
                insert(child, parts.drop(1), value, collapsed)
            } else if (entry.valueNode in sections) {
                insert(entry.valueNode as MappingNode, parts.drop(1), value, collapsed)
            } else {
                insert(parent, listOf(combinedKey(parts[0], parts[1])) + parts.drop(2), value, true)
            }
        }

        private fun combinedKey(first: String, second: String): String =
            SnakeYaml.scalar(SnakeYaml.text(keyNode(first).value + "." + keyNode(second).value))

        private fun keyNode(name: String): ScalarNode =
            parse(name) as? ScalarNode ?: SnakeYaml.text(name)

        private fun put(parent: MappingNode, name: String, value: Node, collapsed: Boolean = false) {
            val key = keyNode(name)
            val previous = find(parent, name)
            require(previous == null || !collapsed && !containsCollapsedEntry(previous)) {
                "Several properties produce the same YAML key '${key.value}'"
            }
            previous?.let(parent.value::remove)
            val entry = NodeTuple(key, value)
            if (collapsed) collapsedEntries.add(entry)
            parent.value.add(entry)
        }

        private fun containsCollapsedEntry(entry: NodeTuple): Boolean {
            if (entry in collapsedEntries) return true
            val value = entry.valueNode
            return value in sections && (value as MappingNode).value.any(::containsCollapsedEntry)
        }

        private fun find(parent: MappingNode, name: String): NodeTuple? {
            val key =
                runCatching { keyNode(name) }.getOrElse { SnakeYaml.text(name) }
            return parent.value.find {
                (it.keyNode as ScalarNode).let { existing ->
                    existing.tag == key.tag && existing.value == key.value
                }
            }
        }

        private fun sort(node: MappingNode) {
            node.value.sortWith(
                compareBy<NodeTuple> { it.valueNode in sections }
                    .thenBy { (it.keyNode as ScalarNode).value }
            )
            node.value
                .filter { it.valueNode in sections }
                .forEach { sort(it.valueNode as MappingNode) }
        }

        private fun appendPlain(node: MappingNode, indent: String, output: StringBuilder): Boolean {
            for (entry in node.value) {
                val key = SnakeYaml.plainFragmentOrNull(entry.keyNode) ?: return false
                if (!entry.keyNode.blockComments.isNullOrEmpty()) output.append('\n')
                output.append(indent).append(key).append(':')
                val value = entry.valueNode
                if (value in sections) {
                    output.append('\n')
                    if (!appendPlain(value as MappingNode, "$indent  ", output)) return false
                } else {
                    val text = SnakeYaml.plainFragmentOrNull(value) ?: return false
                    output.append(' ').append(text).append('\n')
                }
            }
            return true
        }
    }

    private fun addSectionBreaks(root: MappingNode) {
        if (!sectionBreaks) return
        root.value
            .zipWithNext()
            .filterNot { (previous, _) -> SnakeYaml.keepsTrailingLines(previous.valueNode) }
            .forEach { (_, entry) ->
                entry.keyNode.blockComments =
                    listOf(CommentLine(null, null, "", CommentType.BLANK_LINE))
            }
    }
}
