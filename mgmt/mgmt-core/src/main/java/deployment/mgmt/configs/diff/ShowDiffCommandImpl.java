package deployment.mgmt.configs.diff;

import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.properties.PropertyService;
import io.microconfig.properties.io.ConfigIo;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.microconfig.utils.ConsoleColor.*;
import static io.microconfig.utils.IoUtils.readFully;
import static io.microconfig.utils.Logger.*;
import static java.util.stream.Stream.of;

@RequiredArgsConstructor
public class ShowDiffCommandImpl implements ShowDiffCommand {
    private final ComponentGroupService componentGroupService;
    private final PropertyService propertyService;
    private final DeployFileStructure deployFileStructure;
    private final ConfigIo configIo;

    @Override
    public void showPropDiff(String... services) {
        doShow(services, deployFileStructure.service()::getDiffFile, f -> configIo.read(f).forEach(this::colorOutput));
    }

    @Override
    public void showClasspathDiff(String... services) {
        doShow(services, deployFileStructure.process()::getClasspathDiffFile, f -> info(readFully(f)));
    }

    @Override
    public void printProperties(String key) {
        componentGroupService.getServices().forEach(s -> {
            String process = propertyService.getProcessProperties(s).getOrDefault(key, "");
            String system = propertyService.getServiceProperties(s).getOrDefault(key, "");
            info(green(s) + " -> " + (yellow(process) + (system.isEmpty() ? "" : " " + system)));
        });
    }

    private void doShow(String[] services, Function<String, File> fileFetcher, Consumer<File> writer) {
        of(services).forEach(s -> {
            File file = fileFetcher.apply(s);
            if (!file.exists()) return;

            announce(s + ":");
            writer.accept(file);
            logLineBreak();
        });
    }

    private void colorOutput(String key, String value) {
        if (key.startsWith("+")) {
            key = key.replaceFirst("\\+", green("+"));
        } else if (key.startsWith("-")) {
            key = key.replaceFirst("-", red("-"));
        } else {
            key = " " + key;
        }

        info(key + " = " + value.replaceFirst(" -> ", green(" -> ")));
    }
}