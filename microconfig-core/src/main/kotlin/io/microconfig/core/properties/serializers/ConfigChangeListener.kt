package io.microconfig.core.properties.serializers

import io.microconfig.core.properties.Property
import java.io.File
import java.util.function.BiConsumer

interface ConfigChangeListener : BiConsumer<File, Collection<Property>> {
    fun prepare(file: File, properties: Collection<Property>): () -> Unit

    fun prepareAll(outputs: Map<File, Collection<Property>>): Map<File, () -> Unit> =
        outputs.mapValues { (file, properties) -> prepare(file, properties) }

    fun prepareAll(
        outputs: Map<File, Collection<Property>>,
        componentDirectories: Map<File, File>,
    ): Map<File, () -> Unit> = prepareAll(outputs)

    override fun accept(file: File, properties: Collection<Property>) = prepare(file, properties)()
}
