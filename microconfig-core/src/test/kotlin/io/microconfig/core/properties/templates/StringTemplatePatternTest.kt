package io.microconfig.core.properties.templates

import io.microconfig.core.properties.templates.TemplatePattern.Companion.defaultPattern
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StringTemplatePatternTest {
    val templatePattern: TemplatePattern =
        defaultPattern().withTemplatePrefixes(listOf("microconfig.template.", "mgmt.template."))

    val microTemplate: String = "microconfig.template.logback.fromFile"

    val mgmtTemplate: String = "mgmt.template.t2.toFile"

    @Test
    fun startsWithTemplatePrefix() {
        assertFalse(templatePattern.startsWithTemplatePrefix("microconfig"))
        assertTrue(templatePattern.startsWithTemplatePrefix(microTemplate))
        assertTrue(templatePattern.startsWithTemplatePrefix(mgmtTemplate))
    }

    @Test
    fun extractTemplateName() {
        assertEquals("logback", templatePattern.extractTemplateName(microTemplate))
        assertEquals("t2", templatePattern.extractTemplateName(mgmtTemplate))
        assertThrows(
            IllegalArgumentException::class.java,
            { templatePattern.extractTemplateName("bad") },
        )
    }
}
