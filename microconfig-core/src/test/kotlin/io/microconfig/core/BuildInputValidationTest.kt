package io.microconfig.core

import io.microconfig.core.properties.*
import io.microconfig.core.properties.io.selector.ConfigIoFactory
import io.microconfig.io.impl.DumpedFsReader
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import java.util.Properties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.ScalarNode

class BuildInputValidationTest {
    @TempDir lateinit var directory: Path
    private val root
        get() = directory.resolve("repo")

    private val destination
        get() = directory.resolve("output")

    @BeforeEach
    fun createRepository() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
        write("components/app/application.yaml", "value: present\n")
    }

    @Test
    fun cacheIndependentPropertiesSeparately() {
        val source = DeclaringComponentImpl("app", "app", "dev")
        val first =
            PropertyImpl.property(
                "first",
                "#{#currentTimeMillis()}",
                ConfigFormat.PROPERTIES,
                source,
            )
        val second =
            PropertyImpl.property(
                "second",
                "#{#currentTimeMillis()}",
                ConfigFormat.PROPERTIES,
                source,
            )
        val resolver = Microconfig.searchConfigsIn(root.toFile()).resolver()
        val original = first.resolveBy(resolver, source).value
        Thread.sleep(20)
        assertEquals(original, first.resolveBy(resolver, source).value)
        assertTrue(second.resolveBy(resolver, source).value.toLong() > original.toLong())
    }

    @Test
    fun applyCustomPropertyResolutionOnce() {
        val source = DeclaringComponentImpl("app", "app", "dev")
        val original = PropertyImpl.property("key", "value", ConfigFormat.PROPERTIES, source)
        val property =
            object : Property by original {
                override fun resolveBy(resolver: Resolver, root: DeclaringComponent): Property =
                    PropertyImpl.property(
                        "key",
                        "prefix-" + resolver.resolve(this, root),
                        configFormat,
                        source,
                    )
            }
        val resolver = Microconfig.searchConfigsIn(root.toFile()).resolver()
        assertEquals("prefix-value", property.resolveBy(resolver, source).value)
    }

    @Test
    fun shareRawValuesBetweenDirectAndPlaceholderResolution() {
        val source = DeclaringComponentImpl("app", "custom", "dev")
        val context = DeclaringComponentImpl("app", "root", "dev")
        val original = PropertyImpl.property("key", "value", ConfigFormat.PROPERTIES, source)
        val property =
            object : Property by original {
                override fun resolveBy(resolver: Resolver, root: DeclaringComponent): Property =
                    PropertyImpl.property(
                        "key",
                        "prefix-" + resolver.resolve(this, root),
                        configFormat,
                        source,
                    )
            }
        val strategy = PlaceholderResolveStrategy { component, key, _, _, _ ->
            Optional.ofNullable(property.takeIf { component == "custom" && key == "key" })
        }
        val resolver =
            Microconfig.searchConfigsIn(root.toFile())
                .withAdditionalPlaceholderResolvers(listOf(strategy))
                .resolver()
        assertEquals("prefix-value", resolver.resolve("\${custom@key}", context, context))
        assertEquals("prefix-value", property.resolveBy(resolver, context).value)
    }

    @ParameterizedTest
    @ValueSource(strings = ["fromFile", "toFile"])
    fun allowTemplateNamesMatchingFromToNotationFields(name: String) {
        write(
            "components/app/application.yaml",
            "mc.file.$name.fromFile: \${this@configDir}/source.txt\nmc.file.$name.toFile: rendered.txt\n",
        )
        write("components/app/source.txt", "payload")
        build()
        assertEquals("payload", Files.readString(output("rendered.txt")))
    }

    @Test
    fun preserveEmbeddedQuotesInDottedKeys() {
        write("components/app/application.yaml", "it's.value: retained\n")
        build()
        assertEquals(mapOf("it's" to mapOf("value" to "retained")), yaml())
    }

    @Test
    fun preserveEmbeddedQuotesAfterYamlTagPrefixes() {
        write("components/app/application.yaml", "a. !!str it's.value: retained\n")
        build()
        assertEquals(mapOf("a" to mapOf("it's" to mapOf("value" to "retained"))), yaml())
    }

    @ParameterizedTest
    @ValueSource(strings = ["a. \"b.c\": retained\n", "a. !!str \"b.c\": retained\n"])
    fun allowSpacesBeforeQuotedPathParts(source: String) {
        write("components/app/application.yaml", source)
        build()
        assertEquals(mapOf("a" to mapOf("b.c" to "retained")), yaml())
    }

    @ParameterizedTest
    @ValueSource(strings = ["!!int", "!!float", "!!bool"])
    fun preserveExplicitKeyTagsWithoutConstructorValidation(tag: String) {
        write("components/app/application.yaml", "$tag 'literal': retained\n")
        build()
        val content = Files.readString(output("application.yaml"))
        assertTrue(content.contains(tag), content)
        assertTrue(content.contains("literal"), content)
        build()
        assertFalse(Files.exists(output("diff-application.yaml")))
    }

    @Test
    fun preserveTrailingLineInCustomTaggedKey() {
        write("components/app/application.yaml", "? !Opaque |\n  literal\n: retained\n")
        build()
        val document =
            Yaml().compose(StringReader(Files.readString(output("application.yaml"))))
                as MappingNode
        assertEquals("literal\n", (document.value.single().keyNode as ScalarNode).value)
    }

    @Test
    fun preserveTaggedPropertyKeyWithoutSerializationAnchor() {
        write("components/app/application.yaml", "? &key !Opaque value\n: retained\n")
        build()
        assertEquals("!Opaque 'value': retained\n", Files.readString(output("application.yaml")))
    }

    @Test
    fun preserveAtomicFlowValuesAlongsideDescendantKeys() {
        write(
            "components/app/application.yaml",
            "settings: {port: 80}\n+dev:\n  settings.port: 81\ncopy: \${this@settings.port}\n",
        )
        build()
        assertEquals(
            mapOf("copy" to 81, "settings" to mapOf("port" to 80), "settings.port" to 81),
            yaml(),
        )
    }

    @Test
    fun keepAtomicMappingOverridesSeparateFromExistingDescendantKeys() {
        write(
            "components/app/application.yaml",
            "settings:\n  port: 80\n+dev:\n  settings: {port: 81}\ncopy: \${this@settings.port}\n",
        )
        build()
        assertEquals(
            mapOf("copy" to 80, "settings" to mapOf("port" to 81), "settings.port" to 80),
            yaml(),
        )
    }

    @Test
    fun retainFloatingPointKeysAndResolveTheirEquivalentSpelling() {
        write(
            "components/app/application.yaml",
            "1.0: baseline\ncopy: \${this@1.00}\n+dev:\n  1.00: overridden\n",
        )
        build()
        assertEquals(mapOf(1.0 to "overridden", "copy" to "overridden"), yaml())
        build()
        assertFalse(Files.exists(output("diff-application.yaml")))
    }

    @Test
    fun preserveFloatingPointKeysInsideNestedMappings() {
        write("components/app/application.yaml", "values:\n  1.5: retained\n")
        build()
        assertEquals(mapOf("values" to mapOf(1.5 to "retained")), yaml())
    }

    @Test
    fun resolveNestedNumericAndBooleanKeysByTheirOriginalSpelling() {
        write(
            "components/app/application.yaml",
            "values:\n  yes: flag\n  0x1: integer\n  1.50: float\nflag: \${this@values.yes}\ninteger: \${this@values.0x1}\nfloat: \${this@values.1.50}\n",
        )
        build()
        assertEquals("flag", yaml()["flag"])
        assertEquals("integer", yaml()["integer"])
        assertEquals("float", yaml()["float"])
    }

    @Test
    fun distinguishFloatingPointKeysFromNestedIntegerPaths() {
        write(
            "components/app/application.yaml",
            "values:\n  1.5:\n    3:\n      4: float\n  1:\n    5: nested\nfloat: \${this@values.1.50.03.04}\nnested: \${this@values.01.05}\n",
        )
        build()
        assertEquals("float", yaml()["float"])
        assertEquals("nested", yaml()["nested"])
    }

    @Test
    fun resolveGroupedPropertiesUnderCanonicalizedKeys() {
        write(
            "components/app/application.yaml",
            "values:\n  1.50:\n    leaf: floating\n  yes:\n    leaf: boolean\nfloating: \${this@values.1.50.*}\nboolean: \${this@values.yes.*}\n",
        )
        build()
        assertEquals(mapOf("leaf" to "floating"), yaml()["floating"])
        assertEquals(mapOf("leaf" to "boolean"), yaml()["boolean"])
    }

    @Test
    fun resolveCanonicalBooleanKeysUsingTheirOriginalSpelling() {
        write(
            "components/app/application.yaml",
            "yes: original\ncopy: \${this@yes}\n+dev:\n  true: overridden\n",
        )
        build()
        assertEquals(mapOf(true to "overridden", "copy" to "overridden"), yaml())
    }

    @Test
    fun canonicalizeNumericOverrideKeysAndKeepStringKeysDistinct() {
        write(
            "components/app/application.yaml",
            "1: baseline\n\"1\": string\n+dev:\n  0x1: overridden\n",
        )
        build()
        assertEquals(mapOf(1 to "overridden", "1" to "string"), yaml())
    }

    @Test
    fun rejectSemanticallyDuplicateNumericKeys() {
        write("components/app/application.yaml", "1: first\n0x1: second\n")
        assertThrows(IllegalStateException::class.java) { build() }
    }

    @ParameterizedTest
    @ValueSource(strings = ["ordinary", "host=literal", "host:literal"])
    fun preserveVarLikePlainScalarContinuations(text: String) {
        write("components/app/application.yaml", "message: first\n  .var $text\n  last\n")
        build()
        assertEquals(mapOf("message" to "first .var $text last"), yaml())
    }

    @ParameterizedTest
    @ValueSource(strings = ["\n  last\n", "\nnext: present\n"])
    fun preservePlainContinuationsAfterPropertiesStyleVariables(tail: String) {
        write(
            "components/app/application.yaml",
            ".var host=actual\nmessage: first\n  .var host=literal$tail",
        )
        build()
        val expected = "first .var host=literal" + if (tail.contains("last")) " last" else ""
        assertEquals(expected, yaml()["message"])
    }

    @Test
    fun retainVarLikeTextInsideMultilineExpressions() {
        write("components/app/application.yaml", "value: #{'first\n  .var literal\n  last'}\n")
        build()
        assertEquals("first\n  .var literal\n  last", yaml()["value"])
    }

    @ParameterizedTest
    @ValueSource(strings = ["", ".var host=actual\n"])
    fun preservePlainValuesBeginningWithVar(prefix: String) {
        write("components/app/application.yaml", prefix + "message:\n  .var ordinary\n  last\n")
        build()
        assertEquals(mapOf("message" to ".var ordinary last"), yaml())
    }

    @Test
    fun preserveSequenceValuesBeginningWithVar() {
        write("components/app/application.yaml", "items:\n  -\n    .var ordinary\n    last\n")
        build()
        assertEquals(mapOf("items" to listOf(".var ordinary last")), yaml())
    }

    @Test
    fun keepPropertiesStyleVariablesScopedToTheirEnvironment() {
        write(
            "components/app/application.yaml",
            "+dev:\n  .var host=actual\nresult: \${this@host:fallback}\n",
        )
        build()
        assertEquals(mapOf("result" to "actual"), yaml())
    }

    @ParameterizedTest
    @ValueSource(strings = ["value: #{1 + 2\n", "value:\n  #{1 + 2\n"])
    fun allowIncompleteExpressionPrefixesInPlainYaml(source: String) {
        write("components/app/application.yaml", source)
        build()
        assertEquals(mapOf("value" to null), yaml())
    }

    @ParameterizedTest
    @ValueSource(strings = ["items:\n  - #{1 + 2\n", "items:\n  -\n    #{1 + 2\n"])
    fun allowIncompleteExpressionPrefixesInPlainYamlSequences(source: String) {
        write("components/app/application.yaml", source)
        build()
        assertEquals(mapOf("items" to listOf(null)), yaml())
    }

    @Test
    fun preserveUnclosedExpressionPrefixesInsideCommentsAndLiteralText() {
        write(
            "components/app/application.yaml",
            "# documentation: #{unfinished\nvalue: '#{literal'\nscript: |-\n  - #{literal\n",
        )
        build()
        assertEquals(mapOf("value" to "#{literal", "script" to "- #{literal"), yaml())
    }

    @ParameterizedTest
    @ValueSource(strings = ["properties", "process"])
    fun recognizePropertiesExclamationComments(extension: String) {
        Files.delete(root.resolve("components/app/application.yaml"))
        write("components/app/application.$extension", "! ordinary comment\nvalue=present\n")
        build()
        val name = if (extension == "properties") "application.properties" else "process.properties"
        assertEquals("value=present", Files.readString(output(name)))
    }

    @Test
    fun retainExclamationMarksInsideContinuedPropertiesValues() {
        Files.delete(root.resolve("components/app/application.yaml"))
        write("components/app/application.properties", "value=first\\\n !literal=retained\n")
        build()
        val properties =
            Properties().apply {
                load(StringReader(Files.readString(output("application.properties"))))
            }
        assertEquals("first!literal=retained", properties.getProperty("value"))
    }

    @Test
    fun reportDeclarationLineForContinuedPropertiesValues() {
        Files.delete(root.resolve("components/app/application.yaml"))
        write("components/app/application.properties", "value=\${missing@key}\\\n continued\n")
        val error = assertThrows(ResolveException::class.java) { build() }
        assertTrue(error.message.contains("application.properties:1"), error.message)
    }

    @Test
    fun retainEnvironmentDescriptorDefaults() {
        write(
            "envs/dev.yaml",
            "include: {env: ''}\nabstract: null\ngroup:\n  components: [app]\n",
        )
        build()
        assertEquals(mapOf("value" to "present"), yaml())
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "configTypes: null",
                "configTypes: []\nconfigTypes: null",
            ]
    )
    fun retainConfigTypeDescriptorDefaults(descriptor: String) {
        write("microconfig.yaml", descriptor)
        build()
        assertEquals(mapOf("value" to "present"), yaml())
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["include: ignored", "abstract: 'true'", "include: {env: base, exclude: group}"]
    )
    fun rejectEnvironmentFieldsThatWouldOtherwiseBeIgnored(fields: String) {
        write("envs/base.yaml", "{}\n")
        write("envs/dev.yaml", "$fields\ngroup:\n  components: [app]\n")
        assertThrows(RuntimeException::class.java) { build() }
        assertFalse(Files.exists(output("application.yaml")))
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "configTypes: app",
                "configTypes: [{app: scalar}]",
                "configTypes: [{app: {sourceExtensions: .yaml}}]",
                "configTypes: [{app: {resultFileName: [result]}}]",
                "configTypes: [{app: {}, process: {}}]",
            ]
    )
    fun rejectConfigTypeFieldsThatWouldOtherwiseBeIgnored(descriptor: String) {
        write("microconfig.yaml", descriptor)
        assertThrows(RuntimeException::class.java) { build() }
        assertFalse(Files.exists(output("application.yaml")))
    }

    @Test
    fun allowEmptyResultFileBasename() {
        write(
            "microconfig.yaml",
            "configTypes:\n  - app:\n      sourceExtensions: [.yaml]\n      resultFileName: ''\n",
        )
        build()
        assertEquals(
            mapOf("value" to "present"),
            Yaml().load<Map<*, *>>(Files.readString(output(".yaml"))),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["!!str", "!custom"])
    fun recognizeTaggedYamlKeysWithCustomExtensions(tag: String) {
        write("components/app/config.process", "$tag value: retained\n")
        build()
        assertTrue(Files.readString(output("process.yaml")).contains("retained"))
        assertFalse(Files.exists(output("process.properties")))
    }

    @Test
    fun keepPropertiesKeyLookupsLiteral() {
        Files.delete(root.resolve("components/app/application.yaml"))
        write(
            "components/app/application.properties",
            "1=integer\ntrue=boolean\nvalues.true.leaf=group\nnumber=\${this@0x1:fallback}\nflag=\${this@yes:fallback}\ngroup=\${this@values.yes.*:fallback}\n",
        )
        build()
        val values =
            Properties().apply {
                load(StringReader(Files.readString(output("application.properties"))))
            }
        assertEquals("fallback", values.getProperty("number"))
        assertEquals("fallback", values.getProperty("flag"))
        assertEquals("fallback", values.getProperty("group"))
    }

    @Test
    fun supportMoreThanTheSingleCharacterMarkerPool() {
        val count = 6400
        write(
            "components/app/application.yaml",
            (0 until count).joinToString("") { "key$it: \${missing@key$it:fallback}\n" },
        )
        val file = root.resolve("components/app/application.yaml").toFile()
        val properties =
            ConfigIoFactory.newConfigIo(DumpedFsReader()).readFrom(file).properties("app", "dev")
        assertEquals(count, properties.size)
        assertEquals("\${missing@key6399:fallback}", properties.last().value)
    }

    @Test
    fun retainPrivateUseCharactersBesideMaskedStatements() {
        val text = (0xE000..0xF8FF).map(Int::toChar).joinToString("")
        write("components/app/application.yaml", "value: \"$text\${missing@key:fallback}\"\n")
        build()
        assertEquals(text + "fallback", yaml()["value"])
    }

    private fun write(path: String, content: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

    private fun build() =
        MicroconfigRunner(root.toFile(), destination.toFile())
            .build("dev", emptyList(), emptyList())

    private fun output(name: String) = destination.resolve("app/$name")

    private fun yaml(): Map<*, *> = Yaml().load(Files.readString(output("application.yaml")))
}
