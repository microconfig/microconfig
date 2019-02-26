package mgmt.microconfig;

import io.microconfig.commands.Command;
import io.microconfig.commands.CommandContext;
import io.microconfig.configs.files.provider.ComponentTree;
import io.microconfig.environments.Component;
import io.microconfig.environments.EnvironmentProvider;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static io.microconfig.utils.FileUtils.copy;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
class CopyHelpFilesCommand implements Command {
    private final EnvironmentProvider environmentProvider;
    private final ComponentTree componentTree;
    private final Path destRootDir;

    @Override
    public void execute(CommandContext context) {
        context.components(environmentProvider)
                .forEach(this::processComponent);
    }

    private void processComponent(Component component) {
        String componentName = component.getName();
        findSourceHelpFile(componentName)
                .ifPresent(path -> copy(path.toPath(), resolveDestHelpFile(componentName)));
    }

    private Optional<File> findSourceHelpFile(String componentName) {
        List<File> helpFiles = componentTree.getConfigFiles(componentName, p -> p.getName().equals("help.txt")).collect(toList());

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