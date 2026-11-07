package io.microconfig.core

import io.microconfig.core.configtypes.ConfigTypeFilters.eachConfigType
import io.microconfig.core.properties.Properties
import io.microconfig.core.properties.PropertySerializer
import io.microconfig.core.properties.serializers.PropertySerializers.toFileIn
import io.microconfig.core.properties.serializers.PropertySerializers.withConfigDiff
import io.microconfig.core.properties.templates.impl.TemplatesService.Companion.resolveTemplatesBy
import java.io.File

class MicroconfigRunner(val microconfig: Microconfig) {
    constructor(
        rootDir: File,
        destinationDir: File,
    ) : this(Microconfig.searchConfigsIn(rootDir).withDestinationDir(destinationDir))

    /** Starts the instance's only build attempt, even if resolution later fails. */
    fun buildProperties(env: String, groups: List<String>, services: List<String>): Properties {
        microconfig.startBuild()
        return microconfig
            .inEnvironment(env)
            .findComponentsFrom(groups, services)
            .getPropertiesFor(eachConfigType())
            .resolveBy(microconfig.resolver())
            .forEachComponent(resolveTemplatesBy(microconfig.resolver()))
    }

    fun build(env: String, groups: List<String>, services: List<String>) {
        buildProperties(env, groups, services).save(toFiles())
    }

    fun toFiles(): PropertySerializer<File> {
        val configIo = microconfig.dependencies().configIo
        return toFileIn(microconfig.destinationDir(), withConfigDiff(configIo), configIo)
    }
}
