package io.microconfig.core.configtypes

interface ConfigType {
    fun getName(): String

    val sourceExtensions: Set<String>
    val resultFileName: String
}

// Keeps enum configuration names distinct from Enum.name while exposing a Kotlin property.
val ConfigType.name: String
    get() = getName()
