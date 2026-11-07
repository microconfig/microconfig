package io.microconfig.core.properties

import java.util.*

fun interface PlaceholderResolveStrategy {
    fun resolve(
        component: String,
        key: String,
        environment: String,
        configType: String,
        root: String?,
    ): Optional<Property>
}
