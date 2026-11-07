package io.microconfig.core.properties.io.yaml

import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.ValueFormat
import io.microconfig.core.properties.io.properties.PropertiesText
import io.microconfig.core.properties.io.yaml.impl.SnakeYaml
import io.microconfig.core.properties.io.yaml.impl.YamlPropertyPaths
import io.microconfig.core.properties.io.yaml.impl.YamlSource
import org.yaml.snakeyaml.DumperOptions.ScalarStyle
import org.yaml.snakeyaml.error.YAMLException
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.Tag
import java.util.*

object YamlScalars {
    // A conservative subset that stays one plain scalar in block and flow YAML.
    internal fun isPlainScalar(text: String): Boolean =
        text.isNotEmpty() && text != "-" && text != "---" && text != "..." &&
                text.all { it.isLetterOrDigit() || it in "_./-" }

    internal fun canonicalKeyText(text: String): String {
        val node = runCatching { SnakeYaml.parse(text) as? ScalarNode }.getOrNull() ?: return text
        return if (node.tag == Tag.STR) text else canonicalKey(node)
    }

    internal fun canonicalKey(node: ScalarNode): String {
        val key = canonicalScalar(node)
        return SnakeYaml.fragment(
            ScalarNode(
                key.tag,
                key.value,
                key.startMark,
                key.endMark,
                if ('.' in key.value) ScalarStyle.DOUBLE_QUOTED else key.scalarStyle,
            )
        )
    }

    internal fun canonicalScalarValue(node: ScalarNode): String = canonicalScalar(node).value

    private fun canonicalScalar(node: ScalarNode): ScalarNode {
        if (node.tag !in canonicalKeyTags) return node
        return try {
            val yaml = SnakeYaml.parser()
            (yaml.represent(yaml.load<Any?>(SnakeYaml.scalar(node))) as ScalarNode).takeIf {
                it.tag == node.tag
            } ?: node
        } catch (_: RuntimeException) {
            node
        }
    }

    private val canonicalKeyTags = setOf(Tag.INT, Tag.FLOAT, Tag.BOOL, Tag.NULL)

    internal fun canonicalPathText(text: String): String =
        YamlPropertyPaths.parts(text).joinToString(".", transform = ::canonicalKeyText)

    internal fun diffKeyPath(key: String, prefix: Char?): String {
        val parts = YamlPropertyPaths.parts(key)
        val node = SnakeYaml.parse(parts.first()) as ScalarNode
        val first = if (prefix == null) {
            val initial = node.value.firstOrNull()
            if (node.tag != Tag.STR || initial != '+' && initial != '-' && initial != '\\') return key
            "\\${node.value}"
        } else {
            val scalar = ScalarNode(node.tag, node.value, node.startMark, node.endMark, ScalarStyle.PLAIN)
            "$prefix${SnakeYaml.scalar(scalar)}"
        }
        val prefixed = if ('.' in first) doubleQuoted(first) else stringValue(first)
        return (listOf(prefixed) + parts.drop(1)).joinToString(".")
    }

    internal fun matchingKeyPrefix(key: String, reference: String): String? {
        val referenceParts = YamlPropertyPaths.parts(reference)
        val keyParts = YamlPropertyPaths.parts(key)
        var index = 0
        for ((partIndex, part) in keyParts.withIndex()) {
            if (part == canonicalKeyText(referenceParts[index])) index++
            else if (
                index + 1 < referenceParts.size &&
                part ==
                canonicalKeyText(referenceParts[index] + "." + referenceParts[index + 1])
            )
                index += 2
            else return null
            if (index == referenceParts.size) return keyParts.take(partIndex + 1).joinToString(".")
        }
        return null
    }

    fun fromPropertiesText(text: String): String = fromPlainText(PropertiesText.unescape(text))

    fun fromPlainText(text: String): String =
        if (readsBackAsSameText(text)) text else doubleQuoted(text)

    private fun readsBackAsSameText(text: String): Boolean {
        val value = readBack(text)
        return value != null && value !== notScalar && text == value.toString()
    }

    private fun readBack(text: String): Any? {
        if (text.isEmpty() || text.contains('\n') || text != text.trim()) return notScalar
        val scalar = SnakeYaml.plainScalar(text)
        if (scalar?.tag == Tag.STR) return text
        return runCatching {
            val yaml = SnakeYaml.parser()
            if (scalar != null) return@runCatching yaml.load<Any?>(text)
            val mapping = yaml.load<Map<String, Any?>>("value: $text")
            val value = mapping["value"]
            val flow = yaml.load<List<Any?>>("[$text]")
            if (
                value is Map<*, *> ||
                value is Iterable<*> ||
                flow.size != 1 ||
                !Objects.equals(value, flow[0])
            )
                notScalar
            else value
        }
            .getOrDefault(notScalar)
    }

    private val notScalar = Any()

    fun stringValue(text: String): String = if (text == readBack(text)) text else doubleQuoted(text)

    fun literalValue(text: String): String = stringValue(text)

    fun serializedKey(property: Property, key: String = property.key): String =
        if (property.keyFormat == ConfigFormat.PROPERTIES)
            YamlPropertyPaths.fromProperties(key)
        else key

    internal fun indexProperties(
        properties: Collection<Property>,
        overwrite: Boolean = true,
    ): Map<String, Property> {
        if (properties.isEmpty()) return emptyMap()
        if (properties.all { it.keyFormat == ConfigFormat.YAML }) {
            val indexed = linkedMapOf<String, Property>()
            properties.forEach {
                if (overwrite) indexed[it.key] = it else indexed.putIfAbsent(it.key, it)
            }
            return indexed
        }
        val propertiesOnly = properties.all { it.keyFormat == ConfigFormat.PROPERTIES }
        fun keyOf(property: Property): String =
            if (propertiesOnly) PropertiesText.unescape(property.key) else serializedKey(property)

        val indexed = linkedMapOf<String, Property>()
        val rawKeyByIdentity = mutableMapOf<String, String>()
        properties.forEach { property ->
            val key = keyOf(property)
            val previousKey = rawKeyByIdentity[key]
            if (!overwrite && (previousKey != null || property.key in indexed)) return@forEach
            if (previousKey != null && previousKey != property.key) indexed.remove(previousKey)
            val previous = indexed.put(property.key, property)
            if (previous != null) rawKeyByIdentity.remove(keyOf(previous))
            rawKeyByIdentity[key] = property.key
        }
        return indexed
    }

    /** Encodes the logical key for Java Properties without changing already encoded keys. */
    fun propertiesKey(property: Property): String =
        if (property.keyFormat == ConfigFormat.PROPERTIES) property.key
        else YamlPropertyPaths.toProperties(property.key)

    fun serializedValue(property: Property): String =
        when {
            property.valueFormat == ValueFormat.PROPERTIES ->
                fromPropertiesText(property.value)

            property.isLiteralText -> literalFragment(property.value)
            property.valueFormat == ValueFormat.TEXT -> fromPlainText(property.value)
            else -> property.value
        }

    private fun literalFragment(text: String): String =
        SnakeYaml.fragment(
            SnakeYaml.text(
                text,
                if (text.contains('\n')) ScalarStyle.LITERAL else ScalarStyle.PLAIN,
            )
        )

    internal fun isMappingSource(value: String): Boolean =
        try {
            YamlSource(value).compose() is MappingNode
        } catch (_: YAMLException) {
            false
        }

    fun unquote(value: String): String = quotedTextOrNull(value) ?: value

    internal fun quotedTextOrNull(value: String): String? = runCatching {
        val scalar = SnakeYaml.parse(value) as? ScalarNode
        scalar?.takeIf {
            it.scalarStyle == ScalarStyle.DOUBLE_QUOTED || it.scalarStyle == ScalarStyle.SINGLE_QUOTED
        }?.value
    }.getOrNull()

    fun doubleQuoted(text: String): String =
        SnakeYaml.scalar(SnakeYaml.text(text, ScalarStyle.DOUBLE_QUOTED))
}
