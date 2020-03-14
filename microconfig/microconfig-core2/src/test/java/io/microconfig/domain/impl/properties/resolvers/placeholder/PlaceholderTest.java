package io.microconfig.domain.impl.properties.resolvers.placeholder;

import io.microconfig.MicroconfigFactory;
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
        MicroconfigFactory factory = searchConfigsIn(rootDir());

        List<Property> properties = factory
                .inEnvironment("placeholderAsDefaultValue")
                .findComponentWithName("var", false)
                .getPropertiesFor(configTypeWithName("app"))
                .resolveBy(factory.resolver())
                .getProperties();
        System.out.println(properties);
    }

    private File rootDir() {
        return classpathFile("test-props");
    }
}