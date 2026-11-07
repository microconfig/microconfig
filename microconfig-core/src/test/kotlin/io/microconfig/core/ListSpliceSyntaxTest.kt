package io.microconfig.core

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml

class ListSpliceSyntaxTest {
    @TempDir lateinit var directory: Path

    @ParameterizedTest(name = "{0}, empty={1}, last={2}, crlf={3}")
    @MethodSource("blockScalarSplices")
    fun spliceListsImmediatelyAfterBlockScalars(
        style: String,
        empty: Boolean,
        last: Boolean,
        crlf: Boolean,
    ) {
        val root = directory.resolve("repo")
        Files.createDirectories(root.resolve("envs"))
        Files.createDirectories(root.resolve("components/app"))
        Files.writeString(root.resolve("envs/dev.yaml"), "group:\n  components: [app]\n")
        val content = "label: emoji😀\nbase: [one, two]\nlist:\n  - $style\n" +
            (if (empty) "" else "    hello\n    world\n") +
            "  \${this@base}\n" + (if (last) "" else "  - after\n")
        Files.writeString(root.resolve("components/app/application.yaml"),
            if (crlf) content.replace("\n", "\r\n") else content)
        val output = directory.resolve("output")

        MicroconfigRunner(root.toFile(), output.toFile()).build("dev", emptyList(), emptyList())

        val result = Yaml().load<Map<String, Any>>(Files.readString(output.resolve("app/application.yaml")))
        val text = if (empty) "" else if (style == "|") "hello\nworld\n" else "hello world\n"
        assertEquals("emoji😀", result["label"])
        assertEquals(listOf(text, "one", "two") + if (last) emptyList() else listOf("after"), result["list"])
    }

    @ParameterizedTest
    @ValueSource(strings = ["text:\n  hello", "text: hello"])
    fun keepPlainScalarContinuationsWhenAnotherFieldNeedsListAdaptation(prefix: String) {
        val root = directory.resolve("repo")
        Files.createDirectories(root.resolve("envs"))
        Files.createDirectories(root.resolve("components/app"))
        Files.writeString(root.resolve("envs/dev.yaml"), "group:\n  components: [app]\n")
        Files.writeString(root.resolve("components/app/application.yaml"),
            "base: [one, two]\n$prefix\n  \${this@base}\n  - literal\n" +
                "list:\n  - before\n  \${this@base}\n  - next\n")
        val output = directory.resolve("output")

        MicroconfigRunner(root.toFile(), output.toFile()).build("dev", emptyList(), emptyList())

        val result = Yaml().load<Map<String, Any>>(Files.readString(output.resolve("app/application.yaml")))
        assertEquals("hello [one, two] - literal", result["text"])
        assertEquals(listOf("before", "one", "two", "next"), result["list"])
    }

    @ParameterizedTest
    @ValueSource(strings = ["\n", "\r\n"])
    fun preserveScalarAndFlowBoundariesWhenAdaptingStandaloneListInsertions(newline: String) {
        val root = directory.resolve("repo")
        Files.createDirectories(root.resolve("envs"))
        Files.createDirectories(root.resolve("components/app"))
        Files.writeString(root.resolve("envs/dev.yaml"), "group:\n  components: [app]\n")
        val content = """
            label: emoji😀
            base: [one, two]
            note: |
              ${'$'}{unqualified}
              - literal
            first:
              ${'$'}{this@base}

              # comment
              - three
            last:
              - zero
              ${'$'}{this@base}
            flow: [
              ${'$'}{this@base}
            ]
        """.trimIndent().replace("\n", newline)
        Files.writeString(root.resolve("components/app/application.yaml"), content)
        val output = directory.resolve("output")

        MicroconfigRunner(root.toFile(), output.toFile()).build("dev", emptyList(), emptyList())

        val result = Yaml().load<Map<String, Any>>(Files.readString(output.resolve("app/application.yaml")))
        assertEquals("emoji😀", result["label"])
        assertEquals("\${unqualified}\n- literal\n", result["note"])
        assertEquals(listOf("one", "two", "three"), result["first"])
        assertEquals(listOf("zero", "one", "two"), result["last"])
        assertEquals(listOf(listOf("one", "two")), result["flow"])
    }

    companion object {
        @JvmStatic
        fun blockScalarSplices(): List<Arguments> =
            listOf("|", ">").flatMap { style ->
                listOf(false, true).flatMap { empty ->
                    listOf(false, true).flatMap { last ->
                        listOf(false, true).map { crlf -> Arguments.of(style, empty, last, crlf) }
                    }
                }
            }
    }
}
