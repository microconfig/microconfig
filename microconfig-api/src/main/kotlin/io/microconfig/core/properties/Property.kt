package io.microconfig.core.properties

interface Property {
    val key: String
    val value: String

    /** Syntax of [key], independent from the representation of [value]. */
    val keyFormat: ConfigFormat
        get() = configFormat
    val isVar: Boolean
    val isLiteralText: Boolean
        get() = false

    val valueFormat: ValueFormat
        get() = if (configFormat == ConfigFormat.YAML && !isLiteralText) ValueFormat.YAML else ValueFormat.TEXT

    val configFormat: ConfigFormat
    val declaringComponent: DeclaringComponent?

    fun resolveBy(resolver: Resolver, root: DeclaringComponent): Property
}
