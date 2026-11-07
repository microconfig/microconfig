package io.microconfig.core.configtypes

interface ConfigType {
    val typeName: String
    val sourceExtensions: Set<String>
    val resultFileName: String
}
