package io.microconfig.core.properties.repository

class Includes private constructor(private val line: String) {
    fun withDefaultEnv(defaultEnv: String): List<Include> =
        try {
            parseIncludes(defaultEnv)
        } catch (error: RuntimeException) {
            throw IllegalArgumentException(
                "Can't parse include '$line'. Supported format: #include component[optionalEnv], component2",
                error,
            )
        }

    private fun parseIncludes(defaultEnv: String): List<Include> {
        val components =
            line
                .removePrefix(PREFIX)
                .split(',')
                .map { it.trim { character -> character <= ' ' } }
                .filter { it.isNotEmpty() }
        require(components.isNotEmpty()) { "Include must contain component names." }
        return components.map { Include.parse(it, defaultEnv) }
    }

    companion object {
        private const val PREFIX = "#include "

        fun isInclude(line: String): Boolean = line.startsWith(PREFIX)

        fun from(line: String): Includes = Includes(line)
    }
}
