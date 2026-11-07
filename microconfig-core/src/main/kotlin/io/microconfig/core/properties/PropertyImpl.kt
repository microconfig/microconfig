package io.microconfig.core.properties

import io.microconfig.core.properties.impl.GroupedProperty

@ConsistentCopyVisibility
data class PropertyImpl
internal constructor(
    override val key: String,
    override val value: String,
    override val isVar: Boolean,
    override val configFormat: ConfigFormat,
    override val declaringComponent: DeclaringComponent?,
    override val isLiteralText: Boolean = false,
    override val valueFormat: ValueFormat =
        if (configFormat == ConfigFormat.YAML && !isLiteralText) ValueFormat.YAML
        else ValueFormat.TEXT,
    override val keyFormat: ConfigFormat = configFormat,
) : Property {
    override fun resolveBy(resolver: Resolver, root: DeclaringComponent): Property =
        try {
            copy(value = resolver.resolve(this, root))
        } catch (error: ResolveException) {
            error.property = this
            throw error
        }

    override fun toString(): String = (if (isVar) "#" else "") + "$key=$value"

    companion object {
        private const val TEMP_VALUE = "#var "
        private const val ONE_LINE_VAR = ".var "

        fun parse(
            keyValue: String,
            configFormat: ConfigFormat,
            source: DeclaringComponent?,
        ): Property {
            val separator = findSeparatorIndexIn(keyValue)
            require(separator >= 0) {
                "Incorrect delimiter in '$keyValue' in '$source'\nProperty must contain ':' or '=' as delimiter."
            }
            return parsedProperty(keyValue, separator, configFormat, source)
        }

        private fun parsedProperty(
            text: String,
            separator: Int,
            format: ConfigFormat,
            source: DeclaringComponent?,
        ): Property {
            val temporary = isTempProperty(text)
            val prefix =
                if (!temporary) 0
                else if (text.startsWith(TEMP_VALUE)) TEMP_VALUE.length else ONE_LINE_VAR.length
            return PropertyImpl(
                text.substring(prefix, separator).trim { it <= ' ' },
                text.substring(separator + 1).trim { it <= ' ' },
                temporary,
                format,
                source,
                valueFormat = sourceValueFormat(format, source),
                keyFormat = sourceKeyFormat(format, source),
            )
        }

        @JvmOverloads
        fun property(
            key: String,
            value: String,
            format: ConfigFormat,
            source: DeclaringComponent?,
            valueFormat: ValueFormat = sourceValueFormat(format, source),
            keyFormat: ConfigFormat = sourceKeyFormat(format, source),
        ): Property =
            PropertyImpl(key, value, false, format, source,
                valueFormat = valueFormat, keyFormat = keyFormat)

        @JvmOverloads
        fun varProperty(
            key: String,
            value: String,
            format: ConfigFormat,
            source: DeclaringComponent?,
            valueFormat: ValueFormat = sourceValueFormat(format, source),
            keyFormat: ConfigFormat = sourceKeyFormat(format, source),
        ): Property =
            PropertyImpl(key, value, true, format, source,
                valueFormat = valueFormat, keyFormat = keyFormat)

        @JvmOverloads
        fun literalProperty(
            key: String,
            value: String,
            format: ConfigFormat,
            source: DeclaringComponent?,
            valueFormat: ValueFormat = sourceValueFormat(format, source, true),
            keyFormat: ConfigFormat = sourceKeyFormat(format, source),
        ): Property = PropertyImpl(key, value, false, format, source, true, valueFormat, keyFormat)

        internal fun sourceKeyFormat(format: ConfigFormat, source: DeclaringComponent?): ConfigFormat =
            when {
                source is FileBasedComponent && !source.isYaml -> ConfigFormat.PROPERTIES
                source == null && format == ConfigFormat.PROPERTIES -> ConfigFormat.PROPERTIES
                else -> ConfigFormat.YAML
            }

        // Legacy factories accept encoded Properties without a source; use TEXT for logical strings.
        // Readers specify the encoding explicitly.
        internal fun sourceValueFormat(
            format: ConfigFormat,
            source: DeclaringComponent?,
            literal: Boolean = false,
        ): ValueFormat =
            when {
                source is FileBasedComponent && !source.isYaml -> ValueFormat.PROPERTIES
                source == null && format == ConfigFormat.PROPERTIES && !literal -> ValueFormat.PROPERTIES
                format == ConfigFormat.YAML && !literal -> ValueFormat.YAML
                else -> ValueFormat.TEXT
            }

        fun groupProperty(
            key: String,
            children: Collection<Property>,
            source: DeclaringComponent?,
        ): Property = GroupedProperty(key, children, source)

        fun findSeparatorIndexIn(text: String): Int = text.indexOfFirst { it == ':' || it == '=' }

        fun isComment(line: String): Boolean = line.startsWith("#")

        fun isTempProperty(line: String): Boolean =
            line.startsWith(TEMP_VALUE) || line.startsWith(ONE_LINE_VAR)

        fun isDirective(line: String): Boolean = isComment(line) || isTempProperty(line)
    }
}
