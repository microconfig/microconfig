package io.microconfig.core.properties.resolver.placeholder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlaceholderTest {
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
        Placeholder propValue = PlaceholderBorder.parse(new StringBuilder(value)).toPlaceholder(getEnv());

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
    void testException() {
        assertThrows(IllegalStateException.class, () -> parse("123", null));
    }

    String getEnv() {
        return "uat";
    }

    private Placeholder parse(String line, String env) {
        return PlaceholderBorder.parse(new StringBuilder(line)).toPlaceholder(env);
    }
}