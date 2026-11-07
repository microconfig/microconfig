package io.microconfig.core

import io.microconfig.io.FsReader
import io.microconfig.io.impl.DumpedFsReader
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.yaml.snakeyaml.Yaml

class SourceParseCacheTest {
    @TempDir lateinit var directory: Path
    private val reads = ConcurrentHashMap<File, Int>()
    private val root get() = directory.resolve("repo")
    private val destination get() = directory.resolve("output")
    private val reader = object : FsReader by DumpedFsReader() {
        override fun readFully(file: File): String {
            reads.merge(file.canonicalFile, 1, Int::plus)
            return Files.readString(file.toPath())
        }
    }

    @Test
    fun parseSharedNestedSourcesOncePerBuild() {
        write("envs/dev.yaml", "group:\n  components: [first, second]\n")
        write("components/first/application.yaml", "#include shared\nname: first\n")
        write("components/second/application.yaml", "#include shared\nname: second\n")
        write("components/shared/application.yaml", "#include leaf\nshared: common\n")
        write("components/leaf/application.yaml", "leaf: inherited\n")

        build()

        assertEquals(mapOf("name" to "first", "shared" to "common", "leaf" to "inherited"), output("first"))
        assertEquals(mapOf("name" to "second", "shared" to "common", "leaf" to "inherited"), output("second"))
        assertEquals(1, reads[root.resolve("components/shared/application.yaml").toFile().canonicalFile])
        assertEquals(1, reads[root.resolve("components/leaf/application.yaml").toFile().canonicalFile])
    }

    @Test
    fun resolveSharedIncludeComponentNameForEachRoot() {
        write("envs/dev.yaml", "group:\n  components: [first, second, third]\n")
        write("components/first/application.yaml", "#include shared\n")
        write("components/second/application.yaml", "#include shared\n")
        write("components/third/application.yaml", "#include shared\n")
        write("components/shared/application.yaml", "name: \${this@name}\n")

        build()

        assertEquals(mapOf("name" to "first"), output("first"))
        assertEquals(mapOf("name" to "second"), output("second"))
        assertEquals(mapOf("name" to "third"), output("third"))
        assertEquals(1, reads[root.resolve("components/shared/application.yaml").toFile().canonicalFile])
    }

    @Test
    fun keepIncludeTraversalSeparateForEachRoot() {
        write("envs/dev.yaml", "group:\n  components: [warmup, app]\n")
        write("components/warmup/application.yaml", "#include right\n")
        write("components/app/application.yaml", "#include left, right\n")
        write("components/left/application.yaml", "#include common\nvalue: left\n")
        write("components/right/application.yaml", "#include common\n")
        write("components/common/application.yaml", "value: common\n")

        build()

        assertEquals(mapOf("value" to "common"), output("warmup"))
        assertEquals(mapOf("value" to "left"), output("app"))
    }

    @Test
    fun keepDefaultIncludeEnvironmentForCrossEnvironmentReferences() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
        write("envs/prod.yaml", "group:\n  components: [app]\n")
        write("components/app/application.yaml", "#include common\nselected: \${app[prod]@value}\n")
        write("components/common/application.dev.yaml", "value: dev\n")
        write("components/common/application.prod.yaml", "value: prod\n")

        build()

        assertEquals(mapOf("selected" to "prod", "value" to "dev"), output("app"))
    }

    @Test
    fun retainSourceMetadataWhenTwoConfigTypesReadTheSameFile() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
        write("microconfig.yaml", """
            configTypes:
              - first:
                  sourceExtensions: [.yaml]
              - second:
                  sourceExtensions: [.yaml]
            """.trimIndent())
        write("components/app/application.yaml", "value: shared\n")
        val microconfig = Microconfig.searchConfigsIn(root.toFile()).withFsReader(reader)

        val properties = MicroconfigRunner(microconfig)
            .buildProperties("dev", emptyList(), emptyList()).asTypedProperties()

        assertEquals(listOf("first", "second"), properties.map { it.configType.typeName })
        properties.forEach {
            assertEquals(it.configType.typeName, it.properties.single().declaringComponent!!.configType)
            assertEquals("dev", it.properties.single().declaringComponent!!.environment)
        }
        assertEquals(2, reads[root.resolve("components/app/application.yaml").toFile().canonicalFile])
    }

    @Test
    fun readChangedSharedSourcesInTheNextBuild() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
        write("components/app/application.yaml", "#include shared\n")
        write("components/shared/application.yaml", "value: before\n")
        build()
        assertEquals(mapOf("value" to "before"), output("app"))

        write("components/shared/application.yaml", "value: after\n")
        build()

        assertEquals(mapOf("value" to "after"), output("app"))
        assertEquals(2, reads[root.resolve("components/shared/application.yaml").toFile().canonicalFile])
    }

    private fun write(path: String, text: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, text)
    }

    private fun build() {
        val microconfig = Microconfig.searchConfigsIn(root.toFile())
            .withDestinationDir(destination.toFile()).withFsReader(reader)
        MicroconfigRunner(microconfig).build("dev", emptyList(), emptyList())
    }

    private fun output(component: String): Map<*, *> =
        Yaml().load(Files.readString(destination.resolve("$component/application.yaml")))
}
