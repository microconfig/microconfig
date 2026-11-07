package io.microconfig.core.properties.resolvers.expression

import io.microconfig.core.properties.ResolveException
import io.microconfig.core.properties.resolvers.expression.impl.ExpressionResolver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class ExpressionResolverTest {
    private val resolver = ExpressionResolver()

    @Test
    fun stringApi() {
        assertEquals("5Wo", resolve("#{'Hello'.length() + 'World'.substring(0, 2)}"))
    }

    private fun resolve(value: String): String = resolver.resolve(value, null, null)

    @Test
    fun conditionals() {
        assertEquals("string2", resolve("#{(10 > 500) ? 'string1' : 'string2'}"))
    }

    @Test
    fun predefinedFunctions() {
        assertEquals("100m", resolve("#{#findGroup('Xmx(?<xmx>.+)', 'Xmx100m')}"))
    }

    @Test
    fun api() {
        val statement =
            resolver.findStatementIn(StringBuilder("I'm #{ 1 + 2} !")).orElseThrow {
                IllegalStateException()
            }
        assertEquals(4, statement.startIndex)
        assertEquals(13, statement.endIndex)
        assertEquals("#{ 1 + 2}", statement.toString())
        assertEquals("3", statement.resolveFor(null, null))
    }

    @Test
    fun nullResultIsReportedAsResolveError() {
        val error = assertThrows(ResolveException::class.java) { resolve("#{null}") }
        assertTrue(error.message.contains("Can't evaluate #{null}"))
        assertTrue(error.message.contains("Expression evaluated to null"))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        quoteCharacter = '"',
        value =
            [
                "#{3 * 60 * 1000} | 180000",
                "#{'2026-11-06'.split('-')[1]} | 11",
                "#{'1.0-SNAPSHOT'.endsWith('-SNAPSHOT') ? 'snapshot' : 'release'} | snapshot",
                "#{'tcp://' + 'host' + ':' + 8080} | tcp://host:8080",
                "#{'a,b,c'.split(',').length} | 3",
                "#{' a-b '.trim().toUpperCase().replace('-', '_')} | A_B",
                "#{'%s-%s'.formatted('a', 'b')} | a-b",
                "#{#base64('abc')} | YWJj",
                "#{#bytesBase64('hello'.getBytes())} | aGVsbG8=",
                "#{#bytesBase64({104, 101, 108, 108, 111})} | aGVsbG8=",
                "#{#findGroupOrDefault('absent', 'abc', 'fallback')} | fallback",
                "#{#delete('a-b-c', '-')} | abc",
                "#{#substringAfterFirst('a-b-c', '-')} | b-c",
                "#{#substringAfterLast('a-b-c', '-')} | c",
                "#{#removeLineSeparators('a' + #lineSeparator() + 'b')} | ab",
                "#{#currentTimeMillis() > 0} | true",
                "#{#findGroup('[0-9]{2}', 'a12')} | 12",
                "#{'a{b'} | a{b",
                "#{'a}b'} | a}b",
                "#{#{1 + 2} * 3} | 9",
                "#{ {1, 2}.size() } | 2",
                "#{ {'a', 'b'}.listIterator().next() } | a",
                "#{'a'.lines().iterator().next()} | a",
                "#{ {'a': 1}.entrySet().iterator().next().getValue() } | 1",
            ],
    )
    fun evaluateValueExpressions(expression: String, expected: String) {
        assertEquals(expected, resolve(expression))
    }

    @Test
    fun rejectRemovedUuidFunction() {
        val error = assertThrows(ResolveException::class.java) { resolve("#{#uuid4()}") }
        assertTrue(error.message.contains("Available functions"))
    }

    @Test
    fun lineSeparatorFunction() {
        assertEquals("a${System.lineSeparator()}b", resolve("#{'a' + #lineSeparator() + 'b'}"))
    }

    @Test
    fun treatBackslashBeforeDoubleQuoteAsLiteralSpelText() {
        assertEquals("4", resolve("#{\"abc\\\".length()}"))
        assertEquals("a\\\"b", resolve("#{\"a\\\"\"b\"}"))
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "#{T(java.lang.Runtime).getRuntime().availableProcessors()}",
                "#{new java.lang.ProcessBuilder('sh').command().size()}",
                "#{T(java.nio.file.Files).exists(T(java.nio.file.Path).of('/'))}",
                "#{''.getClass().forName('java.lang.Runtime').getName()}",
                "#{T(Class).forName('java.lang.Runtime').getName()}",
                "#{T(Math).max(1, 2)}",
            ]
    )
    fun rejectExpressionsThatReachJavaClasses(expression: String) {
        assertThrows(ResolveException::class.java) { resolve(expression) }
    }

    // Calling #exit here could stop the test JVM if it were accidentally registered.
    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "#{#setProperty('microconfig.expression.probe', 'changed')}",
                "#{#clearProperty('java.version')}",
                "#{#getenv('HOME')}",
                "#{#getProperty('java.version')}",
                "#{#load('/nonexistent/library.so')}",
                "#{#gc()}",
                "#{#readString('/etc/hosts')}",
                "#{#readStringOrEmpty('/etc/hosts')}",
                "#{#readBytes('/etc/hosts')}",
                "#{#readBytesOrEmpty('/etc/hosts')}",
            ]
    )
    fun rejectSystemAndFileFunctions(expression: String) {
        assertThrows(ResolveException::class.java) { resolve(expression) }
        assertNull(System.getProperty("microconfig.expression.probe"))
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "#{#currentTimeMillis.invoke(null)}",
                "#{#base64.getDeclaringClass().getName()}",
                "#{#base64.getName()}",
                "#{#base64.getClass().getName()}",
            ]
    )
    fun rejectReflectionThroughRegisteredFunctions(expression: String) {
        assertThrows(ResolveException::class.java) { resolve(expression) }
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        quoteCharacter = '"',
        value =
            [
                "#{T(java.time.LocalDate).now()} | Java types can't be used in expressions",
                "#{new java.util.Date()} | Java types can't be used in expressions",
                "#{#readString('file')} | Available functions: #base64",
            ],
    )
    fun explainWhatIsNotAvailableInExpressions(expression: String, hint: String) {
        val error = assertThrows(ResolveException::class.java) { resolve(expression) }
        assertTrue(error.message.contains(hint), error.message)
    }
}
