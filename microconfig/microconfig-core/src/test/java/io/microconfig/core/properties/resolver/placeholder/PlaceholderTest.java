package io.microconfig.core.properties.resolver.placeholder;

import io.microconfig.core.properties.resolver.PropertyResolveException;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.microconfig.core.properties.resolver.placeholder.Placeholder.parse;
import static org.junit.jupiter.api.Assertions.*;

class PlaceholderTest {
    @Test
    void testIsPlaceholder() {
        assertTrue(Placeholder.isSinglePlaceholder("${this@property:false}"));
        assertTrue(Placeholder.isSinglePlaceholder("${this[dev]@property:false}"));
        assertTrue(Placeholder.isSinglePlaceholder("${app::this@property:false}"));
        assertTrue(Placeholder.isSinglePlaceholder("${VAULT@/secret/prod/db.user}"));
        assertFalse(Placeholder.isSinglePlaceholder("${app:this@property:false}"));
        assertFalse(Placeholder.isSinglePlaceholder("#{${this@property:false}}"));

//        assertFalse(Placeholder.isSinglePlaceholder("${this@property:false} this2property2}"));
//        assertFalse(Placeholder.isSinglePlaceholder("${this@property:false} ${this@property:false}"));
//        assertFalse(Placeholder.isSinglePlaceholder("${this@property:${this@property2}}"));
    }

    @Test
    void testToString() {
        assertEquals("${component[dev]@property}", parse("${component@property}", "dev").toString());
        assertEquals("${component[prod]@property}", parse("${component[prod]@property}", "dev").toString());
        assertEquals("${component[test]@property:default}", parse("${component[test]@property:default}", "dev").toString());
    }

    @Test
    void testPlaceholderInsideSpell() {
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
        Matcher matcher = Placeholder.PLACEHOLDER_INSIDE_LINE.matcher(value);
        assertTrue(matcher.find());

        Placeholder propValue = parse(matcher.group(), getEnv());
        assertEquals("property", propValue.getValue());
        assertEquals("false", propValue.getDefaultValue().orElse(null));
    }

    @Test
    void testPropertyValuePlaceholder() {
        String value = "${component@property.name}";
        Placeholder propValue = parse(value, getEnv());
        assertEquals("component", propValue.getComponent());
        assertEquals("property.name", propValue.getValue());
    }

    @Test
    void testPropertyValuePlaceholderWithEnv() {
        String value = "${component[dev]@property.name}";
        Placeholder propValue = parse(value, null);
        assertEquals("component", propValue.getComponent());
        assertEquals("dev", propValue.getEnvironment());
        assertEquals("property.name", propValue.getValue());
    }

    @Test
    void testSimplePropertyValue() {
        assertThrows(PropertyResolveException.class, () -> parse("123", null));
    }

    @Test
    void testRegex() {
        Matcher matcher = Placeholder.SINGE_PLACEHOLDER.matcher("${th-server2[dev]@th-server.poolSize}");

        assertTrue(matcher.find());
        assertEquals("th-server2", matcher.group("comp"));
        assertEquals("dev", matcher.group("env"));
        assertEquals("th-server.poolSize", matcher.group("value"));
        assertNull(matcher.group("default"));

        matcher = Placeholder.SINGE_PLACEHOLDER.matcher("${th-server2@th-server.poolSize}");
        assertTrue(matcher.find());
        assertEquals("th-server2", matcher.group("comp"));
        assertNull(matcher.group("env"));
        assertEquals("th-server.poolSize", matcher.group("value"));
        assertNull(matcher.group("default"));

        matcher = Placeholder.SINGE_PLACEHOLDER.matcher("${unknown@prop:defValue123}");
        assertTrue(matcher.find());
        assertEquals("unknown", matcher.group("comp"));
        assertNull(matcher.group("env"));
        assertEquals("prop", matcher.group("value"));
        assertEquals("defValue123", matcher.group("default"));
    }

    String getEnv() {
        return "uat";
    }
}