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
            )
        }

        fun property(
            key: String,
            value: String,
            format: ConfigFormat,
            source: DeclaringComponent?,
        ): Property = PropertyImpl(key, value, false, format, source)

        fun varProperty(
            key: String,
            value: String,
            format: ConfigFormat,
            source: DeclaringComponent?,
        ): Property = PropertyImpl(key, value, true, format, source)

        fun literalProperty(
            key: String,
            value: String,
            format: ConfigFormat,
            source: DeclaringComponent?,
        ): Property = PropertyImpl(key, value, false, format, source, true)

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
