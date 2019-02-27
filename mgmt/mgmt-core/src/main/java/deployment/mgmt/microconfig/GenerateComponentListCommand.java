package deployment.mgmt.microconfig;

import io.microconfig.commands.Command;
import io.microconfig.commands.CommandContext;
import io.microconfig.environments.Component;
import io.microconfig.environments.EnvironmentProvider;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static io.microconfig.utils.FileUtils.write;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
class GenerateComponentListCommand implements Command {
    private final File serviceDir;
    private final EnvironmentProvider environmentProvider;

    @Override
    public void execute(CommandContext context) {
        List<String> componentNames = context.components(environmentProvider)
                .stream()
                .map(Component::getName)
                .collect(toList());

        write(componentListPath(), componentNames);
    }

    private Path componentListPath() {
        return new File(new File(serviceDir, ".mgmt"), "mgmt.clist").toPath();
    }
}