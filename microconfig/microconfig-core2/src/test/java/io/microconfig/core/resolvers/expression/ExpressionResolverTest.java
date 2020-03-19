package io.microconfig.core.resolvers.expression;

import io.microconfig.core.properties.StatementResolver.Statement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpressionResolverTest {
    private ExpressionResolver resolver = new ExpressionResolver();

    @Test
    void testStringApi() {
        assertEquals("5Wo", resolve("#{'Hello'.length() + 'World'.substring(0, 2)}"));
    }

    @Test
    void testPredefinedFunctions() {
        assertEquals("100m", resolve("#{#findGroup('Xmx(?<xmx>.+)', 'Xmx100m')}"));
    }

    @Test
    void testConditionalsWork() {
//        assertEquals("string2", resolve("#{(10 > 500) ? '${string1}' : '${string2}'}"));  //todo fix
        assertEquals("string2", resolve("#{(10 > 500) ? 'string1' : 'string2'}"));
    }

    @Test
    void testSimpleApi() {
        String value = "I'm #{ 1 + 2} !";
        Statement statement = resolver.findStatementIn(value).orElseThrow(IllegalStateException::new);
        assertEquals(4, statement.getStartIndex());
        assertEquals(13, statement.getEndIndex());
        assertEquals("#{ 1 + 2}", statement.toString());
        assertEquals("3", statement.resolve("dev", "app"));
    }

    private String resolve(String value) {
        return resolver.resolveRecursively(value, "dev", "app");
    }
}
