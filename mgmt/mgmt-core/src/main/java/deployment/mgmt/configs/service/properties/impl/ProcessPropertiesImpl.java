package deployment.mgmt.configs.service.properties.impl;

import deployment.mgmt.configs.service.properties.*;
import io.microconfig.properties.io.ConfigIo;
import io.microconfig.utils.SystemPropertiesUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static deployment.mgmt.configs.service.properties.impl.StandardServiceGroup.*;
import static io.microconfig.utils.StringUtils.isEmpty;
import static io.microconfig.utils.StringUtils.replaceMultipleSpaces;
import static java.util.Collections.*;
import static java.util.Optional.ofNullable;
import static java.util.Set.of;

//"mgmt.prestop.script", "mgmt.poststop.script", "mgmt.poststart.script" todo2
@RequiredArgsConstructor
public class ProcessPropertiesImpl implements ProcessProperties {
    private final Map<String, String> keyToValue;
    private final File file;
    private final ConfigIo configIo;

    public static ProcessProperties fromFile(File file, ConfigIo configIo) {
        Map<String, String> keyToValue = configIo.read(file);
        return new ProcessPropertiesImpl(keyToValue, file, configIo);
    }

    public static ProcessProperties fromMap(Map<String, String> properties) {
        return new ProcessPropertiesImpl(properties, null, null);
    }

    public static ProcessProperties emptyProperties() {
        return fromMap(emptyMap());
    }

    @Override
    public MavenSettings getMavenSettings() {
        return new MavenSettingsImpl(this);
    }

    @Override
    public JavaAppSettings getJavaAppSettings() {
        return new JavaAppSettingsImpl(this);
    }

    @Override
    public NotJavaAppSettings getNotJavaAppSettings() {
        return new NotJavaAppSettingsImpl(this);
    }

    @Override
    public HealthCheckSettings getHealthCheckSettings() {
        return new HealthCheckSettingsImpl(this);
    }

    @Override
    public boolean isJavaApp() {
        return hasTrueValue("java.enabled");
    }

    @Override
    public boolean writePid() {
        return !hasTrueValue("mgmt.suppress.pid");
    }

    @Override
    public String getPrepareDirScriptName() {
        return get("mgmt.prepare.dir.script");
    }

    @Override
    public String getPrestartScriptName() {
        return get("mgmt.prestart.script");
    }

    @Override
    public String getVersion() {
        return getMavenSettings().getArtifact().getVersion();
    }

    @Override
    public String getConfigVersion() {
        return get("config.version");
    }

    @Override
    public void changeVersion(String version) {
        getMavenSettings().changeArtifactVersion(version);
    }

    @Override
    public Integer getStopOrder() {
        return getIntegerValue("stop.order");
    }

    @Override
    public boolean allowParallelStart() {
        return hasTrueValue("mgmt.start.parallel");
    }

    @Override
    public int getStartGroup() {
        return getOrDefault("mgmt.start.group", 0);
    }

    @Override
    public int getStartWaitSec() {
        return getOrDefault("mgmt.start.waitSec", 10);
    }

    @Override
    public String getLogFileName(String service) {
        return getOrDefault("mgmt.log.file-name", service + ".log");
    }

    @Override
    public Set<String> getGroups() {
        String groups = get("mgmt.group");
        Set<String> result = new LinkedHashSet<>(isEmpty(groups) ? emptySet() : of(replaceMultipleSpaces(groups).split(",")));
        if (isPatcher()) {
            result.add(PATCHERS.groupName());
        } else if (isTask()) {
            result.add(TASKS.groupName());
        } else {
            result.add(SERVICES.groupName());
        }
        return result;
    }

    @Override
    public boolean isTask() {
        return hasTrueValue("mgmt.task") || hasTrueValue("process.run.on.demand");
    }

    @Override
    public boolean isPatcher() {
        return hasTrueValue("process.script");
    }

    @Override
    public boolean isService() {
        return !isTask() && !isPatcher();
    }

    @Override
    public boolean isWebapp() {
        return hasTrueValue("mgmt.tomcat.webapp.enabled");
    }

    @Override
    public Map<String, String> asMap() {
        return unmodifiableMap(keyToValue);
    }

    @Override
    public String get(String key) {
        return keyToValue.get(key);
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        return keyToValue.getOrDefault(key, defaultValue);
    }

    @Override
    public int getOrDefault(String key, int defaultValue) {
        return ofNullable(get(key))
                .map(Integer::valueOf)
                .orElse(defaultValue);
    }

    @Override
    public Stream<Map.Entry<String, String>> findByPrefix(String prefix) {
        return keyToValue.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix));
    }

    @Override
    public void update(String key, String value) {
        update(Map.of(key, value));
    }

    @Override
    public boolean hasTrueValue(String property) {
        return SystemPropertiesUtils.hasTrueValue(property, keyToValue);
    }

    @Override
    public void update(Map<String, String> update) {
        keyToValue.putAll(update);
        configIo.write(file, keyToValue);
    }

    private Integer getIntegerValue(String name) {
        return ofNullable(get(name))
                .map(Integer::valueOf)
                .orElse(null);
    }
}