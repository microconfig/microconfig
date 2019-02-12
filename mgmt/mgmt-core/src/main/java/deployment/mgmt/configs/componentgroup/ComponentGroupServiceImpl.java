package deployment.mgmt.configs.componentgroup;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.configs.service.properties.PropertyService;
import lombok.AllArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static io.microconfig.utils.FileUtils.delete;
import static io.microconfig.utils.FileUtils.write;
import static io.microconfig.utils.IoUtils.lines;
import static io.microconfig.utils.IoUtils.readFirstLine;
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.Logger.warn;
import static io.microconfig.utils.PropertiesUtils.loadPropertiesAsMap;
import static io.microconfig.utils.PropertiesUtils.readProperties;
import static io.microconfig.utils.StringUtils.isEmpty;
import static java.nio.file.Files.readAllLines;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@AllArgsConstructor
public class ComponentGroupServiceImpl implements ComponentGroupService {
    private static final String CONFIG_VERSION_KEY = "config.version";
    private static final String PROJECT_VERSION_KEY = "project.version";

    private final DeployFileStructure deployFileStructure;
    private final PropertyService propertyService;

    @Override
    public void update(GroupDescription description) {
        write(deployFileStructure.deploy().getGroupDescriptionFile(), description.getEnv() + " " + description.getGroup());
        cleanAlteredVersions();
    }

    @Override
    public void updateConfigVersion(String configVersion) {
        File configVersionFile = deployFileStructure.configs().getConfigVersionFile();
        write(configVersionFile, CONFIG_VERSION_KEY + "=" + configVersion);
    }

    @Override
    public void updateProjectVersion(String fullVersionOrPostfix) {
        boolean isPostfix = fullVersionOrPostfix.startsWith("-") || fullVersionOrPostfix.startsWith(".");
        File projectVersionFile = deployFileStructure.configs().getProjectVersionFile(null);
        if (!projectVersionFile.exists()) {
            throw new IllegalStateException("Can't find project version file " + projectVersionFile);
        }

        String version = isPostfix ? requireNonNull(readProperties(projectVersionFile).get(PROJECT_VERSION_KEY)) + fullVersionOrPostfix : fullVersionOrPostfix;
        File envVersionFile = deployFileStructure.configs().getProjectVersionFile(getEnv());
        write(envVersionFile, PROJECT_VERSION_KEY + "=" + version);
    }

    @Override
    public String getProjectVersion() {
        File projectVersionFile = deployFileStructure.configs().getProjectVersionFile(getEnv());
        return loadPropertiesAsMap(projectVersionFile).get(PROJECT_VERSION_KEY);
    }

    @Override
    public GroupDescription getDescription() {
        File envFile = deployFileStructure.deploy().getGroupDescriptionFile();
        String envDescription = readFirstLine(envFile);
        if (isEmpty(envDescription)) {
            throw new IllegalStateException("Can't find env description in " + envFile);
        }

        String[] parts = envDescription.split(" ");
        return new GroupDescription(parts[0], parts[1]);
    }

    @Override
    public List<String> getServices() {
        File file = deployFileStructure.service().getServiceListFile();
        if (!file.exists()) return emptyList();

        try {
            return readAllLines(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void changeServiceVersion(ServiceDescription newService) {
        List<ServiceDescription> alteredServices = new ArrayList<>(getAlteredServices());
        alteredServices.removeIf(s -> s.getName().equals(newService.getName()));
        alteredServices.add(newService);

        writeAlteredServices(alteredServices);

        announce("Changed " + newService.getName() + " version to " + newService.getVersion()
                + ". Please restart services to apply changes.");
    }

    @Override
    public List<ServiceDescription> getAlteredServices() {
        File file = alteredVersionsFile();
        if (!file.exists()) return emptyList();

        try (Stream<String> lines = lines(file.toPath())) {
            return lines.map(this::toDescription).collect(toList());
        }
    }

    @Override
    public Optional<ServiceDescription> getAlteredVersionService(String service) {
        return getAlteredServices().stream()
                .filter(s -> s.getName().equals(service))
                .findAny();
    }

    @Override
    public void cleanAlteredVersions(String... services) {
        if (services.length == 0) {
            writeAlteredServices(emptyList());
        } else {
            List<ServiceDescription> alteredServices = getAlteredServices();
            alteredServices.removeIf(s -> asList(services).contains(s.getName()));
            writeAlteredServices(alteredServices);
        }
    }

    @Override
    public void replaceServiceVersionWithAltered(String... services) {
        getAlteredServices().stream()
                .filter(s -> propertyService.serviceExists(s.getName()))
                .filter(s -> services.length == 0 || asList(services).contains(s.getName()))
                .forEach(s -> {
                    ProcessProperties props = propertyService.getProcessProperties(s.getName());
                    if (props.getVersion().equals(s.getVersion())) return;

                    warn("Overriding version for " + s.getName() + "."
                            + " Version from config: " + props.getVersion() + ". Overridden version: " + s.getVersion()
                            + ". Use mgmt 'reset-configs' or 'init' to revert version from configs");

                    props.changeVersion(s.getVersion());
                });
    }

    private ServiceDescription toDescription(String line) {
        String[] parts = line.split(":");
        return new ServiceDescription(parts[0], parts[1]);
    }

    private void writeAlteredServices(List<ServiceDescription> services) {
        File alteredServicesFile = alteredVersionsFile();
        if (services.isEmpty()) {
            delete(alteredServicesFile);
            return;
        }

        String content = services.stream()
                .map(s -> s.getName() + ":" + s.getVersion())
                .collect(joining("\n"));

        write(alteredServicesFile, content);
    }

    private File alteredVersionsFile() {
        return deployFileStructure.deploy().getAlteredVersionsFile();
    }
}
