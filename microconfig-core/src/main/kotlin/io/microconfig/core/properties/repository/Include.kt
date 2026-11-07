package io.microconfig.core.properties.repository

data class Include(val component: String, val environment: String) {
    override fun toString(): String = "#include $component[$environment]"

    companion object {
        private val pattern = Regex("^(?<comp>[\\p{L}\\p{Nd}._-]+)(\\[(?<env>.+)])?$")

        fun parse(definition: String, defaultEnv: String): Include {
            val match =
                pattern.matchEntire(definition)
                    ?: throw IllegalArgumentException(
                        "Can't parse include's component: $definition"
                    )
            return Include(match.groups["comp"]!!.value, match.groups["env"]?.value ?: defaultEnv)
        }
    }
}
