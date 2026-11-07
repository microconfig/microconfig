package io.microconfig.core.properties.resolvers.expression.functions.impl

object CustomSystemApi {
    @JvmStatic
    fun currentTimeMillis(): Long = System.currentTimeMillis()

    @JvmStatic
    fun lineSeparator(): String = System.lineSeparator()
}
