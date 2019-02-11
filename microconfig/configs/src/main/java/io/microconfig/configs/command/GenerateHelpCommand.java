package io.microconfig.configs.command;

import io.microconfig.configs.environment.Component;
import io.microconfig.configs.environment.Environment;
import io.microconfig.configs.environment.EnvironmentProvider;
import io.microconfig.configs.properties.files.provider.ComponentTree;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static deployment.util.FileUtils.copy;
import static java.lang.System.getProperty;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class GenerateHelpCommand implements Command {
    private static final String SKIP_VALIDATION_PROPERTY = "skipValidation";

    private final EnvironmentProvider environmentProvider;
    private final ComponentTree componentTree;
    private final Path destRootDir;

    @Override
    public void execute(CommandContext context) {
        List<Component> componentToBuild = collectComponents(context);
        componentToBuild.forEach(this::processComponent);
    }

    private List<Component> collectComponents(CommandContext context) {
        Environment environment = environmentProvider.getByName(context.getEnv());

        List<Component> allComponents = getComponents(context, environment);
        return context.getComponents().isEmpty() ? allComponents : toComponents(context.getComponents(), allComponents, context.getEnv());
    }

    private List<Component> getComponents(CommandContext context, Environment environment) {
        return context.getComponentGroup().isPresent() ?
                environment.getComponentGroupByName(context.getComponentGroup().get()).getComponents()
                : environment.getComponentGroups().stream()
                .flatMap(cg -> cg.getComponents().stream())
                .collect(toList());
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

    private void processComponent(Component component) {
        String componentName = component.getName();

        findSourceHelpFile(componentName)
                .ifPresent(path -> copy(path.toPath(), resolveDestHelpFile(componentName)));
    }

    private Optional<File> findSourceHelpFile(String componentName) {
        List<File> helpFiles = componentTree.getPropertyFiles(componentName, p -> p.getName().equals("help.txt")).collect(toList());

        if (helpFiles.isEmpty()) return empty();
        if (helpFiles.size() == 1) return of(helpFiles.get(0));
        throw new IllegalArgumentException("Multiple help files found for component " + componentName);
    }

    private Path resolveDestHelpFile(String componentName) {
        Path componentRootPath = destRootDir.resolve(componentName);
        Path mgmtSubdir = componentRootPath.resolve(".mgmt");
        return mgmtSubdir.resolve("help.txt");
    }
}