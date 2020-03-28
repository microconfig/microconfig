package io.microconfig.core.properties.resolvers.expression;

import io.microconfig.core.properties.resolvers.RecursiveResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpressionResolverTest {
    private ExpressionResolver resolver = new ExpressionResolver();

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

    private String resolve(String value) {
        return resolver.resolve(value, null, null);
    }
}
