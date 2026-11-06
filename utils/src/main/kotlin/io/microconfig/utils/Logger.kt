package io.microconfig.utils

import java.io.PrintWriter
import java.io.StringWriter

object Logger {
    @Volatile private var enabled = true
    @Volatile private var errorOccurred = false

    fun error(error: Throwable?) {
        if (error != null) error(null, error)
    }

    fun error(message: String?, error: Throwable?) {
        val output = StringWriter()
        PrintWriter(output).use { writer ->
            if (message != null) writer.println(message)
            error?.printStackTrace(writer)
        }
        error(output.toString())
    }

    fun warn(message: String) = info(ConsoleColor.yellow(message))

    fun announce(message: String) = info(ConsoleColor.green(message))

    fun info(message: String) {
        if (enabled) println(message)
    }

    fun error(message: String) {
        errorOccurred = true
        println(ConsoleColor.red(message))
    }

    fun isErrorOccurred(): Boolean = errorOccurred

    fun enableLogger(enabled: Boolean) {
        this.enabled = enabled
    }
}
