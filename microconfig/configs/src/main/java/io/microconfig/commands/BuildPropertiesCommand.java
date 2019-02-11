package io.microconfig.commands;

import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.properties.PropertiesProvider;
import io.microconfig.properties.Property;
import io.microconfig.properties.serializer.PropertySerializer;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static deployment.util.Logger.info;
import static deployment.util.Logger.logLineBreak;
import static java.lang.System.getProperty;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class BuildPropertiesCommand implements Command {
    private static final String SKIP_VALIDATION_PROPERTY = "skipValidation";

    private final EnvironmentProvider environmentProvider;
    private final PropertiesProvider propertiesProvider;
    private final PropertySerializer propertySerializer;

    private final PropertiesPostProcessor propertiesPostProcessor;

    @Override
    public void execute(CommandContext context) {
        List<Component> componentToBuild = collectComponents(context);
        int resultCount = componentToBuild.parallelStream()
                .mapToInt(component -> processComponent(component, context.getEnv()))
                .sum();

        if (resultCount > 0) {
            logLineBreak();
        }
    }

    private List<Component> collectComponents(CommandContext context) {
        Environment environment = environmentProvider.getByName(context.getEnv());
        environment.verifyIpsSet();

        List<Component> allComponents = getComponents(context, environment);
        return context.getComponents().isEmpty() ? allComponents : toComponents(context.getComponents(), allComponents, context.getEnv());
    }

    private List<Component> getComponents(CommandContext context, Environment environment) {
        return context.getComponentGroup().isPresent() ?
                environment.getComponentGroupByName(context.getComponentGroup().get()).getComponents()
                : environment.getComponentGroups().stream().flatMap(cg -> cg.getComponents().stream()).collect(toList());
    }

    private List<Component> toComponents(List<String> names, List<Component> allComponents, String env) {
        if (getProperty(SKIP_VALIDATION_PROPERTY) != null) {
            return names.stream().map(Component::byType).collect(toList());
        }

        return names.stream().map(name -> allComponents
                .stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Component '" + name + "' is not configured for " + env + " env"))).collect(toList());
    }

    private int processComponent(Component component, String env) {
        Map<String, Property> properties = propertiesProvider.getProperties(component, env);
        Optional<File> outputFile = propertySerializer.serialize(component.getName(), properties.values());

        outputFile.ifPresent(f -> {
            propertiesPostProcessor.process(f.getParentFile(), component.getName(), properties);
            info("Generated " + f.getName() + " for " + component.getName() + " [" + env + "]");
        });

        return outputFile.isPresent() ? 1 : 0;
    }
}