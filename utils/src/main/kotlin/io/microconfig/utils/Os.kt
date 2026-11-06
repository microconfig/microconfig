package io.microconfig.utils

object Os {
    private val windows = System.getProperty("os.name", "").startsWith("Win")

    fun isWindows(): Boolean = windows
}
