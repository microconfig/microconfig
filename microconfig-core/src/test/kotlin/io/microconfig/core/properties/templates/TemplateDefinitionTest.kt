package io.microconfig.core.properties.templates

import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.TypedProperties
import io.microconfig.core.properties.templates.TemplatePattern.Companion.defaultPattern
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class TemplateDefinitionTest {
    @Test
    fun binaryTemplateWithoutSourceIsReportedAsIncomplete() {
        val definition = TemplateDefinition("file", "bin", defaultPattern())
        definition.setToFile("out.bin")
        val e: IllegalStateException =
            assertThrows(
                IllegalStateException::class.java,
                { definition.resolve(resolver(), properties()) },
            )
        assertTrue(e.message.orEmpty().contains("Template error: templateName: \'bin\'"))
        assertTrue(e.message.orEmpty().contains("Incomplete template def"))
    }

    private fun resolver(): Resolver {
        val resolver: Resolver = mock(Resolver::class.java)
        `when`(resolver.resolve(anyString(), any(), any())).thenReturn("result")
        return resolver
    }

    private fun properties(): TypedProperties {
        val properties: TypedProperties = mock(TypedProperties::class.java)
        `when`(properties.declaringComponent).thenReturn(mock(DeclaringComponent::class.java))
        return properties
    }
}
