package io.microconfig.core.properties.resolvers.placeholder.strategies.system.impl

import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.PlaceholderResolveStrategy
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.utils.Os
import java.util.Optional
import java.util.function.UnaryOperator

class SystemResolveStrategy(
    private val type: String,
    private val resolver: UnaryOperator<String>,
    private val windows: Boolean = Os.isWindows(),
) : PlaceholderResolveStrategy {
    override fun resolve(
        component: String,
        key: String,
        environment: String,
        configType: String,
        root: String?,
    ): Optional<Property> {
        if (type != component) return Optional.empty()
        return Optional.ofNullable(resolver.apply(key))
            .map { normalizeUserHome(it, key) }
            .map {
                PropertyImpl.property(
                    key,
                    it,
                    PROPERTIES,
                    DeclaringComponentImpl(configType, component, environment),
                )
            }
    }

    private fun normalizeUserHome(value: String, key: String): String =
        if (windows && key == "user.home") value.replace('\\', '/') else value

    companion object {
        const val ENV_OS_SOURCE = "env"
        const val SYSTEM_SOURCE = "system"

        fun systemPropertiesResolveStrategy(): PlaceholderResolveStrategy =
            SystemResolveStrategy(SYSTEM_SOURCE, System::getProperty)

        fun envVariablesResolveStrategy(): PlaceholderResolveStrategy =
            SystemResolveStrategy(ENV_OS_SOURCE, System::getenv)
    }
}
