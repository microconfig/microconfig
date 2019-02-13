package io.microconfig.environments.filebased;

import com.google.gson.Gson;
import io.microconfig.environments.Component;
import io.microconfig.environments.ComponentGroup;
import io.microconfig.environments.EnvInclude;
import io.microconfig.environments.Environment;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Optional.*;
import static java.util.stream.Collectors.toList;

public class EnvironmentParserImpl implements EnvironmentParser<String> {
    private static final String IP = "ip";
    private static final String PORT_OFFSET = "portOffset";
    private static final String INCLUDE = "include";
    private static final String INCLUDE_ENV = "env";
    private static final String EXCLUDE = "exclude";
    private static final String APPEND = "append";
    private static final String COMPONENTS = "components";

    private final Gson gson = new Gson();

    @Override
    @SuppressWarnings("unchecked")
    public Environment parse(String name, String content) {
        Map<String, Map<String, Object>> map = gson.fromJson(content, Map.class);

        Optional<EnvInclude> envInclude = parseInclude(map);
        Optional<Integer> portOffset = parsePortOffset(map);
        Optional<String> envIp = ofNullable((Object) map.remove(IP)).map(Object::toString);

        List<ComponentGroup> componentGroups = map.entrySet().stream().map(componentGroupDeclaration -> {
            String componentGroupName = componentGroupDeclaration.getKey();
            Map<String, Object> properties = componentGroupDeclaration.getValue();
            Optional<String> ip = ofNullable((String) properties.getOrDefault(IP, envIp.orElse(null)));

            List<Component> parsedComponents = fetchComponentsFromProperties(properties, COMPONENTS);
            List<Component> excludedComponents = fetchComponentsFromProperties(properties, EXCLUDE);
            List<Component> appendedComponents = fetchComponentsFromProperties(properties, APPEND);

            return new ComponentGroup(componentGroupName, ip, parsedComponents, excludedComponents, appendedComponents);
        }).collect(toList());

        return new Environment(name, componentGroups, envIp, portOffset, envInclude);
    }

    @SuppressWarnings("unchecked")
    private List<Component> fetchComponentsFromProperties(Map<String, Object> properties, String property) {
        List<String> values = (List<String>) properties.get(property);
        return values == null ? emptyList() : parseComponents(values);
    }

    private List<Component> parseComponents(List<String> components) {
        return components.stream().filter(Objects::nonNull).map(s -> {
            String[] parts = s.split(":");
            if (parts.length > 2) throw new IllegalArgumentException("Incorrect component declaration: " + s);
            return parts.length == 1 ? Component.byType(parts[0]) : Component.byNameAndType(parts[0], parts[1]);
        }).collect(toList());
    }

    private Optional<Integer> parsePortOffset(Map<String, ?> map) {
        return ofNullable(map.remove(PORT_OFFSET))
                .map(Double.class::cast)
                .map(Double::intValue);
    }

    @SuppressWarnings("unchecked")
    private Optional<EnvInclude> parseInclude(Map<String, Map<String, Object>> map) {
        Map<String, Object> includeProps = map.remove(INCLUDE);
        if (includeProps == null) return empty();

        String name = (String) includeProps.get(INCLUDE_ENV);
        Collection<String> excludes = (Collection<String>) includeProps.getOrDefault(EXCLUDE, emptyList());
        return of(new EnvInclude(name, new LinkedHashSet<>(excludes)));
    }
}