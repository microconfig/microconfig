package io.microconfig.core.configtypes

fun interface ConfigTypeFilter {
    fun selectTypes(supportedTypes: List<ConfigType>): List<ConfigType>
}
