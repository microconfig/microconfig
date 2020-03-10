package io.microconfig.factory.configtype;

import io.microconfig.domain.ConfigType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Set;

import static io.microconfig.utils.StreamUtils.filter;
import static java.util.Collections.singleton;

@Getter
@ToString
@RequiredArgsConstructor
public class ConfigTypeImpl implements ConfigType {
    private final String type;
    private final Set<String> sourceExtensions;
    private final String resultFileName;

    public static ConfigType byName(String name) {
        return byNameAndExtensions(name, singleton('.' + name), name);
    }

    public static ConfigType byNameAndExtensions(String name, Set<String> sourceExtensions, String resultFileName) {
        List<String> badExtensions = filter(sourceExtensions, ext -> !ext.startsWith("."));
        if (!badExtensions.isEmpty()) {
            throw new IllegalArgumentException("File extension must start with '.'. Current: " + badExtensions);

        }
        return new ConfigTypeImpl(name, sourceExtensions, resultFileName);
    }
}