package io.microconfig.core.properties.io.yaml

import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.io.properties.PropertiesText
import io.microconfig.core.properties.io.yaml.impl.SnakeYaml
import io.microconfig.core.properties.io.yaml.impl.YamlPropertyPaths
import io.microconfig.core.properties.io.yaml.impl.YamlSource
import java.util.Objects
import org.yaml.snakeyaml.DumperOptions.ScalarStyle
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.Tag

object YamlScalars {
    internal fun canonicalKeyText(text: String): String {
        val node = runCatching { SnakeYaml.parse(text) as? ScalarNode }.getOrNull() ?: return text
        return if (node.tag == Tag.STR) text else canonicalKey(node)
    }

    internal fun canonicalKey(node: ScalarNode): String {
        val key =
            if (node.tag in canonicalKeyTags) {
                try {
                    val yaml = SnakeYaml.parser()
                    (yaml.represent(yaml.load<Any?>(SnakeYaml.scalar(node))) as ScalarNode).takeIf {
                        it.tag == node.tag
                    } ?: node
                } catch (_: RuntimeException) {
                    node
                }
            } else node
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

    private val canonicalKeyTags = setOf(Tag.INT, Tag.FLOAT, Tag.BOOL, Tag.NULL)

    internal fun canonicalPathText(text: String): String =
        YamlPropertyPaths.parts(text).joinToString(".", transform = ::canonicalKeyText)

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
        return runCatching {
                val yaml = SnakeYaml.parser()
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
        if (PropertiesText.isPropertiesText(property.declaringComponent))
            YamlPropertyPaths.fromProperties(key)
        else key

    fun serializedValue(property: Property): String =
        when {
            PropertiesText.isPropertiesText(property.declaringComponent) ->
                fromPropertiesText(property.value)
            property.isLiteralText -> literalFragment(property.value)
            property.configFormat == ConfigFormat.PROPERTIES -> fromPlainText(property.value)
            else -> property.value
        }

    private fun literalFragment(text: String): String =
        SnakeYaml.fragment(
            SnakeYaml.text(
                text,
                if (text.contains('\n')) ScalarStyle.LITERAL else ScalarStyle.PLAIN,
            )
        )

    fun isCollection(value: String): Boolean = runCatching {
        val node = YamlSource(value).compose()
        node is org.yaml.snakeyaml.nodes.MappingNode ||
            node is org.yaml.snakeyaml.nodes.SequenceNode
    }
        .getOrDefault(false)

    fun isQuoted(value: String): Boolean = runCatching {
        val scalar = SnakeYaml.parse(value) as? ScalarNode
        scalar?.scalarStyle == ScalarStyle.DOUBLE_QUOTED ||
            scalar?.scalarStyle == ScalarStyle.SINGLE_QUOTED
    }
        .getOrDefault(false)

    fun unquote(value: String): String =
        if (isQuoted(value)) (SnakeYaml.parse(value) as ScalarNode).value else value

    fun doubleQuoted(text: String): String =
        SnakeYaml.scalar(SnakeYaml.text(text, ScalarStyle.DOUBLE_QUOTED))
}
