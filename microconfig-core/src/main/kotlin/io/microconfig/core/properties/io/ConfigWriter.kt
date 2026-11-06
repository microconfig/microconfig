package io.microconfig.core.properties.io

import io.microconfig.core.properties.Property

interface ConfigWriter {
    fun write(properties: Map<String, String>)

    fun write(properties: Collection<Property>)

    fun serialize(properties: Collection<Property>): String

    fun serializedValue(property: Property): String = property.value
}
