package io.microconfig.core.properties.io.yaml.impl

import io.microconfig.core.properties.*
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.PropertyImpl.Companion.literalProperty
import io.microconfig.core.properties.PropertyImpl.Companion.property
import io.microconfig.core.properties.io.properties.PropertiesText
import io.microconfig.core.properties.io.yaml.ExpressionResultCache
import io.microconfig.core.properties.io.yaml.ExpressionValueCache
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.properties.resolvers.RecursiveResolver
import io.microconfig.core.properties.resolvers.expression.impl.ExpressionResolver.Companion.expressionEnd
import io.microconfig.core.properties.resolvers.expression.impl.ExpressionResolver.Companion.expressionQuoteAt
import io.microconfig.core.properties.resolvers.expression.impl.ExpressionResolver.Companion.expressionsIn
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders
import org.yaml.snakeyaml.DumperOptions.ScalarStyle
import org.yaml.snakeyaml.nodes.*
import java.util.*
import java.util.regex.MatchResult
import kotlin.jvm.optionals.getOrNull

class YamlValueResolver internal constructor(
    private val delegate: RecursiveResolver,
    private val expressionCache: ExpressionValueCache?,
) : Resolver {
    constructor(delegate: RecursiveResolver) : this(delegate, null)

    override fun resolve(
        value: String,
        sourceOfValue: DeclaringComponent?,
        root: DeclaringComponent?,
    ): String = delegate.resolve(value, sourceOfValue, root)

    override fun resolve(property: Property, root: DeclaringComponent): String =
        when (property.valueFormat) {
            ValueFormat.PROPERTIES -> resolvePropertiesText(property, root)
            ValueFormat.YAML -> resolveYamlProperty(property, root)
            ValueFormat.TEXT -> resolveScalar(property, root)
        }

    private fun resolvePropertiesText(property: Property, root: DeclaringComponent): String {
        if (delegate.findStatementIn(property.value).getOrNull() == null) return property.value
        val decoded = PropertyImpl(
            property.key,
            PropertiesText.unescape(property.value),
            property.isVar,
            property.configFormat,
            property.declaringComponent,
            property.isLiteralText,
            ValueFormat.TEXT,
            property.keyFormat,
        )
        return PropertiesText.escapeValue(resolveScalar(decoded, root))
    }

    private fun resolveYamlProperty(property: Property, root: DeclaringComponent): String {
        if (YamlScalars.isPlainScalar(property.value) && delegate.findStatementIn(property.value).getOrNull() == null)
            return property.value
        if (isWholeStatement(property.value)) return resolveScalar(property, root, null)
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

    private fun isWholeStatement(value: String): Boolean =
        value.startsWith("\${") && PlaceholderBorders.endOfPlaceholder(value, 0) == value.length ||
                value.startsWith("#{") && expressionEnd(value, 0) == value.length

    private fun resolveCollection(
        node: Node,
        source: YamlSource,
        property: Property,
        root: DeclaringComponent,
        expressions: ScalarExpressions? = null,
    ): String {
        if (findStatementIn(source.original, expressions, property, root) == null) {
            validateMappingKeys(node, property, java.util.Collections.newSetFromMap(IdentityHashMap()))
            return source.original
        }
        return SnakeYaml.fragment(resolveNode(node, source, property, root, IdentityHashMap(), expressions))
    }

    private fun validateMappingKeys(node: Node, property: Property, visited: MutableSet<Node>) {
        if (!visited.add(node)) return
        when (node) {
            is MappingNode -> {
                val keys = mutableSetOf<Pair<Tag, String>>()
                node.value.forEach { entry ->
                    checkMappingKey(entry.keyNode, keys, property)
                    validateMappingKeys(entry.keyNode, property, visited)
                    validateMappingKeys(entry.valueNode, property, visited)
                }
            }

            is SequenceNode -> node.value.forEach { validateMappingKeys(it, property, visited) }
        }
    }

    private fun resolveNode(
        node: Node,
        source: YamlSource,
        property: Property,
        root: DeclaringComponent,
        resolved: MutableMap<Node, Node>,
        expressions: ScalarExpressions?,
    ): Node {
        resolved[node]?.let {
            return it
        }
        return when (node) {
            is ScalarNode ->
                resolveScalarNode(node, source, property, root, expressions).also { resolved[node] = it }

            is SequenceNode -> resolveSequence(node, source, property, root, resolved, expressions)
            is MappingNode -> resolveMapping(node, source, property, root, resolved, expressions)
            else -> error("Unsupported YAML node ${node.nodeId}")
        }
    }

    private fun resolveScalarNode(
        node: ScalarNode,
        source: YamlSource,
        collection: Property,
        root: DeclaringComponent,
        expressions: ScalarExpressions? = null,
    ): Node {
        val value = source.restore(node.value)
        if (findStatementIn(value, expressions, collection, root) == null)
            return if (value == node.value) node
            else ScalarNode(node.tag, value, node.startMark, node.endMark, node.scalarStyle)
        val declaration = collection.declaringComponent
        if (node.scalarStyle == ScalarStyle.LITERAL || node.scalarStyle == ScalarStyle.FOLDED) {
            val resolvedValue =
                resolveScalar(
                    literalProperty(collection.key, value, YAML, declaration, ValueFormat.TEXT), root,
                    scalarStart = node.startMark?.index ?: 0, inheritedExpressions = expressions
                )
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
                resolveScalar(
                    property(collection.key, original, YAML, declaration, ValueFormat.YAML), root,
                    scalarStart = node.startMark?.index ?: 0, inheritedExpressions = expressions
                )
            )
        if (!source.isSplice(node) && source.hasExplicitTag(node)) result.tag = node.tag
        return result
    }

    private fun resolveScalar(
        property: Property,
        root: DeclaringComponent,
        quotedValue: String? = if (property.valueFormat == ValueFormat.YAML)
            YamlScalars.quotedTextOrNull(property.value) else null,
        scalarStart: Int = 0,
        inheritedExpressions: ScalarExpressions? = null,
    ): String {
        val yamlSyntax = property.valueFormat == ValueFormat.YAML
        val quoted = quotedValue != null
        val original = quotedValue ?: property.value
        val expressions = inheritedExpressions ?: expressionCache?.let { ScalarExpressions(original, scalarStart, it) }
        val result = StringBuilder(if (inheritedExpressions == null) expressions?.value ?: original else original)
        var plainText = false
        var stringValue = quoted
        while (true) {
            val current = findStatementIn(result, expressions, property, root) ?: break
            val expression = expressionContaining(result, current)
            if (expression != null) {
                val value =
                    resolveExpression(
                        result.substring(expression.start(), expression.end()),
                        property,
                        root,
                        expressions,
                    )
                if (expression.start() == 0 && expression.end() == result.length)
                    stringValue = quoted
                result.replace(expression.start(), expression.end(), value)
                plainText = true
                continue
            }
            var value = current.resolveFor(
                property.declaringComponent,
                root,
                if (yamlSyntax) ValueFormat.YAML else ValueFormat.TEXT,
            )
            val wholeValue = current.startIndex == 0 && current.endIndex == result.length
            if (yamlSyntax && !quoted && wholeValue && !current.isPlainText) {
                resolveCollectionValue(value, property, root, expressions)?.let { return it }
            }
            if (wholeValue) {
                plainText = false
                stringValue = quoted
            }
            if (
                yamlSyntax &&
                !current.isPlainText &&
                (quoted || !wholeValue)
            ) {
                YamlScalars.quotedTextOrNull(value)?.let {
                    value = it
                    stringValue = true
                }
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

    private fun findStatementIn(
        value: CharSequence,
        expressions: ScalarExpressions?,
        property: Property,
        root: DeclaringComponent,
    ): RecursiveResolver.Statement? {
        val current = delegate.findStatementIn(value).getOrNull()
        val marked = expressions?.findStatementIn(value) { expression, cacheResult ->
            resolveExpression(expression, property, root, expressions, cacheResult)
        }
            ?: return current
        if (current == null) return marked
        val placeholder = value.subSequence(current.startIndex, current.endIndex).startsWith("\${")
        return if (placeholder || current.startIndex < marked.startIndex) current else marked
    }

    private fun expressionContaining(
        value: CharSequence,
        statement: RecursiveResolver.Statement,
    ): MatchResult? =
        expressionsIn(value)
            .filter { it.start() <= statement.startIndex && statement.endIndex <= it.end() }
            .findFirst()
            .getOrNull()

    private fun resolveExpression(
        expression: String,
        property: Property,
        root: DeclaringComponent,
        expressions: ScalarExpressions? = null,
        cacheResult: ExpressionResultCache? = null,
    ): String {
        val result = StringBuilder(expression)
        while (true) {
            val current =
                findStatementIn(result, expressions, property, root)
                    ?: return result.toString()
            if (current.startIndex == 0 && current.endIndex == result.length) {
                val evaluate = { current.resolveFor(property.declaringComponent, root, ValueFormat.YAML) }
                val text = result.toString()
                return if (cacheResult != null && isCacheableExpression(text, expressions))
                    cacheResult(text, evaluate)
                else evaluate()
            }
            val resolved = current.resolveFor(property.declaringComponent, root, ValueFormat.YAML)
            val value = resolveCachedExpressionsInArgument(resolved, property, root, expressions)
            val quote = expressionQuoteAt(result, current.startIndex)
            result.replace(
                current.startIndex,
                current.endIndex,
                expressionValue(value, current, quote),
            )
        }
    }

    private fun isCacheableExpression(value: String, expressions: ScalarExpressions?): Boolean =
        value.startsWith("#{") && expressionEnd(value, 0) == value.length &&
            PlaceholderBorders.findPlaceholderOrNull(value) == null &&
            expressions?.containsMarkedExpression(value) != true

    private fun resolveCachedExpressionsInArgument(
        value: String,
        property: Property,
        root: DeclaringComponent,
        expressions: ScalarExpressions?,
    ): String {
        if (expressions?.containsMarkedExpression(value) != true) return value
        val text = literalProperty(property.key, value, YAML, property.declaringComponent, ValueFormat.TEXT)
        return resolveScalar(text, root, inheritedExpressions = expressions)
    }

    private fun expressionValue(
        value: String,
        statement: RecursiveResolver.Statement,
        quote: Char?,
    ): String {
        val scalar =
            if (statement.isPlainText) null
            else runCatching { SnakeYaml.parse(value) as? ScalarNode }.getOrNull()
        val quoted = scalar?.scalarStyle == ScalarStyle.SINGLE_QUOTED ||
                scalar?.scalarStyle == ScalarStyle.DOUBLE_QUOTED
        val text = if (quoted) scalar.value else value
        return when {
            quote != null -> text.replace(quote.toString(), quote.toString().repeat(2))
            else -> expressionScalarValue(
                if (quoted) "'" + text.replace("'", "''") + "'" else value,
                scalar,
            )
        }
    }

    private fun expressionScalarValue(value: String, scalar: ScalarNode?): String =
        when (scalar?.tag) {
            Tag.BOOL, Tag.NULL, Tag.INT -> YamlScalars.canonicalScalarValue(scalar)
            Tag.FLOAT -> when (val number = YamlScalars.canonicalScalarValue(scalar)) {
                ".inf" -> "(1.0 / 0.0)"
                "-.inf" -> "(-1.0 / 0.0)"
                ".NaN" -> "(0.0 / 0.0)"
                else -> number
            }

            else -> value
        }

    private fun resolveCollectionValue(
        value: String,
        property: Property,
        root: DeclaringComponent,
        expressions: ScalarExpressions?,
    ): String? {
        if (YamlScalars.isPlainScalar(value)) return null
        val source = YamlSource(value)
        val node = source.compose()
        if (node !is MappingNode && node !is SequenceNode) return null
        return resolveCollection(node, source, property, root, expressions)
    }

    private fun resolveSequence(
        node: SequenceNode,
        source: YamlSource,
        property: Property,
        root: DeclaringComponent,
        resolved: MutableMap<Node, Node>,
        expressions: ScalarExpressions?,
    ): SequenceNode {
        val result = SequenceNode(node.tag, mutableListOf(), node.flowStyle)
        resolved[node] = result
        node.value.forEach { child ->
            val value = resolveNode(child, source, property, root, resolved, expressions)
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
        expressions: ScalarExpressions?,
    ): MappingNode {
        val result = MappingNode(node.tag, mutableListOf(), node.flowStyle)
        resolved[node] = result
        val keys = mutableSetOf<Pair<Tag, String>>()
        node.value.forEach { entry ->
            val key = resolveNode(entry.keyNode, source, property, root, resolved, expressions)
            checkMappingKey(key, keys, property)
            result.value.add(
                NodeTuple(
                    key,
                    resolveNode(entry.valueNode, source, property, root, resolved, expressions),
                )
            )
        }
        return result
    }

    private fun checkMappingKey(key: Node, keys: MutableSet<Pair<Tag, String>>, property: Property) {
        require(keys.add(mappingKey(key))) {
            "Duplicate YAML key ${SnakeYaml.fragment(key)} in ${property.declaringComponent}"
        }
    }

    private fun mappingKey(node: Node): Pair<Tag, String> =
        node.tag to when {
            node !is ScalarNode -> SnakeYaml.fragment(node)
            node.tag == Tag.STR -> node.value
            else -> YamlScalars.canonicalScalarValue(node)
        }
}
