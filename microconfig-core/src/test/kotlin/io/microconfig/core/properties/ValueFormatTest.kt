package io.microconfig.core.properties

import io.microconfig.core.Microconfig
import io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo
import io.microconfig.core.properties.io.yaml.YamlResolvers
import io.microconfig.core.properties.resolvers.expression.impl.ExpressionResolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path

class ValueFormatTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun keepDifferentRepresentationsSeparateWhenResolvingTheSameText() {
        Files.createDirectory(directory.resolve("envs"))
        Files.createDirectory(directory.resolve("components"))
        val context = DeclaringComponentImpl("app", "component", "dev")
        val resolver = Microconfig.searchConfigsIn(directory.toFile()).resolver()
        val encoded = PropertyImpl(
            "value", "#{'\\n'.length()}", false,
            ConfigFormat.PROPERTIES, context, valueFormat = ValueFormat.PROPERTIES
        )
        val plain = encoded.copy(valueFormat = ValueFormat.TEXT)

        assertEquals("1", encoded.resolveBy(resolver, context).value)
        assertEquals("2", plain.resolveBy(resolver, context).value)
        assertEquals("1", encoded.resolveBy(resolver, context).value)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun preserveEncodingWhenSourceMetadataIsDecorated(yaml: Boolean) {
        val file = directory.resolve(if (yaml) "application.yaml" else "application.properties").toFile()
        Files.writeString(
            file.toPath(), if (yaml) """value: '#{"a" + "\tail"}'"""
            else """value=#{\u0022a\u0022 + \u0022\\tail\u0022}"""
        )
        val io = configIo()
        val original = io.readFrom(file).properties("app", "dev").single() as PropertyImpl
        val context = DeclaringComponentImpl.copyOf(requireNotNull(original.declaringComponent))
        val decorated = original.copy(declaringComponent = context)
        val resolver = YamlResolvers.forValues(ExpressionResolver())

        listOf(original, decorated).forEach { property ->
            val resolved = property.resolveBy(resolver, context)
            val serialized = io.writeTo(directory.resolve("result.yaml").toFile()).serialize(listOf(resolved))
            assertEquals(mapOf("value" to "a\\tail"), Yaml().load<Map<String, Any>>(serialized))
            assertEquals(if (yaml) ValueFormat.YAML else ValueFormat.PROPERTIES, resolved.valueFormat)
        }
    }
}
