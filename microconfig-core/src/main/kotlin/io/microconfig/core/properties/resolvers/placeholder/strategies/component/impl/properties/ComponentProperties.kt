package io.microconfig.core.properties.resolvers.placeholder.strategies.component.impl.properties

import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.properties.repository.ComponentGraph
import io.microconfig.core.properties.resolvers.placeholder.strategies.component.ComponentProperty
import java.io.File

class ComponentProperties(
    private val graph: ComponentGraph,
    private val environments: EnvironmentRepository,
    private val root: File,
    private val destination: File,
) {
    fun get(): Map<String, ComponentProperty> =
        listOf(
            NameProperty(),
            ConfigDirProperty(graph, environments),
            ResultDirProperty(destination),
            ConfigRootDirProperty(root),
        )
            .associateBy { it.key() }
}
