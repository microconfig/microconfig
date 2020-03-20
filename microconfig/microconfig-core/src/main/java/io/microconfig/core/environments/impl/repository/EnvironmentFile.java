package io.microconfig.core.environments.impl.repository;

import io.microconfig.io.FsReader;
import lombok.RequiredArgsConstructor;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

import static io.microconfig.utils.FileUtils.getName;
import static io.microconfig.utils.StreamUtils.forEach;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
class EnvironmentFile {
    private static final String IP = "ip";
    private static final String PORT_OFFSET = "portOffset";
    private static final String INCLUDE = "include";
    private static final String INCLUDE_ENV = "env";
    private static final String EXCLUDE = "exclude";
    private static final String APPEND = "append";
    private static final String COMPONENTS = "components";

    private final File file;

    public EnvironmentDefinition parseUsing(FsReader fsReader) {
        try {
            return parse(fsReader.readFully(file), getName(file));
        } catch (RuntimeException e) {
            throw new EnvironmentException("Can't parse env file '" + file + "'", e);
        }
    }

    private EnvironmentDefinition parse(String content, String name) {
        Map<String, Object> keyValue = new Yaml().load(content);

        EnvInclude envInclude = parseInclude(keyValue);
        int portOffset = parsePortOffset(keyValue);
        String envIp = parseIp(keyValue);
        List<ComponentGroupDefinition> componentGroups = parseComponentGroups(keyValue, name, envIp);

        return new EnvironmentDefinition(name, envIp, portOffset, envInclude, componentGroups, file);
    }

    @SuppressWarnings("unchecked")
    private EnvInclude parseInclude(Map<String, Object> keyValue) {
        Map<String, Object> includeProps = (Map<String, Object>) keyValue.remove(INCLUDE);
        if (includeProps == null) return EnvInclude.empty();

        String name = (String) includeProps.get(INCLUDE_ENV);
        Collection<String> excludes = (Collection<String>) includeProps.getOrDefault(EXCLUDE, emptyList());
        return new EnvInclude(name, new LinkedHashSet<>(excludes));
    }

    private int parsePortOffset(Map<String, ?> keyValue) {
        Number offset = (Number) keyValue.remove(PORT_OFFSET);
        return offset == null ? 0 : offset.intValue();
    }

    private String parseIp(Map<String, ?> keyValue) {
        return (String) keyValue.remove(IP);
    }

    private List<ComponentGroupDefinition> parseComponentGroups(Map<String, Object> keyValue, String envName, String envIp) {
        return forEach(keyValue.entrySet(), groupEntry -> {
            try {
                return parseGroup(groupEntry, envIp);
            } catch (RuntimeException e) {
                throw new IllegalStateException("Can't parse group declaration: '" + groupEntry + "' in '" + envName + "' env.", e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private ComponentGroupDefinition parseGroup(Entry<String, Object> componentGroupDeclaration, String envIp) {
        String componentGroupName = componentGroupDeclaration.getKey();
        Map<String, Object> properties = (Map<String, Object>) componentGroupDeclaration.getValue();
        String ip = (String) properties.getOrDefault(IP, envIp);

        List<ComponentDefinition> parsedComponents = parseComponents(properties, COMPONENTS);
        List<ComponentDefinition> excludedComponents = parseComponents(properties, EXCLUDE);
        List<ComponentDefinition> appendedComponents = parseComponents(properties, APPEND);

        return new ComponentGroupDefinition(componentGroupName, ip, parsedComponents, excludedComponents, appendedComponents);
    }

    @SuppressWarnings("unchecked")
    private List<ComponentDefinition> parseComponents(Map<String, Object> keyValue, String property) {
        List<String> values = (List<String>) keyValue.get(property);
        if (values == null) return emptyList();

        return values.stream()
                .filter(Objects::nonNull)
                .map(s -> {
                    String[] parts = s.split(":");
                    if (parts.length > 2) throw new IllegalArgumentException("Incorrect component declaration: " + s);
                    return parts.length == 1 ? ComponentDefinition.withName(parts[0]) : ComponentDefinition.withAlias(parts[0], parts[1]);
                }).collect(toList());
    }
}