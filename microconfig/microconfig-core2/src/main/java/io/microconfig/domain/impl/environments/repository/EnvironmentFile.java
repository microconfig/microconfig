package io.microconfig.domain.impl.environments.repository;

import io.microconfig.domain.Component;
import io.microconfig.domain.ComponentGroup;
import io.microconfig.domain.impl.environments.EnvironmentImpl;
import io.microconfig.io.formats.Io;
import lombok.RequiredArgsConstructor;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

import static io.microconfig.io.FileUtils.getName;
import static java.util.Collections.emptyList;
import static java.util.Optional.*;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class EnvironmentFile {
    private static final String IP = "ip";
    private static final String PORT_OFFSET = "portOffset";
    private static final String INCLUDE = "include";
    private static final String INCLUDE_ENV = "env";
    private static final String EXCLUDE = "exclude";
    private static final String APPEND = "append";
    private static final String COMPONENTS = "components";

    private final File file;

    public EnvironmentDefinition parseUsing(Io io) {
        try {
            return doParse(getName(file), io.readFully(file));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Can't parse '" + file + "' env", e);
        }
    }

    private EnvironmentDefinition doParse(String name, String content) {
        Map<String, Object> keyValue = new Yaml().load(content);

        Optional<EnvInclude> envInclude = parseInclude(keyValue);
        Optional<Integer> portOffset = parsePortOffset(keyValue);
        Optional<String> envIp = parseIp(keyValue);
        List<ComponentGroup> componentGroups = parseComponentGroups(keyValue, name, envIp);

        return new EnvironmentImpl(name, componentGroups, envIp, portOffset, envInclude, null);
    }

    @SuppressWarnings("unchecked")
    private Optional<EnvInclude> parseInclude(Map<String, Object> keyValue) {
        Map<String, Object> includeProps = (Map<String, Object>) keyValue.remove(INCLUDE);
        if (includeProps == null) return empty();

        String name = (String) includeProps.get(INCLUDE_ENV);
        Collection<String> excludes = (Collection<String>) includeProps.getOrDefault(EXCLUDE, emptyList());
        return of(new EnvInclude(name, new LinkedHashSet<>(excludes)));
    }

    private Optional<Integer> parsePortOffset(Map<String, ?> keyValue) {
        return ofNullable(keyValue.remove(PORT_OFFSET))
                .map(Number.class::cast)
                .map(Number::intValue);
    }

    private Optional<String> parseIp(Map<String, ?> keyValue) {
        return ofNullable(keyValue.remove(IP)).map(Object::toString);
    }

    private List<ComponentGroup> parseComponentGroups(Map<String, Object> keyValue, String envName, Optional<String> envIp) {
        return keyValue.entrySet()
                .stream()
                .map(group -> {
                    try {
                        return parseGroup(group, envIp);
                    } catch (RuntimeException e) {
                        throw new RuntimeException("Can't parse group declaration: '" + group + "' in '" + envName + "' env.", e);
                    }
                }).collect(toList());
    }

    @SuppressWarnings("unchecked")
    private ComponentGroup parseGroup(Entry<String, Object> componentGroupDeclaration, Optional<String> envIp) {
        String componentGroupName = componentGroupDeclaration.getKey();
        Map<String, Object> properties = (Map<String, Object>) componentGroupDeclaration.getValue();
        Optional<String> ip = ofNullable((String) properties.getOrDefault(IP, envIp.orElse(null)));

        List<Component> parsedComponents = parseComponents(properties, COMPONENTS);
        List<Component> excludedComponents = parseComponents(properties, EXCLUDE);
        List<Component> appendedComponents = parseComponents(properties, APPEND);

        return new ComponentGroup(componentGroupName, ip,
                parsedComponents, excludedComponents, appendedComponents
        );
    }

    @SuppressWarnings("unchecked")
    private List<Component> parseComponents(Map<String, Object> keyValue, String property) {
        List<String> values = (List<String>) keyValue.get(property);
        if (values == null) return emptyList();

        return values.stream()
                .filter(Objects::nonNull)
                .map(s -> {
                    String[] parts = s.split(":");
                    if (parts.length > 2) throw new IllegalArgumentException("Incorrect component declaration: " + s);
                    return parts.length == 1 ? Component.byType(parts[0]) : Component.byNameAndType(parts[0], parts[1]);
                }).collect(toList());
    }
}