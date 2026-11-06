package io.microconfig.core.configtypes.impl

import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.configtypes.ConfigTypeRepository
import io.microconfig.io.FsReader
import io.microconfig.utils.Logger.announce
import java.io.File
import org.yaml.snakeyaml.Yaml

class CustomConfigTypeRepository(private val fsReader: FsReader, private val descriptorFile: File) :
    ConfigTypeRepository {
    override val configTypes: List<ConfigType>
        get() {
            if (!descriptorFile.exists()) return emptyList()
            val types = parseConfigTypes()
            if (types.isNotEmpty()) announce("Using settings from $descriptorFile")
            return types
        }

    private fun parseConfigTypes(): List<ConfigType> =
        try {
            val descriptor = Yaml().load<Map<String, Any?>>(fsReader.readFully(descriptorFile))
            val types = descriptor?.get("configTypes") as? List<*> ?: emptyList<Any>()
            types.map(::parseType)
        } catch (error: RuntimeException) {
            throw IllegalStateException("Can't parse descriptor: $descriptorFile", error)
        }

    private fun parseType(value: Any?): ConfigType {
        if (value is String) return ConfigTypeImpl.byName(value)
        val definition = value as Map<*, *>
        val name = definition.keys.first() as String
        val attributes = definition[name] as? Map<*, *> ?: emptyMap<Any, Any>()
        val extensions =
            (attributes["sourceExtensions"] as? List<*>)?.map { it as String }?.toSet()
                ?: setOf(".$name")
        val resultFile = attributes["resultFileName"] as? String ?: name
        return ConfigTypeImpl.byNameAndExtensions(name, extensions, resultFile)
    }

    companion object {
        fun findDescriptorIn(rootDir: File, fsReader: FsReader): ConfigTypeRepository =
            CustomConfigTypeRepository(fsReader, File(rootDir, "microconfig.yaml"))
    }
}
