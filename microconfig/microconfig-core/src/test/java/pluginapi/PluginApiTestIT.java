package pluginapi;

import io.microconfig.commands.buildconfig.factory.ConfigType;
import io.microconfig.commands.buildconfig.factory.MicroconfigFactory;
import io.microconfig.commands.buildconfig.factory.StandardConfigType;
import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.EnvComponent;
import io.microconfig.configs.resolver.PropertyResolver;
import io.microconfig.configs.resolver.PropertyResolverHolder;
import io.microconfig.utils.MicronconfigTestFactory;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import static io.microconfig.configs.Property.parse;
import static io.microconfig.configs.PropertySource.fileSource;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.of;

public class PluginApiTestIT {
    public void placeholderValues() {
        System.out.println(envToValues("${ports@p1} ${ports@p2}", new File("source.yaml")));
    }

    public Map<String, String> envToValues(String lineWithPlaceholders, File currentFile) {
        Property property = parse(lineWithPlaceholders, "", fileSource(currentFile, -1, false));
        EnvComponent root = new EnvComponent(property.getSource().getComponent(), "");

        MicroconfigFactory factory = MicronconfigTestFactory.getFactory();
        PropertyResolver propertyResolver = ((PropertyResolverHolder) factory
                .newConfigProvider(configTypeFor(currentFile)))
                .getResolver();

        return allEnvs(factory)
                .stream()
                .collect(toMap(identity(), env -> resolve(property.withNewEnv(env), root, propertyResolver)));
    }

    private String resolve(Property property, EnvComponent root, PropertyResolver propertyResolver) {
        return propertyResolver.resolve(property, root);
    }

    private Set<String> allEnvs(MicroconfigFactory factory) {
        return factory.getEnvironmentProvider().getEnvironmentNames();
    }

    private ConfigType configTypeFor(File file) {
        UnaryOperator<String> fileExtension = currentFileName -> {
            int lastDot = currentFileName.lastIndexOf('.');
            if (lastDot >= 0) return currentFileName.substring(lastDot);

            throw new IllegalStateException("Current file doesn't have an extension. Unable to resolve component type.");
        };

        String ext = fileExtension.apply(file.getName());
        return of(StandardConfigType.values())
                .filter(ct -> ct.getConfigExtensions().stream().anyMatch(e -> e.equals(ext)))
                .map(StandardConfigType::type)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find ConfigType for extension " + ext));
    }
}
