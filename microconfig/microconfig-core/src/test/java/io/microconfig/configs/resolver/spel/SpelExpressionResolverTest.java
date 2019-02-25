package io.microconfig.configs.resolver.spel;

import io.microconfig.configs.Property;
import io.microconfig.configs.PropertySource;
import io.microconfig.configs.resolver.PropertyResolver;
import io.microconfig.configs.resolver.RootComponent;
import io.microconfig.environments.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpelExpressionResolverTest {
    @Mock
    PropertyResolver placeholderResolver;
    @Mock
    RootComponent context;

    @Test
    void testResolves() {
        Property spel = prop("#{'${some.test1.property}'.length() + '${some.test2.property}'.length()}");
        when(placeholderResolver.resolve(spel, context)).thenReturn("#{'someTestValue'.length() + 'someTestValue2'.length()}");
        SpelExpressionResolver resolver = new SpelExpressionResolver(placeholderResolver);
        String result = resolver.resolve(spel, context);
        assertEquals("27", result);
    }

    @Test
    void testDelegatesIfNotSpelExpression() {
        Property property = prop("${some.test1.property}-${some.test2.property}");
        when(placeholderResolver.resolve(property, context)).thenReturn("someValue");
        SpelExpressionResolver resolver = new SpelExpressionResolver(placeholderResolver);
        String result = resolver.resolve(property, context);
        assertEquals("someValue", result);
    }

    @Test
    void testExpressionsWork() {
        Property property = prop("#{${some.int.property1} * ${some.int.property2}}");
        when(placeholderResolver.resolve(property, context)).thenReturn("#{1000 * 2}");
        SpelExpressionResolver resolver = new SpelExpressionResolver(placeholderResolver);
        String result = resolver.resolve(property, context);
        assertEquals("2000", result);
    }

    @Test
    void testConditionalsWork() {
        Property spel = prop("#{(${some.int.property1} > 500) ? '${connection.string1}' : '${connection.string2}'}");
        when(placeholderResolver.resolve(spel, context))
                .thenReturn("#{(1000 > 500) ? 'connparams1' : 'connparams2'}");
        PropertyResolver resolver = new SpelExpressionResolver(placeholderResolver);
        String result = resolver.resolve(spel, context);
        assertEquals("connparams1", result);
    }

    static Property prop(String value) {
        return new Property("key", value, "uat", new PropertySource(Component.byType("c"), "c"));
    }
}
