package io.microconfig.core.properties.repository

import io.microconfig.core.configtypes.ConfigType
import java.io.File
import java.util.*

interface ComponentGraph {
    /** Files are ordered from the lowest to the highest override priority. */
    fun getConfigFilesOf(
        component: String,
        environment: String,
        configType: ConfigType,
    ): List<ConfigFile>

    fun getFolderOf(component: String): Optional<File>
}
