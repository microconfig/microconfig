package io.microconfig.core.properties.templates.impl

import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.TypedProperties
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders
import io.microconfig.core.properties.templates.TemplateContentPostProcessor
import io.microconfig.core.templates.Template
import io.microconfig.utils.IoUtils.readFully
import java.io.File
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
            val replacement =
                if (matcher.group("escaped") != null) placeholder
                else resolve(placeholder, component, resolver)
            output.append(
                if (replacement == null) content.substring(matcher.start(), end)
                else indentMultilineValue(replacement, output)
            )
            copied = end
            if (end > matcher.end()) matcher.region(end, content.length)
        }
        return output.append(content, copied, content.length).toString()
    }

    private fun placeholderEnd(start: Int, fallback: Int): Int {
        val full = PlaceholderBorders.findPlaceholderIn(content, start).orElse(null)
        if (full?.startIndex == start) return full.endIndex
        val qualified = "\${this@" + content.substring(start + 2)
        val short = PlaceholderBorders.findPlaceholderIn(qualified).orElse(null)
        return if (short?.startIndex == 0) start + short.endIndex - "this@".length else fallback
    }

    private fun resolve(
        placeholder: String,
        component: DeclaringComponent,
        resolver: Resolver,
    ): String? {
        if (placeholder == "\${this@templateName}") return templateNameWithoutBrackets()
        if (isValidPlaceholder(placeholder))
            return resolver.resolve(placeholder, component, component)
        val qualified = "\${this@" + placeholder.substring(2)
        if (!isValidPlaceholder(qualified)) return null
        return try {
            resolver.resolve(qualified, component, component)
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun indentMultilineValue(value: String, output: StringBuilder): String {
        val lineStart = output.lastIndexOfAny(charArrayOf('\n', '\r')) + 1
        val indentation = " ".repeat(output.length - lineStart)
        return lineBreak.replace(value) { it.value + indentation }
    }

    fun withContent(content: String): StringTemplate =
        StringTemplate(templateName, source, destination, pattern, content)

    fun templateNameWithoutBrackets(): String = templateName.replaceFirst(Regex("\\[.+]$"), "")

    fun postProcessContent(
        processor: TemplateContentPostProcessor,
        type: String,
        properties: TypedProperties,
    ): StringTemplate = withContent(processor.process(type, source, content, properties))

    companion object {
        private val lineBreak = Regex("\\r\\n|[\\n\\r]")

        fun isValidPlaceholder(value: String): Boolean {
            val placeholder = PlaceholderBorders.findPlaceholderIn(value).orElse(null)
            return placeholder?.startIndex == 0 && placeholder.endIndex == value.length
        }
    }
}
