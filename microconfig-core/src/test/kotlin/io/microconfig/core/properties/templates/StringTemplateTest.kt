package io.microconfig.core.properties.templates

import io.microconfig.core.ClasspathReader.Companion.classpathFile
import io.microconfig.core.Microconfig.Companion.searchConfigsIn
import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.templates.TemplatePattern.Companion.defaultPattern
import io.microconfig.core.properties.templates.impl.StringTemplate
import java.io.File
import java.util.function.UnaryOperator
import java.util.regex.Pattern
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class StringTemplateTest {
    val root: DeclaringComponent = DeclaringComponentImpl("app", "th-server", "uat")

    val templatePattern: Pattern = defaultPattern().placeholderPattern

    val source: File = File("source")

    val destination: File = File("destination")

    @ParameterizedTest
    @ValueSource(strings = ["\${ip:default}", "\${this@ip}"])
    fun resolveIp(placeholder: String) {
        resolve(placeholder, "172.30.162.3")
    }

    @ParameterizedTest
    @ValueSource(strings = ["default", ""])
    fun resolveDefaultValue(defaultValue: String) {
        resolve("\${param:" + defaultValue + "}", defaultValue)
    }

    @Test
    fun testEscaped() {
        resolve("\\\${param:default}", "\${param:default}")
    }

    @Test
    fun testExtendedPattern() {
        resolve("aaa\${env}bbb\\\${xxx}ccc\${missing_param:---}ddd", "aaauatbbb\${xxx}ccc---ddd")
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "\${missing:\${this@ip}}",
                "\${ip:\${this@missing}}",
                "\${this@missing:\${this@ip}}",
                "\${this@ip:\${this@missing}}",
            ]
    )
    fun resolveNestedDefaultValue(placeholder: String) {
        resolve(placeholder, "172.30.162.3")
    }

    @ParameterizedTest
    @ValueSource(strings = ["\${missing:{this@ip}}", "\${this@missing:{this@ip}}"])
    fun preserveBracesInLiteralDefaultValue(placeholder: String) {
        resolve(placeholder, "{this@ip}")
    }

    @Test
    fun resolveExpressionInDefaultValue() {
        resolve("\${this@missing:#{1+1}}", "2")
    }

    @Test
    fun resolveLongDefaultValue() {
        val defaultValue: String = "x".repeat(100000)
        resolve("\${this@missing:" + defaultValue + "}", defaultValue)
    }

    @Test
    fun templateNameWithoutBrackets() {
        val template: StringTemplate =
            StringTemplate(
                "name[12]",
                source,
                destination,
                TemplatePattern.DEFAULT_PLACEHOLDER_PATTERN,
                "content",
            )
        assertEquals("name", template.templateNameWithoutBrackets())
    }

    @Test
    fun testEnvProperties() {
        val entry = System.getenv().entries.first()
        val result: String =
            StringTemplate(
                    "name",
                    source,
                    destination,
                    templatePattern,
                    "\${env@" + entry.key + "}",
                )
                .resolveBy(resolver(), root)
                .content
        val escape: UnaryOperator<String> =
            UnaryOperator<String> { v -> v.replace(Regex("\\\\+"), "/") }
        assertEquals(escape.apply(entry.value), escape.apply(result))
    }

    @Test
    fun testSystemProperties() {
        resolve("\${system@user.name}", System.getProperty("user.name"))
    }

    @Test
    fun insertPlainTextOfPropertiesValues() {
        val result: String =
            StringTemplate(
                    "name",
                    classpathFile("templates/templateWithPropertiesValues.txt"),
                    destination,
                    templatePattern,
                )
                .resolveBy(resolver(), DeclaringComponentImpl("app", "propertiesYamlTarget", "dev"))
                .content
        assertEquals("win=C:\\temp\\new\nraw=x\\y", result.replace("\r\n", "\n").trim())
    }

    @Test
    fun testMultiLinePlaceholderInTemplate() {
        val result: String =
            StringTemplate(
                    "name",
                    classpathFile("templates/templateWithMultiLines.yaml"),
                    destination,
                    templatePattern,
                )
                .resolveBy(resolver(), DeclaringComponentImpl("app", "mergeLists", "some"))
                .content
        assertEquals(
            "key1:\n  key2:\n    key3:\n      - name: n1\n        v: v2\n      - v1\n      - v2\n      - v3\n      - v4\n      - v5\n\n\nkey10: mergeLists",
            result.replace("\r\n", "\n").trim { it <= ' ' },
        )
    }

    private fun resolver(): Resolver {
        return searchConfigsIn(classpathFile("repo")).resolver()
    }

    private fun resolve(placeholder: String, expected: String) {
        val template: StringTemplate =
            StringTemplate("name", source, destination, templatePattern, placeholder)
        val result: String = template.resolveBy(resolver(), root).content
        assertEquals(expected, result)
    }
}
