package io.microconfig.utils

object ConsoleColor {
    fun green(message: String): String = applyColor(message, "\u001B[32m")

    fun red(message: String): String = applyColor(message, "\u001B[31m")

    fun yellow(message: String): String = applyColor(message, "\u001B[33m")

    private fun applyColor(message: String, color: String): String =
        if (message.isEmpty()) message else "$color$message\u001B[0m"
}
