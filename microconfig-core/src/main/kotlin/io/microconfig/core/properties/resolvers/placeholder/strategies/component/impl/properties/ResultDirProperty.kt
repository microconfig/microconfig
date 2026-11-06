package io.microconfig.core.properties.resolvers.placeholder.strategies.component.impl.properties

import io.microconfig.core.properties.resolvers.placeholder.strategies.component.ComponentProperty
import java.io.File
import java.util.Optional

class ResultDirProperty(private val destination: File) : ComponentProperty {
    override fun key(): String = "resultDir"

    override fun resolveFor(component: String, environment: String): Optional<String> =
        Optional.of(File(destination, component).absolutePath.replace('\\', '/'))
}
