package deployment.mgmt.configs.service.properties.impl;

import deployment.mgmt.configs.service.properties.JavaAppSettings;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import static io.microconfig.utils.OsUtil.isWindows;
import static io.microconfig.utils.StringUtils.isEmpty;
import static io.microconfig.utils.StringUtils.replaceMultipleSpaces;
import static java.io.File.pathSeparator;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static mgmt.utils.ProcessUtil.currentJavaPath;

@RequiredArgsConstructor
public class JavaAppSettingsImpl implements JavaAppSettings {
    private final ProcessProperties processProperties;

    @Override
    public String getJavaPath() {
        String path = processProperties.get("java.path");

        if (isWindows() && path.startsWith("/")) {
            return currentJavaPath();
        }

        return path;
    }

    @Override
    public String getClasspathPrepend() {
        return processProperties.findByPrefix("java.classpath.prepend")
                .sorted(comparing(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .collect(joining(pathSeparator, "", pathSeparator));
    }

    @Override
    public String getJavaOpts() {
        return processProperties.get("java.opts");
    }

    @Override
    public List<String> getJavaOptsAsList() {
        return toList(getJavaOpts());
    }

    @Override
    public String getProcessArgs() {
        return processProperties.get("process.args");
    }

    @Override
    public List<String> getProcessArgsAsList() {
        return toList(getProcessArgs());
    }

    @Override
    public String getMainClass() {
        return processProperties.get("java.main");
    }

    private List<String> toList(String param) {
        return isEmpty(param) ? emptyList() : asList(replaceMultipleSpaces(param).split(" "));
    }
}