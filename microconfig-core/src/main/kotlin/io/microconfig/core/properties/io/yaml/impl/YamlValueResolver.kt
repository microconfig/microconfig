package io.microconfig.core.properties.io.yaml.impl

import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.FileBasedComponent
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl.Companion.literalProperty
import io.microconfig.core.properties.PropertyImpl.Companion.property
import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.properties.resolvers.RecursiveResolver
import io.microconfig.core.properties.resolvers.expression.impl.ExpressionResolver.Companion.expressionsIn
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders.Companion.findPlaceholderIn
import java.util.IdentityHashMap
import java.util.regex.MatchResult
import org.yaml.snakeyaml.DumperOptions.ScalarStyle
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.SequenceNode

class YamlValueResolver(private val delegate: RecursiveResolver) : Resolver {
    override fun resolve(
        value: String,
        sourceOfValue: DeclaringComponent?,
        root: DeclaringComponent?,
    ): String = delegate.resolve(value, sourceOfValue, root)

    override fun resolve(property: Property, root: DeclaringComponent): String {
        if ((property.declaringComponent as? FileBasedComponent)?.isYaml != true)
            return delegate.resolve(property, root)
        if (property.configFormat != YAML || property.isLiteralText)
            return resolveScalar(property, root)
        return resolveYamlProperty(property, root)
    }

    private fun resolveYamlProperty(property: Property, root: DeclaringComponent): String {
        val source = YamlSource(property.value)
        val node = source.compose() ?: return property.value
        return when {
            node is MappingNode || node is SequenceNode ->
                resolveCollection(node, source, property, root)
            node is ScalarNode && source.hasExplicitTag(node) ->
                SnakeYaml.fragment(resolveScalarNode(node, source, property, root))
            else -> resolveScalar(property, root)
        }
    }

    private fun resolveCollection(
        node: Node,
        source: YamlSource,
        property: Property,
        root: DeclaringComponent,
    ): String {
        if (delegate.findStatementIn(property.value).isEmpty) return property.value
        return SnakeYaml.fragment(resolveNode(node, source, property, root, IdentityHashMap()))
    }

    private fun resolveNode(
        node: Node,
        source: YamlSource,
        property: Property,
        root: DeclaringComponent,
        resolved: MutableMap<Node, Node>,
    ): Node {
        resolved[node]?.let {
            return it
        }
        return when (node) {
            is ScalarNode ->
                resolveScalarNode(node, source, property, root).also { resolved[node] = it }
            is SequenceNode -> resolveSequence(node, source, property, root, resolved)
            is MappingNode -> resolveMapping(node, source, property, root, resolved)
            else -> error("Unsupported YAML node ${node.nodeId}")
        }
    }

    private fun resolveScalarNode(
        node: ScalarNode,
        source: YamlSource,
        collection: Property,
        root: DeclaringComponent,
    ): Node {
        val value = source.restore(node.value)
        if (delegate.findStatementIn(value).isEmpty)
            return if (value == node.value) node
            else ScalarNode(node.tag, value, node.startMark, node.endMark, node.scalarStyle)
        val declaration = collection.declaringComponent
        if (node.scalarStyle == ScalarStyle.LITERAL || node.scalarStyle == ScalarStyle.FOLDED) {
            val resolvedValue =
                resolveScalar(literalProperty(collection.key, value, YAML, declaration), root)
            return ScalarNode(
                node.tag,
                resolvedValue,
                node.startMark,
                node.endMark,
                node.scalarStyle,
            )
        }
        val quoted =
            !source.isSplice(node) &&
                (node.scalarStyle == ScalarStyle.SINGLE_QUOTED ||
                    node.scalarStyle == ScalarStyle.DOUBLE_QUOTED)
        val original =
            if (quoted) SnakeYaml.scalar(SnakeYaml.parse(source.fragment(node)) as ScalarNode)
            else value
        val result =
            SnakeYaml.parse(
                resolveScalar(property(collection.key, original, YAML, declaration), root)
            )
        if (!source.isSplice(node) && source.hasExplicitTag(node)) result.tag = node.tag
        return result
    }

    private fun resolveScalar(property: Property, root: DeclaringComponent): String {
        val yamlSyntax = property.configFormat == YAML && !property.isLiteralText
        val quoted = yamlSyntax && YamlScalars.isQuoted(property.value)
        val result =
            StringBuilder(if (quoted) YamlScalars.unquote(property.value) else property.value)
        var plainText = false
        var stringValue = quoted
        while (true) {
            val current = delegate.findStatementIn(result).orElse(null) ?: break
            val expression = expressionContaining(result, current)
            if (expression != null) {
                val value =
                    resolveExpression(
                        result.substring(expression.start(), expression.end()),
                        property,
                        root,
                    )
                if (expression.start() == 0 && expression.end() == result.length)
                    stringValue = quoted
                result.replace(expression.start(), expression.end(), value)
                plainText = true
                continue
            }
            var value = current.resolveFor(property.declaringComponent, root)
            val wholeValue = current.startIndex == 0 && current.endIndex == result.length
            if (wholeValue) {
                plainText = false
                stringValue = quoted
            }
            if (
                !current.isPlainText &&
                    (quoted || !yamlSyntax || !wholeValue) &&
                    YamlScalars.isQuoted(value)
            ) {
                value = YamlScalars.unquote(value)
                stringValue = true
            }
            plainText = plainText || current.isPlainText
            result.replace(current.startIndex, current.endIndex, value)
        }
        return when {
            !yamlSyntax -> result.toString()
            quoted -> YamlScalars.doubleQuoted(result.toString())
            stringValue -> YamlScalars.stringValue(result.toString())
            plainText -> YamlScalars.fromPlainText(result.toString())
            else -> result.toString()
        }
    }

    private fun expressionContaining(
        value: CharSequence,
        statement: RecursiveResolver.Statement,
    ): MatchResult? =
        expressionsIn(maskPlaceholders(value))
            .filter { it.start() <= statement.startIndex && statement.endIndex <= it.end() }
            .findFirst()
            .orElse(null)

    private fun maskPlaceholders(value: CharSequence): String {
        val masked = StringBuilder(value)
        while (true) {
            val placeholder = findPlaceholderIn(masked).orElse(null) ?: return masked.toString()
            for (index in placeholder.startIndex until placeholder.endIndex) {
                if (masked[index] != '\n' && masked[index] != '\r') masked.setCharAt(index, 'x')
            }
        }
    }

    private fun resolveExpression(
        expression: String,
        property: Property,
        root: DeclaringComponent,
    ): String {
        val result = StringBuilder(expression)
        while (true) {
            val current = delegate.findStatementIn(result).orElse(null) ?: return result.toString()
            val value = current.resolveFor(property.declaringComponent, root)
            if (current.startIndex == 0 && current.endIndex == result.length) return value
            val quote = expressionQuoteAt(result, current.startIndex)
            result.replace(
                current.startIndex,
                current.endIndex,
                expressionValue(value, current, quote),
            )
        }
    }

    private fun expressionQuoteAt(expression: CharSequence, offset: Int): Char? {
        var quote: Char? = null
        var index = 0
        while (index < offset) {
            val character = expression[index]
            if (character == '$' && index + 1 < offset && expression[index + 1] == '{') {
                val placeholder = findPlaceholderIn(expression, index).orElse(null)
                if (placeholder != null && placeholder.startIndex == index) {
                    index = placeholder.endIndex
                    continue
                }
            }
            if (quote == null && (character == '\'' || character == '"')) quote = character
            else if (character == quote) {
                if (index + 1 < offset && expression[index + 1] == quote) index++ else quote = null
            }
            index++
        }
        return quote
    }

    private fun expressionValue(
        value: String,
        statement: RecursiveResolver.Statement,
        quote: Char?,
    ): String {
        val yamlString = !statement.isPlainText && YamlScalars.isQuoted(value)
        val text = if (yamlString) YamlScalars.unquote(value) else value
        return when {
            quote != null -> text.replace(quote.toString(), quote.toString().repeat(2))
            yamlString -> "'" + text.replace("'", "''") + "'"
            else -> value
        }
    }

    private fun resolveSequence(
        node: SequenceNode,
        source: YamlSource,
        property: Property,
        root: DeclaringComponent,
        resolved: MutableMap<Node, Node>,
    ): SequenceNode {
        val result = SequenceNode(node.tag, mutableListOf(), node.flowStyle)
        resolved[node] = result
        node.value.forEach { child ->
            val value = resolveNode(child, source, property, root, resolved)
            if (source.isSplice(child)) {
                require(value is SequenceNode) {
                    "List splice must resolve to a YAML sequence in ${property.declaringComponent}"
                }
                result.value.addAll(value.value)
            } else result.value.add(value)
        }
        return result
    }

    private fun resolveMapping(
        node: MappingNode,
        source: YamlSource,
        property: Property,
        root: DeclaringComponent,
        resolved: MutableMap<Node, Node>,
    ): MappingNode {
        val result = MappingNode(node.tag, mutableListOf(), node.flowStyle)
        resolved[node] = result
        node.value.forEach { entry ->
            result.value.add(
                NodeTuple(
                    resolveNode(entry.keyNode, source, property, root, resolved),
                    resolveNode(entry.valueNode, source, property, root, resolved),
                )
            )
        }
        return result
    }
}
