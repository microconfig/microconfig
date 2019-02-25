package io.microconfig.commands;

import io.microconfig.configs.ConfigProvider;
import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.RootComponent;
import io.microconfig.configs.serializer.ConfigSerializer;
import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import io.microconfig.environments.EnvironmentProvider;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.microconfig.utils.Logger.info;
import static io.microconfig.utils.Logger.logLineBreak;
import static java.lang.System.getProperty;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class BuildConfigCommand implements Command {
    private static final String SKIP_VALIDATION_PROPERTY = "skipValidation";

    private final EnvironmentProvider environmentProvider;
    private final ConfigProvider configProvider;
    private final ConfigSerializer configSerializer;

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
        List<Component> allComponents = getComponents(context);
        return context.getComponents().isEmpty() ?
                allComponents
                : toComponents(context.getComponents(), allComponents, context.getEnv());
    }

    private List<Component> getComponents(CommandContext context) {
        Environment environment = environmentProvider.getByName(context.getEnv());

        return context.getComponentGroup().isPresent() ?
                environment.getGroupByName(context.getComponentGroup().get())
                        .getComponents()
                : environment.getComponentGroups()
                .stream()
                .flatMap(cg -> cg.getComponents().stream())
                .collect(toList());
    }

    private List<Component> toComponents(List<String> names, List<Component> allComponents, String env) {
        if (getProperty(SKIP_VALIDATION_PROPERTY) != null) {
            return names.stream()
                    .map(Component::byType)
                    .collect(toList());
        }

        return names.stream()
                .map(name -> allComponents.stream()
                        .filter(c -> c.getName().equals(name))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Component '" + name + "' is not configured for " + env + " env")))
                .collect(toList());
    }

    private int processComponent(Component component, String env) {
        Map<String, Property> properties = configProvider.getProperties(component, env);
        Optional<File> outputFile = configSerializer.serialize(component.getName(), properties.values());

        outputFile.ifPresent(f -> {
            propertiesPostProcessor.process(new RootComponent(component, env), f.getParentFile(), properties, configProvider);
            info("Generated " + f.getName() + " for " + component.getName() + " [" + env + "]");
        });

        return outputFile.isPresent() ? 1 : 0;
    }
}