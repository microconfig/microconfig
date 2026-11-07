package io.microconfig.core.properties

/**
 * Environment overrides and temporary-property blocks retain their scope until repository merging.
 */
@ConsistentCopyVisibility
data class OverrideProperty
private constructor(
    val environments: List<String>,
    private val delegate: Property,
) : Property by delegate {
    fun isForAllEnvs(): Boolean = environments.isEmpty()

    fun isOnlyFor(environment: String): Boolean =
        environments.size == 1 && environment in environments

    fun isSharedWith(environment: String): Boolean =
        environments.size > 1 && environment in environments

    companion object {
        private const val MULTILINE_VAR = ".var"
        private const val MULTILINE_VAR_PREFIX = ".var."

        fun isOverrideProperty(key: String): Boolean =
            isEnvBlock(key) || isMultilineVar(key) || key == MULTILINE_VAR

        fun isEnvBlock(key: String): Boolean = key.startsWith("+")

        @JvmOverloads
        fun overrideProperty(
            key: String,
            value: String,
            format: ConfigFormat,
            source: DeclaringComponent?,
            literalText: Boolean = false,
            valueFormat: ValueFormat = PropertyImpl.sourceValueFormat(format, source, literalText),
            keyFormat: ConfigFormat = PropertyImpl.sourceKeyFormat(format, source),
        ): Property {
            val end = envsEnd(key)
            require(end != key.length) { invalidOverride(propertyMissing(key), key, source) }
            val environments = extractEnvs(key, end, source)
            val propertyKey = key.substring(end + 1)
            require(propertyKey != MULTILINE_VAR) {
                invalidOverride(propertyMissing(propertyKey), key, source)
            }
            val nestedVar = isMultilineVar(propertyKey)
            val adjustedKey =
                if (nestedVar) propertyKey.removePrefix(MULTILINE_VAR_PREFIX) else propertyKey
            return OverrideProperty(
                environments,
                PropertyImpl(
                    adjustedKey,
                    value,
                    isMultilineVar(key) || nestedVar,
                    format,
                    source,
                    literalText,
                    valueFormat,
                    keyFormat,
                ),
            )
        }

        private fun envsEnd(key: String): Int = key.indexOf('.', 1).takeIf { it >= 0 } ?: key.length

        private fun propertyMissing(key: String): String =
            if (key == MULTILINE_VAR) "must be a block of temp properties"
            else "must contain a property, like '+env.key'"

        private fun invalidOverride(
            problem: String,
            key: String,
            source: DeclaringComponent?,
        ): String = "Override '$key' $problem in $source"

        private fun extractEnvs(key: String, end: Int, source: DeclaringComponent?): List<String> {
            if (!isEnvBlock(key)) return emptyList()
            val environments =
                key.substring(1, end).split(',').map { it.trim { character -> character <= ' ' } }
            checkEnvNames(environments, key, source)
            return environments.distinct()
        }

        private fun checkEnvNames(
            environments: List<String>,
            key: String,
            source: DeclaringComponent?,
        ) {
            require(environments.none(::isEnvBlock)) {
                invalidOverride(
                    "must have '+' only once, before the first env, like '+prod-a, prod-b'",
                    key,
                    source,
                )
            }
            require(environments.size <= 1 || "" !in environments) {
                invalidOverride("has an empty env name in the env list", key, source)
            }
        }

        private fun isMultilineVar(key: String): Boolean = key.startsWith(MULTILINE_VAR_PREFIX)

        fun envVarProperty(
            header: String,
            line: String,
            format: ConfigFormat,
            source: DeclaringComponent?,
        ): Property =
            OverrideProperty(
                extractEnvs(header, envsEnd(header), source),
                PropertyImpl.parse(line, format, source),
            )

        fun isEmptyBlock(key: String, value: String, source: DeclaringComponent?): Boolean {
            if (value.isNotEmpty()) return false
            val end = envsEnd(key)
            val empty = end == key.length || key.substring(end + 1) == MULTILINE_VAR
            if (empty) extractEnvs(key, end, source)
            return empty
        }

        fun checkMultilineVarPosition(key: String, source: DeclaringComponent?) {
            val rest = withoutAllowedMultilineVar(key) + "."
            require(
                !rest.contains(".$MULTILINE_VAR_PREFIX") && !rest.startsWith(MULTILINE_VAR_PREFIX)
            ) {
                "'.var:' block must be at the top level or directly inside a '+env:' block, but key '$key' has it deeper in $source"
            }
        }

        private fun withoutAllowedMultilineVar(key: String): String {
            val propertyKey =
                if (isEnvBlock(key)) key.substring((envsEnd(key) + 1).coerceAtMost(key.length))
                else key
            return if (propertyKey == MULTILINE_VAR) ""
            else propertyKey.removePrefix(MULTILINE_VAR_PREFIX)
        }
    }
}
