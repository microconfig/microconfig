package io.microconfig.domain.impl.properties.resolvers.placeholder;

import io.microconfig.domain.Property;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static io.microconfig.ClasspathUtils.classpathFile;
import static io.microconfig.MicroconfigFactory.searchConfigsIn;
import static io.microconfig.domain.impl.helpers.ConfigTypeFilters.configTypeWithName;

class PlaceholderTest {
    @Test
    void resolve() {
        List<Property> properties = searchConfigsIn(rootDir())
                .inEnvironment("placeholderAsDefaultValue")
                .findComponentWithName("var", false)
                .getPropertiesFor(configTypeWithName("app"))
                .getProperties();
        System.out.println(properties);
    }

    private File rootDir() {
        return classpathFile("test-props");
    }
}