native-image \
    --initialize-at-build-time=io.microconfig.core.properties.PropertiesRepository \
    --initialize-at-build-time=io.microconfig.core.properties.PropertiesFactory \
    --initialize-at-build-time=io.microconfig.core.properties.PlaceholderResolveStrategy \
    --initialize-at-build-time=io.microconfig.core.environments.EnvironmentRepository \
    --initialize-at-build-time=io.microconfig.core.configtypes.ConfigTypeRepository \
    --initialize-at-build-time=io.microconfig.core.configtypes.ConfigType \
    --initialize-at-build-time=io.microconfig.core.properties.io.selector.ConfigFormatDetector \
    --initialize-at-build-time=io.microconfig.core.environments.ComponentFactory \
    -jar microconfig-cli/build/libs/microconfig-cli-4.1.1-all.jar
        #    -H:+PrintClassInitialization \
