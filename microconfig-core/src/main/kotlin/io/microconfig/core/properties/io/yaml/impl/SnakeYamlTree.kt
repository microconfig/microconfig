package io.microconfig.core.properties.io.yaml.impl

import io.microconfig.core.properties.io.yaml.YamlTree
import java.util.IdentityHashMap
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.comments.CommentLine
import org.yaml.snakeyaml.comments.CommentType
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.Tag

class SnakeYamlTree(private val sectionBreaks: Boolean) : YamlTree {
    override fun toYaml(flatProperties: Map<String, String>): String {
        if (flatProperties.isEmpty()) return ""
        val tree = PropertyTree().build(flatProperties)
        addSectionBreaks(tree)
        return if (sectionBreaks) SnakeYaml.serialize(tree, true) else SnakeYaml.fragment(tree)
    }

    private class PropertyTree {
        private val sections =
            java.util.Collections.newSetFromMap(IdentityHashMap<MappingNode, Boolean>())

        fun build(properties: Map<String, String>): MappingNode {
            val root = section()
            properties.toSortedMap().forEach { (key, value) ->
                insert(root, YamlPropertyPaths.parts(key), SnakeYaml.parse(value))
            }
            sort(root)
            return root
        }

        private fun section(): MappingNode =
            MappingNode(Tag.MAP, mutableListOf(), DumperOptions.FlowStyle.BLOCK).also(sections::add)

        private fun insert(parent: MappingNode, parts: List<String>, value: Node) {
            if (parts.size == 1) {
                put(parent, parts.first(), value)
                return
            }
            val entry = find(parent, parts.first())
            if (entry == null) {
                val child = section()
                put(parent, parts.first(), child)
                insert(child, parts.drop(1), value)
            } else if (entry.valueNode in sections) {
                insert(entry.valueNode as MappingNode, parts.drop(1), value)
            } else {
                insert(parent, listOf(combinedKey(parts[0], parts[1])) + parts.drop(2), value)
            }
        }

        private fun combinedKey(first: String, second: String): String =
            SnakeYaml.scalar(SnakeYaml.text(keyNode(first).value + "." + keyNode(second).value))

        private fun keyNode(name: String): ScalarNode =
            SnakeYaml.parse(name) as? ScalarNode ?: SnakeYaml.text(name)

        private fun put(parent: MappingNode, name: String, value: Node) {
            val key = keyNode(name)
            find(parent, name)?.let(parent.value::remove)
            parent.value.add(NodeTuple(key, value))
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
