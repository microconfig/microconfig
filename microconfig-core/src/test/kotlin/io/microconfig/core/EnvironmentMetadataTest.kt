package io.microconfig.core

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class EnvironmentMetadataTest {
    @TempDir lateinit var root: Path

    @Test
    fun treatIpAsAnOrdinaryGroupNameAndUseDefaultForMissingProperty() {
        write("envs/dev.yaml", "ip:\n  components: [app]\n")
        write("components/app/application.yaml",
            "value: \${this@ip:fallback}\ngroup: \${this@group}\norder: \${this@order}\n")

        val properties = build()

        assertEquals("fallback", properties["value"])
        assertEquals("ip", properties["group"])
        assertEquals("0", properties["order"])
    }

    @Test
    fun resolveAndOverrideIpAsAnOrdinaryIncludedProperty() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
        write("components/shared/application.yaml", "ip: base\nurl: \${this@ip}\n")
        write("components/app/application.yaml", "#include shared\n+dev:\n  ip: configured\n")

        assertEquals(mapOf("ip" to "configured", "url" to "configured"), build())
    }

    private fun build() = MicroconfigRunner(root.toFile(), root.resolve("out").toFile())
        .buildProperties("dev", emptyList(), emptyList()).propertiesAsKeyValue

    private fun write(name: String, content: String) {
        val file = root.resolve(name)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }
}
