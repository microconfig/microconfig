package io.microconfig.domain.impl.properties.resolvers.placeholder;

import io.microconfig.Microconfig;
import io.microconfig.domain.Property;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static io.microconfig.ClasspathUtils.classpathFile;
import static io.microconfig.Microconfig.searchConfigsIn;
import static io.microconfig.domain.impl.helpers.ConfigTypeFilters.configTypeWithName;

class PlaceholderTest {
    @Test
    void resolve() {
        Microconfig factory = searchConfigsIn(rootDir());

        List<Property> properties = factory
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