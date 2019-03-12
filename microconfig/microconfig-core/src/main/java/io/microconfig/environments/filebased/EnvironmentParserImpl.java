package io.microconfig.environments.filebased;

import com.google.gson.Gson;
import io.microconfig.environments.Component;
import io.microconfig.environments.ComponentGroup;
import io.microconfig.environments.EnvInclude;
import io.microconfig.environments.Environment;
import lombok.RequiredArgsConstructor;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Optional.*;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class EnvironmentParserImpl implements EnvironmentParser {
    private static final String IP = "ip";
    private static final String PORT_OFFSET = "portOffset";
    private static final String INCLUDE = "include";
    private static final String INCLUDE_ENV = "env";
    private static final String EXCLUDE = "exclude";
    private static final String APPEND = "append";
    private static final String COMPONENTS = "components";

    private final Function<String, Map<String, Object>> parser;

    public static EnvironmentParser yamlParser() {
        return new EnvironmentParserImpl(new Yaml()::load);
    }

    @SuppressWarnings("unchecked")
    public static EnvironmentParser jsonParser() {
        Gson gson = new Gson();
        return new EnvironmentParserImpl(content -> gson.fromJson(content, Map.class));
    }

    @Override
    public Environment parse(String name, String content) {
        try {
            return doParse(name, content);
        } catch (RuntimeException e) {
            throw new RuntimeException("Can't parse '" + name + "' env", e);
        }
    }

    private Environment doParse(String name, String content) {
        Map<String, Object> map = parser.apply(content);

        Optional<EnvInclude> envInclude = parseInclude(map);
        Optional<Integer> portOffset = parsePortOffset(map);
        Optional<String> envIp = parseIp(map);
        List<ComponentGroup> componentGroups = parseComponentGroups(map, envIp, name);

        return new Environment(name, componentGroups, envIp, portOffset, envInclude);
    }

    @SuppressWarnings("unchecked")
    private Optional<EnvInclude> parseInclude(Map<String, Object> map) {
        Map<String, Object> includeProps = (Map<String, Object>) map.remove(INCLUDE);
        if (includeProps == null) return empty();

        String name = (String) includeProps.get(INCLUDE_ENV);
        Collection<String> excludes = (Collection<String>) includeProps.getOrDefault(EXCLUDE, emptyList());
        return of(new EnvInclude(name, new LinkedHashSet<>(excludes)));
    }

    private Optional<Integer> parsePortOffset(Map<String, ?> map) {
        return ofNullable(map.remove(PORT_OFFSET))
                .map(Number.class::cast)
                .map(Number::intValue);
    }

    private Optional<String> parseIp(Map<String, ?> map) {
        return ofNullable(map.remove(IP)).map(Object::toString);
    }

    private List<ComponentGroup> parseComponentGroups(Map<String, Object> map, Optional<String> envIp, String envName) {
        return map.entrySet()
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
    private List<Component> parseComponents(Map<String, Object> properties, String property) {
        List<String> values = (List<String>) properties.get(property);
        if (values == null) {
            return emptyList();
        }

        return values
                .stream()
                .filter(Objects::nonNull)
                .map(s -> {
                    String[] parts = s.split(":");
                    if (parts.length > 2) throw new IllegalArgumentException("Incorrect component declaration: " + s);
                    return parts.length == 1 ? Component.byType(parts[0]) : Component.byNameAndType(parts[0], parts[1]);
                }).collect(toList());
    }
}