package io.microconfig;

import io.microconfig.core.properties.Properties;

import static io.microconfig.ClasspathUtils.classpathFile;
import static io.microconfig.Microconfig.searchConfigsIn;
import static io.microconfig.core.configtypes.impl.ConfigTypeFilters.configType;
import static io.microconfig.core.configtypes.impl.StandardConfigType.APPLICATION;

public class MicroconfigTestOld {
    private final Microconfig microconfig = searchConfigsIn(classpathFile("repo"));



    private Properties buildComponent(String component, String env) {
        return microconfig.inEnvironment(env)
                .getOrCreateComponentWithName(component)
                .getPropertiesFor(configType(APPLICATION))
                .resolveBy(microconfig.resolver())
                .withoutTempValues();
    }
}
