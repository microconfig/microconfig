package io.microconfig

import io.microconfig.utils.Logger.error
import kotlin.system.exitProcess

class CommandLineParamParser(private val keyToValue: Map<String, String?>) {
    fun value(key: String): String? = valueOr(key, null)

    fun valueOr(key: String, defaultValue: String?): String? {
        val value = keyToValue[key]?.takeIf { it.isNotEmpty() } ?: return defaultValue
        return if (value.length >= 2 && value.startsWith('"') && value.endsWith('"'))
            value.substring(1, value.length - 1)
        else value
    }

    fun listValue(key: String): List<String> =
        value(key)
            .orEmpty()
            .split(',')
            .map { it.trim { character -> character <= ' ' } }
            .filter { it.isNotEmpty() }

    fun requiredValue(key: String, message: String): String =
        value(key) ?: printErrorAndExit(message)

    fun booleanValue(key: String): Boolean = value(key) == "true"

    fun contains(key: String): Boolean = key in keyToValue

    companion object {
        /** Parses key/value pairs and the valueless `-v` flag; malformed keys throw [IllegalArgumentException]. */
        fun parse(vararg args: String): CommandLineParamParser =
            parse(args) {}

        internal fun parse(
            args: Array<out String>,
            beforeValidation: (CommandLineParamParser) -> Unit,
        ): CommandLineParamParser {
            val (values, invalidKey) = parseArguments(args)
            val parsed = CommandLineParamParser(values)
            beforeValidation(parsed)
            require(invalidKey == null) { "key '$invalidKey' must start with -" }
            return parsed
        }

        private fun parseArguments(args: Array<out String>): Pair<Map<String, String?>, String?> {
            val values = linkedMapOf<String, String?>()
            var invalidKey: String? = null
            var index = 0
            while (index < args.size) {
                val key = args[index]
                if (key.startsWith('-')) {
                    val flag = key == "-v"
                    values[key.removePrefix("-")] = if (flag) null else args.getOrNull(index + 1)
                    index += if (flag) 1 else 2
                } else {
                    if (invalidKey == null) invalidKey = key
                    index++
                }
            }
            return values to invalidKey
        }

        fun printErrorAndExit(message: String): Nothing {
            error(message)
            exitProcess(-1)
        }
    }
}
