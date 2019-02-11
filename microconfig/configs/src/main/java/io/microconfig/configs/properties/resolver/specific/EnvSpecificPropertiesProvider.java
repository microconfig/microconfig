package io.microconfig.configs.properties.resolver.specific;

import io.microconfig.configs.environment.*;
import io.microconfig.configs.properties.PropertiesProvider;
import io.microconfig.configs.properties.Property;
import io.microconfig.configs.properties.files.provider.ComponentTree;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import static deployment.util.StringUtils.unixLikePath;
import static io.microconfig.configs.properties.Property.Source.SYSTEM;
import static java.lang.System.getProperty;
import static java.util.Arrays.asList;

@RequiredArgsConstructor
public class EnvSpecificPropertiesProvider implements PropertiesProvider {
    private static final String PORT_OFFSET = "portOffset";
    private static final String IP = "ip";

    private static final String ENV = "env";
    private static final String NAME = "name";
    private static final String GROUP = "group";
    private static final String ORDER = "order";

    private static final String USER_HOME = "userHome";
    private static final String CONFIG_DIR = "configDir";
    private static final String SERVICE_DIR = "serviceDir";
    private static final String FOLDER = "folder";

    private final PropertiesProvider propertiesProvider;
    private final EnvironmentProvider environmentProvider;
    private final ComponentTree componentTree;
    private final File componentsDir;

    @Override
    public Map<String, Property> getProperties(Component component, String environment) {
        Map<String, Property> properties = propertiesProvider.getProperties(component, environment);
        addEnvSpecificProperties(component, environment, properties);
        return properties;
    }

    private void addEnvSpecificProperties(Component component, String envName, Map<String, Property> properties) {
        Environment environment;
        try {
            environment = environmentProvider.getByName(envName);
        } catch (EnvironmentNotExistException e) {
            return;
        }

        addPortOffset(properties, environment);
        addIp(properties, component, environment);
        addEnv(properties, environment);
        addName(properties, component, environment);
        addServiceProps(properties, component, environment);
        addConfigDir(properties, environment);
        addUserHome(properties, environment);
    }

    private void addPortOffset(Map<String, Property> properties, Environment environment) {
        environment.getPortOffset().ifPresent(p ->
                properties.putIfAbsent(PORT_OFFSET, new Property(PORT_OFFSET, p.toString(), environment.getName(), getSystemSource(), true))
        );
    }

    private void addIp(Map<String, Property> properties, Component component, Environment environment) {
        environment.getComponentGroupByComponentName(component.getName()).flatMap(ComponentGroup::getIp).ifPresent(ip -> doAdd(IP, ip, properties, environment, true));
    }

    private void addEnv(Map<String, Property> properties, Environment environment) {
        doAdd(ENV, environment.getName(), properties, environment, false);
    }

    private void addName(Map<String, Property> properties, Component component, Environment environment) {
        doAdd(NAME, component.getName(), properties, environment, false);
    }

    private void addServiceProps(Map<String, Property> properties, Component component, Environment environment) {
        Optional<ComponentGroup> componentGroup = environment.getComponentGroupByComponentName(component.getName());
        if (!componentGroup.isPresent()) return;

        int componentOrder = 1 + componentGroup.get().getComponentNames().indexOf(component.getName());
        doAdd(ORDER, String.valueOf(componentOrder), properties, environment, true);
        doAdd(GROUP, componentGroup.get().getName(), properties, environment, true);

        doAdd(SERVICE_DIR, new File(componentsDir, component.getName()).getAbsolutePath(), properties, environment, true);
        Optional<File> folder = componentTree.getFolder(component.getType());
        folder.ifPresent(file -> doAdd(FOLDER, file.getAbsolutePath(), properties, environment, true));
    }

    private void addConfigDir(Map<String, Property> properties, Environment environment) {
        String configDir = componentTree.getRepoDirRoot().getParentFile().getAbsolutePath();
        doAdd(CONFIG_DIR, unixLikePath(configDir), properties, environment, true);
    }

    private void addUserHome(Map<String, Property> properties, Environment environment) {
        doAdd(USER_HOME, unixLikePath(getProperty("user.home")), properties, environment, true);
    }

    private void doAdd(String name, String value, Map<String, Property> properties, Environment environment, boolean temp) {
        properties.put(name, new Property(name, value, environment.getName(), getSystemSource(), temp));
    }

    private Property.Source getSystemSource() {
        return new Property.Source(Component.byType(""), SYSTEM);
    }

    public static boolean isEnvSpecificProperty(String name) {
        return asList(ENV, NAME, PORT_OFFSET, IP, ORDER).contains(name);
    }
}