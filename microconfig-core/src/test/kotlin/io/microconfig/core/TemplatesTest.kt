package io.microconfig.core

import io.microconfig.core.ClasspathReader.Companion.classpathFile
import io.microconfig.core.Microconfig.Companion.searchConfigsIn
import io.microconfig.core.configtypes.ConfigTypeFilters.configType
import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.properties.templates.impl.TemplatesService.Companion.resolveTemplatesBy
import io.microconfig.utils.IoUtils.readFully
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class TemplatesTest {
    @TempDir lateinit var destinationDir: File

    @Test
    fun testMustache() {
        val root: File = classpathFile("templates")
        val microconfig: Microconfig = searchConfigsIn(root).withDestinationDir(destinationDir)
        val runner: MicroconfigRunner = MicroconfigRunner(root, destinationDir)
        microconfig
            .environments()
            .getOrCreateByName("dev")
            .findComponentWithName("mustache")
            .getPropertiesFor(configType(APPLICATION))
            .resolveBy(microconfig.resolver())
            .forEachComponent(resolveTemplatesBy(microconfig.resolver()))
            .save(runner.toFiles())
        assertEquals(
            readFully(File(root, "components/mustache/expect.dev")).replace("\r\n", "\n").trim(),
            readFully(File(destinationDir, "mustache/template.yaml")).replace("\r\n", "\n").trim(),
        )
    }
}
