package io.microconfig.properties.resolver.spel;

import io.microconfig.environments.Component;
import io.microconfig.properties.Property;
import io.microconfig.properties.resolver.PropertyResolver;
import io.microconfig.properties.resolver.RootComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class SpelExpressionResolverTest {
    @Mock
    PropertyResolver placeholderResolver;
    @Mock
    RootComponent context;

    @Test
    public void testResolves() {
        Property spel = prop("#{'${some.test1.property}'.length() + '${some.test2.property}'.length()}");
        when(placeholderResolver.resolve(spel, context)).thenReturn("#{'someTestValue'.length() + 'someTestValue2'.length()}");
        SpelExpressionResolver resolver = new SpelExpressionResolver(placeholderResolver);
        String result = resolver.resolve(spel, context);
        assertEquals("27", result);
    }

    @Test
    public void testDelegatesIfNotSpelExpression() {
        Property property = prop("${some.test1.property}-${some.test2.property}");
        when(placeholderResolver.resolve(property, context)).thenReturn("someValue");
        SpelExpressionResolver resolver = new SpelExpressionResolver(placeholderResolver);
        String result = resolver.resolve(property, context);
        assertEquals("someValue", result);
    }

    @Test
    public void testExpressionsWork() {
        Property property = prop("#{${some.int.property1} * ${some.int.property2}}");
        when(placeholderResolver.resolve(property, context)).thenReturn("#{1000 * 2}");
        SpelExpressionResolver resolver = new SpelExpressionResolver(placeholderResolver);
        String result = resolver.resolve(property, context);
        assertEquals("2000", result);
    }

    @Test
    public void testConditionalsWork() {
        Property spel = prop("#{(${some.int.property1} > 500) ? '${connection.string1}' : '${connection.string2}'}");
        when(placeholderResolver.resolve(spel, context))
                .thenReturn("#{(1000 > 500) ? 'connparams1' : 'connparams2'}");
        PropertyResolver resolver = new SpelExpressionResolver(placeholderResolver);
        String result = resolver.resolve(spel, context);
        assertEquals("connparams1", result);
    }

    static Property prop(String value) {
        return new Property("key", value, "uat", new Property.Source(Component.byType("c"), "c"));
    }
}
