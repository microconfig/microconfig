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
        fun parse(vararg args: String): CommandLineParamParser =
            CommandLineParamParser(parseArguments(args))

        private fun parseArguments(args: Array<out String>): Map<String, String?> = buildMap {
            args.indices.step(2).forEach { index ->
                val key = args[index]
                if (!key.startsWith('-')) printErrorAndExit("key '$key' must start with -")
                put(key.substring(1), args.getOrNull(index + 1))
            }
        }

        fun printErrorAndExit(message: String): Nothing {
            error(message)
            exitProcess(-1)
        }
    }
}
