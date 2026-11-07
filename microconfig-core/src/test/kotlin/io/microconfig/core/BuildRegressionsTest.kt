package io.microconfig.core

import io.microconfig.core.environments.repository.EnvironmentException
import io.microconfig.core.properties.ResolveException
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml

class BuildRegressionsTest {
    @TempDir lateinit var directory: Path
    private val root
        get() = directory.resolve("repo")

    private val destination
        get() = directory.resolve("output")

    @BeforeEach
    fun createRepository() {
        Files.createDirectories(root.resolve("components/app"))
        write("envs/dev.yaml", "group:\n  components: [app]\n")
    }

    @Test
    fun spliceListsBeforeBetweenAndAfterExplicitItems() {
        write(
            "components/app/application.yaml",
            """
            base:
              - one
              - two
            first:
              ${'$'}{this@base}
              - three
            middle:
              - zero
              ${'$'}{this@base}
              - three
            last:
              - zero
              ${'$'}{this@base}
            nested:
              - ${'$'}{this@base}
            """
                .trimIndent(),
        )
        build()
        val result = yaml()
        assertEquals(listOf("one", "two", "three"), result["first"])
        assertEquals(listOf("zero", "one", "two", "three"), result["middle"])
        assertEquals(listOf("zero", "one", "two"), result["last"])
        assertEquals(listOf(listOf("one", "two")), result["nested"])
    }

    @Test
    fun evaluateExpressionsContainingQuotedAndNestedBraces() {
        write(
            "components/app/application.yaml",
            """
            regex: #{#findGroup('[0-9]{2}', 'a12')}
            opening: #{'a{b'}
            closing: #{'a}b'}
            escaped: #{'it''s {fine}'}
            nested: #{#{2 + 3} * 2}
            collection: #{ {1, 2}.size() }
            """
                .trimIndent(),
        )
        build()
        assertEquals(
            mapOf(
                "regex" to 12,
                "opening" to "a{b",
                "closing" to "a}b",
                "escaped" to "it's {fine}",
                "nested" to 10,
                "collection" to 2,
            ),
            yaml(),
        )
    }

    @Test
    fun resolveQuotedNestedPlaceholderInsideExpressionDefault() {
        write(
            "components/app/application.yaml",
            "result: \${this@missing:#{'\${this@alsoMissing:'a}b'}'}}\nnext: value\n",
        )
        build()
        assertEquals(mapOf("result" to "a}b", "next" to "value"), yaml())
    }

    @ParameterizedTest
    @ValueSource(strings = ["yaml", "properties"])
    fun removeObsoleteOutputAfterChangingFormat(firstFormat: String) {
        val secondFormat = if (firstFormat == "yaml") "properties" else "yaml"
        val oldSource = "components/app/application.$firstFormat"
        val separator = if (firstFormat == "yaml") ": " else "="
        write(oldSource, "value${separator}old\n")
        build()
        write(oldSource, "value${separator}changed\n")
        build()
        assertTrue(output("diff-application.$firstFormat").toFile().exists())
        Files.delete(root.resolve(oldSource))
        write(
            "components/app/application.$secondFormat",
            "value${if (secondFormat == "yaml") ": " else "="}new\n",
        )
        build()
        assertTrue(output("application.$secondFormat").toFile().exists())
        assertFalse(output("application.$firstFormat").toFile().exists())
        assertFalse(output("diff-application.$firstFormat").toFile().exists())
    }

    @Test
    fun keepPreviousFormatWhenNewSerializationFails() {
        write("components/app/application.properties", "value=old\n")
        build()
        Files.delete(root.resolve("components/app/application.properties"))
        write("components/app/application.yaml", "value: \${missing@value:[invalid}\n")
        assertThrows(RuntimeException::class.java) { build() }
        assertEquals("value=old", Files.readString(output("application.properties")))
        assertFalse(output("application.yaml").toFile().exists())
    }

    @Test
    fun reportObsoleteOutputCleanupFailuresAndAllowRetry() {
        write("components/app/application.properties", "value=current\n")
        val obstruction = output("application.yaml/retained.txt")
        Files.createDirectories(obstruction.parent)
        Files.writeString(obstruction, "retained")
        assertThrows(IllegalStateException::class.java) { build() }
        assertEquals("retained", Files.readString(obstruction))
        Files.delete(obstruction)
        Files.delete(obstruction.parent)
        build()
        assertEquals("value=current", Files.readString(output("application.properties")))
    }

    @Test
    fun treatInlineDirectivesAsYamlComments() {
        write(
            "components/app/application.yaml",
            """
            key: 1 #include missing
            label: x #var injected: leaked
            result: ${'$'}{this@injected:fallback}
            """
                .trimIndent(),
        )
        build()
        assertEquals(mapOf("key" to 1, "label" to "x", "result" to "fallback"), yaml())
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "db\\:url=value",
                "a\\=b=value",
                "path\\\\:value=tail",
                "trailing\\ =value",
                "\u00a0key=value",
                "key\u00a0=value",
            ]
    )
    fun preserveEscapedPropertiesKeys(source: String) {
        write("components/app/application.properties", source)
        build()
        assertEquals(
            properties(source),
            properties(Files.readString(output("application.properties"))),
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "db\\:url=value",
                "a\\=b=value",
                "path\\\\:value=tail",
                "trailing\\ =value",
                "1=value",
                "\u00a0key=value",
                "key\u00a0=value",
            ]
    )
    fun preservePropertiesKeysWhenWritingYaml(source: String) {
        write("components/app/application.properties", source)
        write("components/app/marker.yaml", "marker: true\n")
        build()
        val expected =
            properties(source).entries.associate { it.key to it.value } + ("marker" to true)
        assertEquals(expected, yaml())
        build()
        assertFalse(output("diff-application.yaml").toFile().exists())
    }

    @Test
    fun preserveEscapedPropertiesKeysInGroups() {
        write("components/source/application.properties", "db.url\\:key=value\n")
        write("components/app/application.yaml", "group: \${source@db.*}\n")
        build()
        assertEquals(mapOf("group" to mapOf("url:key" to "value")), yaml())
        build()
        assertFalse(output("diff-application.yaml").toFile().exists())
    }

    @ParameterizedTest
    @ValueSource(strings = ["process", "env"])
    fun detectPropertiesWithEscapedColons(extension: String) {
        listOf("db\\:url=value\nit's=valid\n", "it's=valid\ndb\\:url=value\n").forEach { content ->
            write("components/app/config.$extension", content)
            build()
            assertEquals(
                mapOf("db:url" to "value", "it's" to "valid"),
                properties(Files.readString(output("$extension.properties"))),
            )
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["'a''=b': quoted", "\"a\\\"=b\": quoted", "\"a=b\": quoted"])
    fun detectYamlWithQuotedKeysInCustomFormats(source: String) {
        write("components/app/config.process", source)
        build()
        assertEquals(Yaml().load<Map<*, *>>(source), yaml("process.yaml"))
    }

    @Test
    fun excludeComponentsByAlias() {
        write("components/app/application.yaml", "name: \${this@name}\n")
        write("envs/base.yaml", "abstract: true\ngroup:\n  components: [first:app, second:app]\n")
        write("envs/dev.yaml", "include:\n  env: base\ngroup:\n  exclude: [first]\n")
        build()
        assertFalse(destination.resolve("first").toFile().exists())
        assertTrue(destination.resolve("second/application.yaml").toFile().exists())
    }

    @Test
    fun preserveTemplateIndentationAfterReplacingTemplateName() {
        write(
            "components/app/application.yaml",
            "text: |-\n  one\n  two\nmc.template.t: \${this@configDir}/template.txt -> result.txt\n",
        )
        write("components/app/template.txt", "\${this@templateName}\n  \${this@text}\n")
        build()
        assertEquals("t\n  one\n  two\n", Files.readString(output("result.txt")))
    }

    @Test
    fun rejectRemovedUuidFunction() {
        write("components/app/application.yaml", "id: #{#uuid4()}\n")
        val error = assertThrows(ResolveException::class.java) { build() }
        assertTrue(error.message.contains("uuid4"))
        assertTrue(error.message.contains("Available functions"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["yml", "k8s", "helm"])
    fun detectYamlDocumentsWithSupportedExtensions(extension: String) {
        write("components/app/config.$extension", "---\nvalue: 1\n")
        build()
        val file =
            when (extension) {
                "yml" -> "application.yaml"
                "helm" -> "values.yaml"
                else -> "k8s.yaml"
            }
        assertEquals(mapOf("value" to 1), yaml(file))
    }

    @Test
    fun preserveQuotedKeysThatLookLikeTemporaryDeclarations() {
        write(
            "components/app/application.yaml",
            "\".var label\": kept\n\"#var label\": also kept\nvalue: 1\n",
        )
        build()
        assertEquals(
            mapOf(".var label" to "kept", "#var label" to "also kept", "value" to 1),
            yaml(),
        )
    }

    @Test
    fun preserveDifferentYamlKeyTypesWithTheSameText() {
        write(
            "components/app/application.yaml",
            "\"1\": quoted\n1: numeric\n\"true\": text\ntrue: boolean\n",
        )
        build()
        assertEquals(
            mapOf("1" to "quoted", 1 to "numeric", "true" to "text", true to "boolean"),
            yaml(),
        )
    }

    @Test
    fun distinguishTemplateTypesWhenNamesMatch() {
        write(
            "components/app/application.yaml",
            """
            mc.file.same.fromFile: ${'$'}{this@configDir}/one.txt
            mc.file.same.toFile: one.txt
            mc.template.same.fromFile: ${'$'}{this@configDir}/two.txt
            mc.template.same.toFile: two.txt
            """
                .trimIndent(),
        )
        write("components/app/one.txt", "one")
        write("components/app/two.txt", "two")
        build()
        assertEquals("one", Files.readString(output("one.txt")))
        assertEquals("two", Files.readString(output("two.txt")))
    }

    @Test
    fun reportChangesToTrailingPropertiesWhitespace() {
        write("components/app/application.properties", "value=abc \n")
        build()
        write("components/app/application.properties", "value=abc\n")
        build()
        assertTrue(output("diff-application.properties").toFile().exists())
    }

    @Test
    fun reportEnvironmentProfileCycles() {
        write("components/app/application.yaml", "value: 1\n")
        write("envs/dev.yaml", "profiles: [base]\ngroup:\n  components: [app]\n")
        write("envs/base.yaml", "profiles: [dev]\n")
        val error = assertThrows(EnvironmentException::class.java) { build() }
        assertTrue(error.message.contains("dev -> base -> dev"))
    }

    @Test
    fun reuseSharedProfilesThroughDifferentBranches() {
        write("components/app/application.yaml", "value: base\n")
        write("components/app/application.shared.yaml", "shared: present\n")
        write("envs/dev.yaml", "profiles: [first, second]\ngroup:\n  components: [app]\n")
        write("envs/first.yaml", "profiles: [shared]\n")
        write("envs/second.yaml", "profiles: [shared]\n")
        write("envs/shared.yaml", "{}\n")
        build()
        assertEquals(mapOf("value" to "base", "shared" to "present"), yaml())
    }

    @Test
    fun reportEnvironmentIncludeCycles() {
        write("components/app/application.yaml", "value: 1\n")
        write("envs/dev.yaml", "include:\n  env: base\n")
        write("envs/base.yaml", "include:\n  env: dev\n")
        val error = assertThrows(EnvironmentException::class.java) { build() }
        assertTrue(error.message.contains("dev -> base -> dev"))
    }

    @Test
    fun deleteDiffSymlinkWithoutDeletingItsTarget() {
        org.junit.jupiter.api.Assumptions.assumeFalse(io.microconfig.utils.Os.isWindows())
        write("components/app/application.yaml", "value: unchanged\n")
        build()
        val target = directory.resolve("unrelated")
        Files.createDirectories(target)
        Files.writeString(target.resolve("retained.txt"), "retained")
        Files.createSymbolicLink(output("diff-application.yaml"), target)
        build()
        assertEquals("retained", Files.readString(target.resolve("retained.txt")))
        assertFalse(Files.exists(output("diff-application.yaml")))
    }

    @ParameterizedTest
    @ValueSource(strings = ["application.yaml", "application.properties", "process.yaml"])
    fun preserveTemplateOutputsNamedLikeGeneratedConfigs(name: String) {
        write(
            "components/app/application.yaml",
            "mc.template.config: \${this@configDir}/source.txt -> $name\n",
        )
        write("components/app/source.txt", "payload\n")
        build()
        assertEquals("payload\n", Files.readString(output(name)))
        build()
        assertEquals("payload\n", Files.readString(output(name)))
    }

    @Test
    fun preserveOppositeFormatTemplateAlongsideConfig() {
        write(
            "components/app/application.yaml",
            "value: config\nmc.file.config: \${this@configDir}/source.txt -> application.properties\n",
        )
        write("components/app/source.txt", "copied=value\n")
        build()
        assertEquals("copied=value\n", Files.readString(output("application.properties")))
        assertEquals(mapOf("value" to "config"), yaml())
    }

    @Test
    fun rejectConfigAndTemplateDestinationCollisionBeforeWriting() {
        write("components/app/application.yaml", "value: old\n")
        build()
        write("components/app/source.txt", "replacement")
        write(
            "components/app/application.yaml",
            "value: new\nmc.file.config: \${this@configDir}/source.txt -> application.yaml\n",
        )
        assertThrows(IllegalArgumentException::class.java) { build() }
        assertEquals(mapOf("value" to "old"), yaml())
    }

    @ParameterizedTest
    @ValueSource(strings = ["'", "\""])
    fun resolveAndOverrideQuotedYamlKeys(quote: String) {
        write(
            "components/app/application.yaml",
            "${quote}host$quote: localhost\nvalue: baseline\nurl: \${this@host}\n+dev:\n  ${quote}value$quote: overridden\n",
        )
        build()
        assertEquals(
            mapOf("host" to "localhost", "url" to "localhost", "value" to "overridden"),
            yaml(),
        )
    }

    @Test
    fun overrideDottedKeysAcrossDifferentQuoteStyles() {
        write(
            "components/app/application.yaml",
            "'literal.key': baseline\n+dev:\n  \"literal.key\": overridden\n",
        )
        build()
        assertEquals(mapOf("literal.key" to "overridden"), yaml())
    }

    @Test
    fun rejectRecursiveYamlMergeWithoutHanging() {
        write("components/app/application.yaml", "value: &value\n  <<: *value\n")
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(2)) {
            val error = assertThrows(IllegalArgumentException::class.java) { build() }
            assertTrue(error.message.orEmpty().contains("Recursive YAML merge"))
        }
    }

    @Test
    fun preserveYamlMergeSequencePrecedence() {
        write(
            "components/app/application.yaml",
            ".var:\n  first: &first {value: first, left: present}\n  second: &second {value: second, right: present}\nresult:\n  <<: [*first, *second]\n  local: present\n",
        )
        build()
        assertEquals(
            mapOf(
                "result" to
                    mapOf(
                        "value" to "first",
                        "left" to "present",
                        "right" to "present",
                        "local" to "present",
                    )
            ),
            yaml(),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["{\"a\": 1}", "#{#findGroup('[0-9]{2}', 'a12')}", "'text } text'"])
    fun consumeWholeDefaultWhenReferencedPropertyExists(default: String) {
        write(
            "components/app/application.yaml",
            "present: actual\nresult: \${this@present:$default}\n",
        )
        build()
        assertEquals("actual", yaml()["result"])
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " # list"])
    fun mergeListsContainingOnlyStandaloneSplices(comment: String) {
        write(
            "components/app/application.yaml",
            ".var:\n  first: [one, two]\n  second: [three, four]\nresult:\n  \${this@first}$comment\n  \${this@second}$comment\n",
        )
        build()
        assertEquals(listOf("one", "two", "three", "four"), yaml()["result"])
    }

    @ParameterizedTest
    @ValueSource(strings = ["plain", "literal"])
    fun preserveIndentationInsideMultilineStatements(style: String) {
        val content =
            if (style == "plain") "value: #{1 +\n  2}\n" else "value: |-\n  #{1 +\n    2}\n"
        write("components/app/application.yaml", content)
        build()
        assertEquals(if (style == "plain") 3 else "3", yaml()["value"])
    }

    @Test
    fun applyTransitiveProfilesToInlineOverridesAndReferences() {
        write("envs/dev.yaml", "profiles: [base]\ngroup:\n  components: [app]\n")
        write("envs/base.yaml", "abstract: true\nprofiles: [common]\n")
        write("envs/common.yaml", "abstract: true\n")
        write(
            "components/app/application.yaml",
            "value: baseline\nresult: \${app[common]@value}\n+common:\n  value: overridden\n",
        )
        write("components/app/application.common.yaml", "fileValue: present\n")
        build()
        assertEquals(
            mapOf("value" to "overridden", "result" to "overridden", "fileValue" to "present"),
            yaml(),
        )
    }

    @Test
    fun resolveAliasConfigDirectoryBeforeMatchingAnUnrelatedFolder() {
        write("envs/dev.yaml", "group:\n  components: [app:base]\n")
        write(
            "components/base/application.yaml",
            "mc.template.config: \${this@configDir}/source.txt -> rendered.txt\n",
        )
        write("components/base/source.txt", "correct template")
        write("components/app/source.txt", "unrelated template")
        build()
        assertEquals("correct template", Files.readString(output("rendered.txt")))
    }

    @Test
    fun preserveSetTagsOnBlockMappings() {
        write(
            "components/app/application.yaml",
            "permissions: !!set\n  read: null\n  write: null\n",
        )
        build()
        assertEquals(mapOf("permissions" to setOf("read", "write")), yaml())
    }

    @Test
    fun preserveCustomTagsOnBlockMappings() {
        write("components/app/application.yaml", "secret: !Secret\n  field: value\n")
        build()
        val document =
            Yaml().compose(Files.newBufferedReader(output("application.yaml")))
                as org.yaml.snakeyaml.nodes.MappingNode
        assertEquals(org.yaml.snakeyaml.nodes.Tag("!Secret"), document.value.single().valueNode.tag)
    }

    @Test
    fun rejectUnsupportedRootTagsExplicitly() {
        write("components/app/application.yaml", "!!set\nread: null\n")
        assertThrows(IllegalArgumentException::class.java) { build() }
    }

    @Test
    fun retainLiteralQuotesInPropertiesKeysWhenWritingYaml() {
        write("components/app/application.properties", "\"foo\"=first\nfoo=second\n")
        write("components/app/marker.yaml", "marker: true\n")
        build()
        assertEquals(mapOf("\"foo\"" to "first", "foo" to "second", "marker" to true), yaml())
    }

    @Test
    fun decodePropertiesKeysForMustacheModels() {
        write(
            "components/app/application.properties",
            "db\\:host=localhost\nmc.mustache.config=\${this@configDir}/source.txt -> rendered.txt\n",
        )
        write("components/app/source.txt", "{{db:host}}")
        build()
        assertEquals("localhost", Files.readString(output("rendered.txt")))
    }

    @Test
    fun reuseResolvedDynamicPropertiesAcrossReferences() {
        write(
            "components/app/application.yaml",
            "stamp: #{#currentTimeMillis()}\n" +
                (0..7).joinToString("") { "ref$it: \${this@stamp}\n" },
        )
        build()
        val result = yaml()
        (0..7).forEach { assertEquals(result["stamp"], result["ref$it"]) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["this", "c"])
    fun resolveSelfPlaceholdersUsingEachReferenceChain(component: String) {
        write("components/app/application.yaml", "p1: \${a@v}\np2: \${b@v}\n")
        write("components/a/application.yaml", "v: \${c@w}\nx: A\n")
        write("components/b/application.yaml", "v: \${c@w}\nx: B\n")
        write("components/c/application.yaml", "w: \${$component@x}\n")
        build()
        assertEquals(mapOf("p1" to "A", "p2" to "B"), yaml())
    }

    @Test
    fun resolveCyclicDefaultsUsingEachReferenceChain() {
        write("components/app/application.yaml", "p1: \${a@x}\np2: \${a@y}\n")
        write("components/a/application.yaml", "x: \${this@y:X}\ny: \${this@x:Y}\n")
        build()
        assertEquals(mapOf("p1" to "Y", "p2" to "X"), yaml())
    }

    @Test
    fun applyComponentChangesWithoutIncludingAnEnvironment() {
        listOf("a", "b", "c").forEach { write("components/$it/application.yaml", "name: $it\n") }
        write("envs/dev.yaml", "group:\n  components: [a, c]\n  append: [b]\n  exclude: [c]\n")
        build()
        assertTrue(Files.exists(destination.resolve("a/application.yaml")))
        assertTrue(Files.exists(destination.resolve("b/application.yaml")))
        assertFalse(Files.exists(destination.resolve("c/application.yaml")))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun applyComponentChangesToNewGroupsInIncludedEnvironments(withComponents: Boolean) {
        listOf("a", "b", "c").forEach { write("components/$it/application.yaml", "name: $it\n") }
        write("envs/base.yaml", "abstract: true\ng1:\n  components: [a]\n")
        write(
            "envs/dev.yaml",
            "include:\n  env: base\ng2:\n  append: [b]\n" +
                if (withComponents) "  components: [c]\n  exclude: [c]\n" else "",
        )
        build()
        assertTrue(Files.exists(destination.resolve("a/application.yaml")))
        assertTrue(Files.exists(destination.resolve("b/application.yaml")))
        assertFalse(Files.exists(destination.resolve("c/application.yaml")))
    }

    @Test
    fun applyComponentChangesOnceAtEachEnvironmentIncludeLevel() {
        listOf("a", "b", "c", "d").forEach {
            write("components/$it/application.yaml", "name: $it\n")
        }
        write("envs/base.yaml", "abstract: true\ngroup:\n  components: [a]\n  append: [b]\n")
        write(
            "envs/middle.yaml",
            "abstract: true\ninclude:\n  env: base\ngroup:\n  append: [c]\n  exclude: [a]\n",
        )
        write("envs/dev.yaml", "include:\n  env: middle\ngroup:\n  append: [d]\n  exclude: [c]\n")
        build()
        assertTrue(Files.exists(destination.resolve("b/application.yaml")))
        assertTrue(Files.exists(destination.resolve("d/application.yaml")))
        assertFalse(Files.exists(destination.resolve("a/application.yaml")))
        assertFalse(Files.exists(destination.resolve("c/application.yaml")))
    }

    @Test
    fun applyReplacementExclusionAndAppendOnceAcrossAliasedEnvironmentGroups() {
        write("components/app/application.yaml", "name: \${this@name}\n")
        write("envs/base.yaml", """
            abstract: true
            group:
              components: [old:app, kept:app]
              append: [base:app]
            """.trimIndent())
        write("envs/middle.yaml", """
            abstract: true
            include:
              env: base
            group:
              components: [replacement:app, drop:app]
              exclude: [drop]
              append: [mid:app]
            """.trimIndent())
        write("envs/dev.yaml", """
            include:
              env: middle
            group:
              exclude: [replacement]
              append: [final:app, drop:app]
            """.trimIndent())

        val components = Microconfig.searchConfigsIn(root.toFile())
            .inEnvironment("dev").allComponents.asList()
        assertEquals(listOf("mid", "final", "drop"), components.map { it.name })
        assertTrue(components.all { it.originalName == "app" && it.environment == "dev" })
        build()

        listOf("mid", "final", "drop").forEach { alias ->
            val output = Files.readString(destination.resolve("$alias/application.yaml"))
            assertEquals(mapOf("name" to alias), Yaml().load<Map<String, String>>(output))
        }
        listOf("old", "kept", "base", "replacement").forEach {
            assertFalse(Files.exists(destination.resolve(it)))
        }
    }

    @Test
    fun keepOnlyTheLastDuplicateYamlGroupBeforeApplyingEnvironmentChanges() {
        write("components/app/application.yaml", "name: \${this@name}\n")
        write("envs/base.yaml", "abstract: true\ngroup:\n  components: [base:app]\n")
        write("envs/dev.yaml", """
            include:
              env: base
            group:
              exclude: [base]
              append: [discarded:app]
            "group":
              append: [kept:app]
            """.trimIndent())

        val environment = Microconfig.searchConfigsIn(root.toFile()).inEnvironment("dev")
        assertEquals(listOf("group"), environment.groups.map { it.name })
        assertEquals(listOf("base", "kept"), environment.allComponents.asList().map { it.name })
        build()

        assertTrue(Files.exists(destination.resolve("base/application.yaml")))
        assertTrue(Files.exists(destination.resolve("kept/application.yaml")))
        assertFalse(Files.exists(destination.resolve("discarded")))
    }

    @Test
    fun applyStandaloneComponentChangesByAlias() {
        write(
            "envs/dev.yaml",
            "group:\n  components: [old:app, alias:app]\n  exclude: [old]\n  append: [extra:app]\n",
        )
        write("components/app/application.yaml", "name: \${this@name}\n")
        build()
        assertTrue(Files.exists(destination.resolve("alias/application.yaml")))
        assertTrue(Files.exists(destination.resolve("extra/application.yaml")))
        assertFalse(Files.exists(destination.resolve("old/application.yaml")))
    }

    @Test
    fun rejectDuplicatesAfterAppendingStandaloneComponents() {
        write("envs/dev.yaml", "group:\n  components: [app]\n  append: [app]\n")
        write("components/app/application.yaml", "name: app\n")
        val error = assertThrows(IllegalStateException::class.java) { build() }
        assertTrue(error.message.orEmpty().contains("several declarations"))
    }

    @Test
    fun recordAddedPropertiesAfterAnExistingEmptyConfig() {
        write("components/app/application.properties", "key=added\n")
        Files.createDirectories(output("application.properties").parent)
        Files.writeString(output("application.properties"), "")
        build()
        assertEquals("+key=added", Files.readString(output("diff-application.properties")))
    }

    @Test
    fun recordRemovalOfTheLastPropertiesEntry() {
        write("components/app/application.properties", "key=old\n")
        build()
        write("components/app/application.properties", "")
        build()
        assertFalse(Files.exists(output("application.properties")))
        assertEquals("-key=old", Files.readString(output("diff-application.properties")))
    }

    @ParameterizedTest
    @ValueSource(strings = ["\u2003", "\u2002", "\u000B"])
    fun preserveUnicodeWhitespaceInPropertiesValues(space: String) {
        val source = "key=${space}value\n"
        write("components/app/application.properties", source)
        build()
        assertEquals(
            properties(source),
            properties(Files.readString(output("application.properties"))),
        )
    }

    @Test
    fun keepVariablesUnderQuotedEnvironmentLikeKeysGlobal() {
        write(
            "components/app/application.yaml",
            "\"+other\":\n  #var value: ordinary\n  key: visible\nresult: \${this@value:fallback}\n",
        )
        build()
        assertEquals(mapOf("+other" to mapOf("key" to "visible"), "result" to "ordinary"), yaml())
    }

    @Test
    fun preserveDefaultBoundariesAndEscapesInTemplates() {
        write(
            "components/app/application.yaml",
            "present: actual\nmc.template.config: \${this@configDir}/source.txt -> rendered.txt\n",
        )
        write(
            "components/app/source.txt",
            "\${this@present:{\"a\": 1}}\n\\\${this@present:{\"a\": 1}}\n\\\${this@templateName}\n",
        )
        build()
        assertEquals(
            "actual\n\${this@present:{\"a\": 1}}\n\${this@templateName}\n",
            Files.readString(output("rendered.txt")),
        )
    }

    @Test
    fun indentTemplateValuesAfterEarlierReplacementsOnTheSameLine() {
        write(
            "components/app/application.yaml",
            ".var:\n  text: |-\n    one\n    two\nmc.template.t: \${this@configDir}/source.txt -> rendered.txt\n",
        )
        write("components/app/source.txt", "\${this@templateName} \${this@text}\n")
        build()
        assertEquals("t one\n  two\n", Files.readString(output("rendered.txt")))
    }

    @Test
    fun preserveExplicitProfileInPlaceholderReferences() {
        write("envs/dev.yaml", "profiles: [prod]\ngroup:\n  components: [app]\n")
        write("envs/prod.yaml", "{}\n")
        write("components/source/application.prod.yaml", "value: prod\n")
        write("components/source/application.dev.yaml", "value: dev\n")
        write(
            "components/app/application.yaml",
            "explicit: \${source[prod]@value}\nimplicit: \${source@value}\n" +
                "environment: \${source[prod]@env}\n",
        )

        build()

        assertEquals(mapOf("explicit" to "prod", "implicit" to "dev", "environment" to "prod"), yaml())
    }

    @ParameterizedTest
    @ValueSource(strings = ["'1'", "'true'", "1.5", "'a.b'", "'a''b'"])
    fun serializeAtomicAndDescendantConflictsWithQuotedOrTypedKeys(key: String) {
        write("components/app/application.yaml", "$key: {}\n")
        write("components/app/application.dev.yaml", "$key:\n  child: present\n")

        build()

        val originalKey = Yaml().load<Any>(key)
        assertEquals(mapOf(originalKey to emptyMap<Any, Any>(), "$originalKey.child" to "present"), yaml())
    }

    private fun write(path: String, content: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

    private fun build() =
        MicroconfigRunner(root.toFile(), destination.toFile())
            .build("dev", emptyList(), emptyList())

    private fun output(name: String): Path = destination.resolve("app/$name")

    private fun yaml(file: String = "application.yaml"): Map<*, *> =
        Yaml().load(Files.readString(output(file)))

    private fun properties(text: String): Properties =
        Properties().apply { load(StringReader(text)) }
}
