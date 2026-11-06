package io.microconfig.core.properties.resolvers.expression;

import io.microconfig.core.properties.ResolveException;
import io.microconfig.core.properties.resolvers.RecursiveResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionResolverTest {
    ExpressionResolver resolver = new ExpressionResolver();

    @Test
    void stringApi() {
        assertEquals("5Wo", resolve("#{'Hello'.length() + 'World'.substring(0, 2)}"));
    }

    @Test
    void conditionals() {
//        assertEquals("string2", resolve("#{(10 > 500) ? '${string1}' : '${string2}'}"));  //todo fix
        assertEquals("string2", resolve("#{(10 > 500) ? 'string1' : 'string2'}"));
    }

    @Test
    void predefinedFunctions() {
        assertEquals("100m", resolve("#{#findGroup('Xmx(?<xmx>.+)', 'Xmx100m')}"));
    }

    @Test
    void api() {
        String value = "I'm #{ 1 + 2} !";
        RecursiveResolver.Statement statement = resolver.findStatementIn(new StringBuilder(value)).orElseThrow(IllegalStateException::new);
        assertEquals(4, statement.getStartIndex());
        assertEquals(13, statement.getEndIndex());
        assertEquals("#{ 1 + 2}", statement.toString());
        assertEquals("3", statement.resolveFor(null, null));
    }

    @Test
    void nullResultIsReportedAsResolveError() {
        ResolveException e = assertThrows(ResolveException.class, () -> resolve("#{null}"));
        assertTrue(e.getMessage().contains("Can't evaluate #{null}"));
        assertTrue(e.getMessage().contains("Expression evaluated to null"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
            "#{3 * 60 * 1000}                                                 | 180000",
            "#{'2026-11-06'.split('-')[1]}                                    | 11",
            "#{'1.0-SNAPSHOT'.endsWith('-SNAPSHOT') ? 'snapshot' : 'release'} | snapshot",
            "#{'tcp://' + 'host' + ':' + 8080}                                | tcp://host:8080",
            "#{'a,b,c'.split(',').length}                                     | 3",
            "#{' a-b '.trim().toUpperCase().replace('-', '_')}                | A_B",
            "#{'%s-%s'.formatted('a', 'b')}                                   | a-b",
            "#{#base64('abc')}                                                | YWJj",
            "#{#currentTimeMillis() > 0}                                      | true",
    })
    void evaluateValueExpressions(String expression, String expected) {
        assertEquals(expected, resolve(expression));
    }

    @Test
    void uuid4Function() {
        assertTrue(resolve("#{#uuid4()}").matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void lineSeparatorFunction() {
        assertEquals("a" + System.lineSeparator() + "b", resolve("#{'a' + #lineSeparator() + 'b'}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "#{T(java.lang.Runtime).getRuntime().availableProcessors()}",
            "#{new java.lang.ProcessBuilder('sh').command().size()}",
            "#{T(java.nio.file.Files).exists(T(java.nio.file.Path).of('/'))}",
            "#{''.getClass().forName('java.lang.Runtime').getName()}",
            "#{T(Class).forName('java.lang.Runtime').getName()}",
            "#{T(Math).max(1, 2)}"
    })
    void rejectExpressionsThatReachJavaClasses(String expression) {
        assertThrows(ResolveException.class, () -> resolve(expression));
    }

    // #exit isn't checked here: if it were still registered, the test would stop the JVM
    @ParameterizedTest
    @ValueSource(strings = {
            "#{#setProperty('microconfig.expression.probe', 'changed')}",
            "#{#clearProperty('java.version')}",
            "#{#getenv('HOME')}",
            "#{#getProperty('java.version')}",
            "#{#load('/nonexistent/library.so')}",
            "#{#gc()}",
            "#{#readString('/etc/hosts')}",
            "#{#readStringOrEmpty('/etc/hosts')}",
            "#{#readBytes('/etc/hosts')}",
            "#{#readBytesOrEmpty('/etc/hosts')}"
    })
    void rejectSystemAndFileFunctions(String expression) {
        assertThrows(ResolveException.class, () -> resolve(expression));
        assertEquals(null, System.getProperty("microconfig.expression.probe"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "#{#currentTimeMillis.invoke(null)}",
            "#{#base64.getDeclaringClass().getName()}",
            "#{#base64.getName()}",
            "#{#base64.getClass().getName()}"
    })
    void rejectReflectionThroughRegisteredFunctions(String expression) {
        assertThrows(ResolveException.class, () -> resolve(expression));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
            "#{T(java.time.LocalDate).now()} | Java types can't be used in expressions",
            "#{new java.util.Date()}         | Java types can't be used in expressions",
            "#{#readString('file')}          | Available functions: #base64",
    })
    void explainWhatIsNotAvailableInExpressions(String expression, String hint) {
        ResolveException e = assertThrows(ResolveException.class, () -> resolve(expression));
        assertTrue(e.getMessage().contains(hint), e.getMessage());
    }

    private String resolve(String value) {
        return resolver.resolve(value, null, null);
    }
}
