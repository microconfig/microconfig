package io.microconfig.core.properties

import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.templates.Template

fun interface PropertySerializer<T> {
    fun prepare(properties: List<TypedProperties>): PropertySerializer<T> = this

    fun serialize(
        properties: Collection<Property>,
        templates: List<Template>,
        configType: ConfigType,
        componentName: String,
        environment: String,
    ): T
}
