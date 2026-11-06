package io.microconfig.core.properties

interface Property {
    val key: String
    val value: String
    val isVar: Boolean
    val isLiteralText: Boolean
        get() = false

    val configFormat: ConfigFormat
    val declaringComponent: DeclaringComponent?

    fun resolveBy(resolver: Resolver, root: DeclaringComponent): Property
}
