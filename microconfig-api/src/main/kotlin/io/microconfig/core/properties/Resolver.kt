package io.microconfig.core.properties

fun interface Resolver {
    fun resolve(
        value: String,
        sourceOfValue: DeclaringComponent?,
        root: DeclaringComponent?,
    ): String

    fun resolve(property: Property, root: DeclaringComponent): String =
        resolve(property.value, property.declaringComponent, root)
}
