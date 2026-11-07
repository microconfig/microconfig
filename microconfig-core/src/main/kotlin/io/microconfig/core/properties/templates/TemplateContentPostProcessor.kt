package io.microconfig.core.properties.templates

import io.microconfig.core.properties.TypedProperties
import java.io.File
import java.util.function.UnaryOperator

fun interface TemplateContentPostProcessor {
    fun process(
        templateType: String,
        source: File,
        templateContent: String,
        properties: TypedProperties,
    ): String

    fun process(
        templateType: String,
        source: File,
        templateContent: String,
        properties: TypedProperties,
        resolvePlaceholders: UnaryOperator<String>,
    ): String = process(templateType, source, templateContent, properties)
}
