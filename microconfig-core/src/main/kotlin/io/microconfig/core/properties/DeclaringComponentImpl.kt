package io.microconfig.core.properties

data class DeclaringComponentImpl(
    override val configType: String,
    override val component: String,
    override val environment: String,
) : DeclaringComponent {
    override fun toString(): String = "$component[$environment]"

    companion object {
        fun copyOf(component: DeclaringComponent): DeclaringComponent =
            component as? DeclaringComponentImpl
                ?: DeclaringComponentImpl(
                    component.configType,
                    component.component,
                    component.environment,
                )
    }
}
