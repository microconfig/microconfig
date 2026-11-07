package io.microconfig.core.properties.serializers

import io.microconfig.core.properties.Property
import java.io.File
import java.util.function.BiConsumer

interface ConfigChangeListener : BiConsumer<File, Collection<Property>> {
    fun prepare(file: File, properties: Collection<Property>): () -> Unit

    override fun accept(file: File, properties: Collection<Property>) = prepare(file, properties)()
}
