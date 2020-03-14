package io.microconfig.domain.impl.properties.resolvers.placeholder;

import io.microconfig.Microconfig;
import io.microconfig.domain.Property;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static io.microconfig.ClasspathUtils.classpathFile;
import static io.microconfig.Microconfig.searchConfigsIn;
import static io.microconfig.domain.impl.configtype.StandardConfigType.APPLICATION;
import static io.microconfig.domain.impl.helpers.ConfigTypeFilters.configType;

class PlaceholderTest {
    @Test
    void resolve() {
        Microconfig microconfig = searchConfigsIn(rootDir());

        List<Property> properties = microconfig
                .inEnvironment("placeholderAsDefaultValue")
                .findComponentWithName("simple", false)
                .getPropertiesFor(configType(APPLICATION))
                .resolveBy(microconfig.resolver())
                .getProperties();
        System.out.println(properties);
    }

    private File rootDir() {
        return classpathFile("test-props");
    }
}