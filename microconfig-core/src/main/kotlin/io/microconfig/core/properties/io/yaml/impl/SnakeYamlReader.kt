package io.microconfig.core.properties.io.yaml.impl

import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.FileBasedComponent.Companion.fileSource
import io.microconfig.core.properties.OverrideProperty
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.io.ConfigReader
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.properties.resolvers.expression.impl.ExpressionResolver
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders
import io.microconfig.io.FsReader
import java.io.File
import java.util.IdentityHashMap
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.error.MarkedYAMLException
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.Tag

class SnakeYamlReader(private val file: File, reader: FsReader) : ConfigReader {
    private val source = YamlSource(if (file.exists()) reader.readFully(file) else "")
    private val document = composeDocument()

    private fun composeDocument(): Node? =
        try {
            source.compose()
        } catch (error: MarkedYAMLException) {
            val mark = error.problemMark ?: error.contextMark
            val location = fileSource(file, mark?.line ?: 0, true, "", "")
            throw IllegalArgumentException(
                "Can't parse YAML in '$location': ${source.restore(error.problem)}",
                error,
            )
        }

    override fun format(): ConfigFormat = ConfigFormat.YAML

    override fun properties(configType: String, environment: String): List<Property> {
        val properties = flattenDocument(configType, environment)
        return properties + environmentVars(configType, environment)
    }

    private fun flattenDocument(configType: String, environment: String): List<Property> {
        val root = document ?: return emptyList()
        require(root is MappingNode && root.tag == Tag.MAP) {
            "YAML configuration must contain a mapping in '${fileSource(file, root.startMark.line, true, configType, environment)}'"
        }
        return flatten(
            root,
            "",
            configType,
            environment,
            java.util.Collections.newSetFromMap(IdentityHashMap()),
        )
    }

    private fun flatten(
        node: MappingNode,
        prefix: String,
        configType: String,
        environment: String,
        path: MutableSet<Node>,
    ): List<Property> {
        check(path.add(node)) {
            "Recursive YAML alias in '$file' at line ${node.startMark.line + 1}"
        }
        val result =
            node.value.flatMap { entry ->
                val key =
                    entry.keyNode as? ScalarNode
                        ?: throw IllegalArgumentException(
                            "YAML property key must be a scalar in '$file'"
                        )
                val name = source.restore(key.value)
                if (
                    key.scalarStyle == DumperOptions.ScalarStyle.PLAIN &&
                        PropertyImpl.isTempProperty(name)
                )
                    emptyList()
                else
                    readProperty(
                        entry.valueNode,
                        key,
                        prefix + keyPart(key),
                        configType,
                        environment,
                        path,
                    )
            }
        path.remove(node)
        return result
    }

    private fun keyPart(key: ScalarNode): String {
        val name = source.restore(key.value)
        if (
            key.scalarStyle == DumperOptions.ScalarStyle.PLAIN &&
                OverrideProperty.isOverrideProperty(name)
        )
            return name
        if (key.tag != Tag.STR) return source.fragment(key)
        val protected =
            key.scalarStyle != DumperOptions.ScalarStyle.PLAIN &&
                ('.' in name ||
                    OverrideProperty.isOverrideProperty(name) ||
                    PropertyImpl.isTempProperty(name))
        val style =
            if (protected) DumperOptions.ScalarStyle.DOUBLE_QUOTED
            else DumperOptions.ScalarStyle.PLAIN
        return SnakeYaml.scalar(SnakeYaml.text(name, style))
    }

    private fun readProperty(
        value: Node,
        key: ScalarNode,
        name: String,
        configType: String,
        environment: String,
        path: MutableSet<Node>,
    ): List<Property> {
        if (
            value is MappingNode &&
                value.flowStyle == DumperOptions.FlowStyle.BLOCK &&
                value.tag == Tag.MAP &&
                value.value.isNotEmpty()
        ) {
            return flatten(value, "$name.", configType, environment, path)
        }
        return listOfNotNull(toProperty(value, key, name, configType, environment))
    }

    private fun toProperty(
        node: Node,
        key: ScalarNode,
        name: String,
        configType: String,
        environment: String,
    ): Property? {
        val declaredIn = fileSource(file, key.startMark.line, true, configType, environment)
        val literal =
            node is ScalarNode &&
                node.tag == Tag.STR &&
                node.scalarStyle in
                    setOf(DumperOptions.ScalarStyle.LITERAL, DumperOptions.ScalarStyle.FOLDED)
        val value = valueOf(node)
        OverrideProperty.checkMultilineVarPosition(name, declaredIn)
        if (OverrideProperty.isOverrideProperty(name)) {
            if (OverrideProperty.isEmptyBlock(name, value, declaredIn)) return null
            return OverrideProperty.overrideProperty(
                name,
                value,
                ConfigFormat.YAML,
                declaredIn,
                literal,
            )
        }
        return if (literal) PropertyImpl.literalProperty(name, value, ConfigFormat.YAML, declaredIn)
        else PropertyImpl.property(name, value, ConfigFormat.YAML, declaredIn)
    }

    private fun valueOf(node: Node): String {
        if (node !is ScalarNode) return source.fragment(node)
        if (node.tag != Tag.STR && source.hasExplicitTag(node))
            return source.restore(SnakeYaml.scalar(node))
        if (node.scalarStyle == DumperOptions.ScalarStyle.PLAIN) {
            val value = source.restore(node.value)
            return if (
                node.tag == Tag.STR &&
                    (source.hasExplicitTag(node) ||
                        value.contains('\n') && !isWholeStatement(value))
            )
                YamlScalars.stringValue(value)
            else value
        }
        if (
            node.scalarStyle == DumperOptions.ScalarStyle.LITERAL ||
                node.scalarStyle == DumperOptions.ScalarStyle.FOLDED
        ) {
            return source.restore(node.value)
        }
        val original = SnakeYaml.parse(source.fragment(node)) as ScalarNode
        return SnakeYaml.scalar(
            SnakeYaml.text(source.restore(original.value), original.scalarStyle)
        )
    }

    private fun isWholeStatement(value: String): Boolean {
        val placeholder = PlaceholderBorders.findPlaceholderIn(value).orElse(null)
        if (placeholder?.startIndex == 0 && placeholder.endIndex == value.length) return true
        return ExpressionResolver.expressionsIn(value).anyMatch {
            it.start() == 0 && it.end() == value.length
        }
    }

    private fun environmentVars(configType: String, environment: String): List<Property> =
        environmentHeaders().map { (line, header) ->
            OverrideProperty.envVarProperty(
                header,
                source.line(line),
                ConfigFormat.PROPERTIES,
                fileSource(file, line, true, configType, environment),
            )
        }

    private fun environmentHeaders(): Map<Int, String> {
        val root = document as? MappingNode ?: return emptyMap()
        val headers = mutableMapOf<Int, String>()
        val directives = directives()
        root.value.forEach { entry ->
            val key = entry.keyNode as? ScalarNode ?: return@forEach
            val name = source.restore(key.value)
            if (
                key.scalarStyle != DumperOptions.ScalarStyle.PLAIN ||
                    !OverrideProperty.isEnvBlock(name)
            )
                return@forEach
            val first = key.startMark.line
            val next =
                root.value
                    .dropWhile { it !== entry }
                    .drop(1)
                    .firstOrNull()
                    ?.keyNode
                    ?.startMark
                    ?.line ?: Int.MAX_VALUE
            directives.forEach { (line, directive) ->
                if (
                    line > first &&
                        line < next &&
                        source.indentation(line) > key.startMark.column &&
                        PropertyImpl.isTempProperty(directive)
                )
                    headers[line] = name
            }
        }
        return headers
    }

    private fun directives(): MutableMap<Int, String> {
        val result = sortedMapOf<Int, String>()
        source
            .comments()
            .filter { PropertyImpl.isDirective(source.line(it.startMark.line)) }
            .forEach { event ->
                val comment = source.restore("#" + event.value)
                if (PropertyImpl.isDirective(comment))
                    result[event.startMark.line] = comment.trim { it <= ' ' }
            }
        return result
    }

    override fun propertiesAsMap(): Map<String, String> =
        properties("", "")
            .associate {
                it.key to SnakeYaml.fragment(SnakeYaml.parse(YamlScalars.serializedValue(it)))
            }
            .toSortedMap()

    override fun commentsByLineNumber(): Map<Int, String> {
        val declarations = directives()
        environmentHeaders().keys.forEach(declarations::remove)
        return declarations
    }
}
