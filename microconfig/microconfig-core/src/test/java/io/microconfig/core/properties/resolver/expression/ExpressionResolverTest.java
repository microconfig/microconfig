package io.microconfig.core.properties.resolver.expression;

import io.microconfig.core.environments.Component;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.resolver.EnvComponent;
import io.microconfig.core.properties.resolver.PropertyResolveException;
import io.microconfig.core.properties.resolver.PropertyResolver;
import io.microconfig.core.properties.sources.SpecialSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.microconfig.core.properties.resolver.expression.Expression.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpressionResolverTest {
    @Mock
    PropertyResolver placeholderResolver;
    @Mock
    EnvComponent context;

    @Test
    void testResolves() {
        Property spel = prop("#{'${some.test1.property}'.length() + '${some.test2.property}'.length()}");
        when(placeholderResolver.resolve(spel, context)).thenReturn("#{'someTestValue'.length() + 'someTestValue2'.length()}");
        ExpressionResolver resolver = new ExpressionResolver(placeholderResolver);
        String result = resolver.resolve(spel, context);
        assertEquals("27", result);
    }

    @Test
    void testDelegatesIfNotSpelExpression() {
        Property property = prop("${some.test1.property}-${some.test2.property}");
        when(placeholderResolver.resolve(property, context)).thenReturn("someValue");
        ExpressionResolver resolver = new ExpressionResolver(placeholderResolver);
        String result = resolver.resolve(property, context);
        assertEquals("someValue", result);
    }

    @Test
    void testExpressionsWork() {
        Property property = prop("#{${some.int.property1} * ${some.int.property2}}");
        when(placeholderResolver.resolve(property, context)).thenReturn("#{1000 * 2}");
        ExpressionResolver resolver = new ExpressionResolver(placeholderResolver);
        String result = resolver.resolve(property, context);
        assertEquals("2000", result);
    }

    @Test
    void testConditionalsWork() {
        Property spel = prop("#{(${some.int.property1} > 500) ? '${connection.string1}' : '${connection.string2}'}");
        when(placeholderResolver.resolve(spel, context))
                .thenReturn("#{(1000 > 500) ? 'connparams1' : 'connparams2'}");
        PropertyResolver resolver = new ExpressionResolver(placeholderResolver);
        String result = resolver.resolve(spel, context);
        assertEquals("connparams1", result);
    }

    @Test
    void testException() {
        assertThrows(PropertyResolveException.class, () -> parse("fsdfd"));
    }

    @Test
    void testToString() {
        String value = "#{ 1 + 2}";
        Expression expression = parse(value);
        assertEquals(value, expression.toString());
    }

    private Property prop(String value) {
        return Property.property("key", value, "uat", new SpecialSource(Component.byType("c"), "c"));
    }
}
