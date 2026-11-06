package io.microconfig.core.properties.templates

import io.microconfig.core.properties.TypedProperties
import java.io.File

fun interface TemplateContentPostProcessor {
    fun process(
        templateType: String,
        source: File,
        templateContent: String,
        properties: TypedProperties,
    ): String
}
