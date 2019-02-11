package io.microconfig.properties.resolver.placeholder;

import org.junit.Test;

import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PlaceholderTest {
    @Test
    public void testPlaceholderInsideSpell() {
        doTestSpel("#{${this@property:false}}");
        doTestSpel("#{ 4 + !${this@property:false} - 1}");
        doTestSpel("#{!${this@property:false} + 1}");
        doTestSpel("#{!${this@property:false}}");
        doTestSpel("#{ !${this@property:false} }");
        /*
         eureka.client.serviceUrl.defaultZone=http://${eureka.user}:${eureka.password}@${cr-eureka-cib@ip:${cr-eureka-alpha@ip}}:${cr-eureka-cib@server.port:${cr-eureka-alpha@server.port}}/eureka/
        th-server.v24=${th-server@th-server.v1}}
        th-server.v25=${th-server[uat]@th-server.v1}}
        */
    }

    private void doTestSpel(String value) {
        Matcher matcher = Placeholder.PATTERN_FOR_RESOLVE.matcher(value);
        assertTrue(matcher.find());

        Placeholder propValue = Placeholder.parse(matcher.group(), getEnv());
        assertEquals("property", propValue.getValue());
        assertEquals("false", propValue.getDefaultValue().orElse(null));
    }

    @Test
    public void testPropertyValuePlaceholder() {
        String value = "${component@property.name}";
        Placeholder propValue = Placeholder.parse(value, getEnv());
        assertEquals("component", propValue.getComponent());
        assertEquals("property.name", propValue.getValue());
    }

    @Test
    public void testPropertyValuePlaceholderWithEnv() {
        String value = "${component[dev]@property.name}";
        Placeholder propValue = Placeholder.parse(value, null);
        assertEquals("component", propValue.getComponent());
        assertEquals("dev", propValue.getEnvironment());
        assertEquals("property.name", propValue.getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSimplePropertyValue() {
        String value = "123";
        Placeholder.parse(value, null);
    }

    @Test
    public void testRegex() {
        Matcher matcher = Placeholder.PATTERN.matcher("${th-server2[dev]@th-server.poolSize}");

        assertTrue(matcher.find());
        assertEquals("th-server2", matcher.group("comp"));
        assertEquals("dev", matcher.group("env"));
        assertEquals("th-server.poolSize", matcher.group("value"));
        assertEquals(null, matcher.group("default"));

        matcher = Placeholder.PATTERN.matcher("${th-server2@th-server.poolSize}");
        assertTrue(matcher.find());
        assertEquals("th-server2", matcher.group("comp"));
        assertEquals(null, matcher.group("env"));
        assertEquals("th-server.poolSize", matcher.group("value"));
        assertEquals(null, matcher.group("default"));

        matcher = Placeholder.PATTERN.matcher("${unknown@prop:defValue123}");
        assertTrue(matcher.find());
        assertEquals("unknown", matcher.group("comp"));
        assertEquals(null, matcher.group("env"));
        assertEquals("prop", matcher.group("value"));
        assertEquals("defValue123", matcher.group("default"));
    }

    public String getEnv() {
        return "uat";
    }
}