package io.microconfig.core.properties.resolvers.placeholder.strategies.component.impl.properties

import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.properties.repository.ComponentGraph
import io.microconfig.core.properties.resolvers.placeholder.strategies.component.ComponentProperty
import java.util.*
import kotlin.jvm.optionals.getOrNull

class ConfigDirProperty(
    private val graph: ComponentGraph,
    private val environments: EnvironmentRepository,
) : ComponentProperty {
    override fun key(): String = "configDir"

    override fun resolveFor(component: String, environment: String): Optional<String> {
        val original =
            environments
                .getOrCreateByName(environment)
                .findComponentWithName(component)
                .originalName
        return Optional.ofNullable(graph.getFolderOf(original).getOrNull()?.absolutePath?.replace('\\', '/'))
    }
}
