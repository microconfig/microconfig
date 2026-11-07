package io.microconfig.core.properties.resolvers.placeholder.strategies.component.impl.properties

import io.microconfig.core.properties.resolvers.placeholder.strategies.component.ComponentProperty
import java.io.File
import java.util.*

class ConfigRootDirProperty(root: File) : ComponentProperty {
    private val rootPath = root.absolutePath.replace('\\', '/')

    override fun key(): String = "configRoot"

    override fun resolveFor(component: String, environment: String): Optional<String> =
        Optional.of(rootPath)
}
