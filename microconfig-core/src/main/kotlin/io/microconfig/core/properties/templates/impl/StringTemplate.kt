package io.microconfig.core.properties.templates.impl

import io.microconfig.core.properties.*
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.PropertyImpl.Companion.literalProperty
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders
import io.microconfig.core.properties.templates.TemplateContentPostProcessor
import io.microconfig.core.templates.Template
import io.microconfig.utils.IoUtils.readFully
import java.io.File
import java.util.function.UnaryOperator
import java.util.regex.Pattern

class StringTemplate
@JvmOverloads
constructor(
    private val templateName: String,
    override val source: File,
    override val destination: File,
    private val pattern: Pattern,
    override val content: String = readFully(source),
) : Template {
    override val fileName: String
        get() = destination.name

    override val contentAsBytes: ByteArray
        get() = content.toByteArray(Charsets.UTF_8)

    fun resolveBy(resolver: Resolver, component: DeclaringComponent): StringTemplate =
        withContent(replacePlaceholders(resolver, component))

    private fun replacePlaceholders(resolver: Resolver, component: DeclaringComponent): String {
        val matcher = pattern.matcher(content)
        val output = StringBuilder()
        var copied = 0
        while (matcher.find()) {
            val start = matcher.start("placeholder")
            val end = placeholderEnd(start, matcher.end())
            val placeholder = content.substring(start, end)
            output.append(content, copied, matcher.start())
            if (matcher.group("escaped") != null) output.append(placeholder)
            else {
                val replacement = resolve(placeholder, component, resolver)
                output.append(
                    if (replacement == null) content.substring(matcher.start(), end)
                    else indentMultilineValue(replacement, output)
                )
            }
            copied = end
            if (end > matcher.end()) matcher.region(end, content.length)
        }
        return output.append(content, copied, content.length).toString()
    }

    private fun placeholderEnd(start: Int, fallback: Int): Int =
        PlaceholderBorders.endOfPlaceholder(content, start).takeIf { it >= 0 } ?: fallback

    private fun resolve(
        placeholder: String,
        component: DeclaringComponent,
        resolver: Resolver,
    ): String? {
        if (placeholder == "\${this@templateName}") return templateNameWithoutBrackets()
        if (isValidPlaceholder(placeholder))
            return resolveValue(placeholder, component, resolver)
        val qualified = "\${this@" + placeholder.substring(2)
        if (!isValidPlaceholder(qualified)) return resolveNestedHeader(placeholder, component, resolver)
        return try {
            resolveValue(qualified, component, resolver)
        } catch (error: RuntimeException) {
            if (error.isUnresolvedValue()) null else throw error
        }
    }

    private fun resolveValue(value: String, component: DeclaringComponent, resolver: Resolver): String =
        resolver.resolve(literalProperty(templateName, value, YAML, component, ValueFormat.TEXT), component)

    private fun resolveNestedHeader(
        placeholder: String,
        component: DeclaringComponent,
        resolver: Resolver,
    ): String? {
        if (PlaceholderBorders.endOfPlaceholder(placeholder, 0) != placeholder.length) return null
        if (PlaceholderBorders.findPlaceholderOrNull(placeholder) == null) return null
        val resolved = resolveValue(placeholder, component, resolver)
        if (resolved == placeholder) return null
        return if (PlaceholderBorders.endOfPlaceholder(resolved, 0) == resolved.length)
            resolve(resolved, component, resolver) ?: resolved
        else resolved
    }

    private fun indentMultilineValue(value: String, output: StringBuilder): String {
        if ('\n' !in value && '\r' !in value) return value
        val lineStart = output.lastIndexOfAny(charArrayOf('\n', '\r')) + 1
        val indentation = " ".repeat(output.length - lineStart)
        return lineBreak.replace(value) { it.value + indentation }
    }

    fun withContent(content: String): StringTemplate =
        StringTemplate(templateName, source, destination, pattern, content)

    fun templateNameWithoutBrackets(): String = templateName.replaceFirst(bracketsSuffix, "")

    @JvmOverloads
    fun postProcessContent(
        processor: TemplateContentPostProcessor,
        type: String,
        properties: TypedProperties,
        resolvePlaceholders: UnaryOperator<String> = UnaryOperator.identity(),
    ): StringTemplate = withContent(processor.process(type, source, content, properties, resolvePlaceholders))

    companion object {
        private val lineBreak = Regex("\\r\\n|[\\n\\r]")
        private val bracketsSuffix = Regex("\\[.+]$")

        fun isValidPlaceholder(value: String): Boolean {
            val placeholder = PlaceholderBorders.findPlaceholderOrNull(value)
            return placeholder?.startIndex == 0 && placeholder.endIndex == value.length
        }
    }
}
