package deployment.mgmt.configs.service.properties;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

public interface ProcessProperties {
    JavaAppSettings getJavaAppSettings();

    NotJavaAppSettings getNotJavaAppSettings();

    MavenSettings getMavenSettings();

    HealthCheckSettings getHealthCheckSettings();

    boolean isJavaApp();

    boolean isWebapp();

    boolean writePid();

    String getPrepareDirScriptName();

    String getPrestartScriptName();

    String getVersion();

    String getConfigVersion();

    Integer getStopOrder();

    int getStartWaitSec();

    boolean allowParallelStart();

    int getStartGroup();

    String getLogFileName(String service);

    Set<String> getGroups();

    void changeVersion(String version);

    boolean isTask();

    boolean isPatcher();

    boolean isService();

    Map<String, String> asMap();

    String get(String key);

    String getOrDefault(String key, String defaultValue);

    int getOrDefault(String key, int defaultValue);

    boolean hasTrueValue(String property);

    Stream<Entry<String, String>> findByPrefix(String prefix);

    void update(String key, String value);

    void update(Map<String, String> keyToValue);
}