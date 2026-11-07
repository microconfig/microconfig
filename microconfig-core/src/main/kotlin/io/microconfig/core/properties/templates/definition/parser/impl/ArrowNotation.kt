package io.microconfig.core.properties.templates.definition.parser.impl

import io.microconfig.core.properties.Property

internal object ArrowNotation {
    fun pathsIn(property: Property): Pair<String, String>? =
        splitPaths(property, property.value.trim { it <= ' ' })

    private fun splitPaths(property: Property, value: String): Pair<String, String>? {
        val parts = value.split(" -> ")
        if (parts.size == 1) {
            validateMissingPaths(property, value)
            return null
        }
        require(parts.size == 2) { problem(property, "Expected one ' -> ' separator") }
        return parts[0] to parts[1]
    }

    private fun validateMissingPaths(property: Property, value: String) {
        require(value != "->" && !value.startsWith("-> ")) {
            problem(property, "Missing template source path")
        }
        require(!value.endsWith(" ->")) {
            problem(property, "Missing template destination path")
        }
    }

    private fun problem(property: Property, message: String): String =
        "$message in '${property.key}' at '${property.declaringComponent}'"
}
