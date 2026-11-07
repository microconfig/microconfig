package io.microconfig.core.properties.templates

import java.util.regex.Pattern

data class TemplatePattern(val templatePrefixes: List<String>, val placeholderPattern: Pattern) {
    fun startsWithTemplatePrefix(key: String): Boolean = templatePrefixes.any(key::startsWith)

    fun extractTemplateType(value: String): String = value.substringAfter('.').substringBefore('.')

    fun extractTemplateName(value: String): String =
        templatePrefixes.firstOrNull(value::startsWith)?.let {
            value.removePrefix(it).substringBefore('.')
        } ?: throw IllegalArgumentException("Incorrect template $value")

    fun withTemplatePrefixes(prefixes: List<String>): TemplatePattern =
        copy(templatePrefixes = prefixes)

    fun withPlaceholderPattern(pattern: Pattern): TemplatePattern =
        copy(placeholderPattern = pattern)

    class Builder {
        private var prefixes: List<String> = emptyList()
        private var pattern: Pattern = DEFAULT_PLACEHOLDER_PATTERN

        fun templatePrefixes(prefixes: List<String>): Builder = apply { this.prefixes = prefixes }

        fun placeholderPattern(pattern: Pattern): Builder = apply { this.pattern = pattern }

        fun build(): TemplatePattern = TemplatePattern(prefixes, pattern)
    }

    companion object {
        @JvmField
        val DEFAULT_PLACEHOLDER_PATTERN: Pattern =
            Pattern.compile(
                "(?<escaped>\\\\)?" +
                    // Nested defaults are extended to their closing brace by StringTemplate.
                    "(?<placeholder>\\$\\{(?<name>.+?)(?::(?<defuvalue>[^}]*))??})"
            )

        fun defaultPattern(): TemplatePattern =
            TemplatePattern(
                listOf(
                    "microconfig.template.",
                    "microconfig.mustache.",
                    "microconfig.file.",
                    "mc.template.",
                    "mc.mustache.",
                    "mc.file.",
                ),
                DEFAULT_PLACEHOLDER_PATTERN,
            )

        fun builder(): Builder = Builder()
    }
}
