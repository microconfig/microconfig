package io.microconfig.core.configtypes;

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
    private final String name;
    private final Set<String> sourceExtensions;
    private final String resultFileName;
    private final String resultFileExtension;

    public static ConfigType byName(String name) {
        return byNameAndExtensions(name, singleton('.' + name), name);
    }

    public static ConfigType byNameAndExtensions(String name, Set<String> sourceExtensions, String resultFileName) {
        return byNameAndExtensionsAndResultFileExtension(name, sourceExtensions, resultFileName, null);
    }

    public static ConfigType byNameAndExtensionsAndResultFileExtension(String name, Set<String> sourceExtensions, String resultFileName, String resultFileExtension) {
        List<String> badExtensions = filter(sourceExtensions, ext -> !ext.startsWith("."));
        if (!badExtensions.isEmpty()) {
            throw new IllegalArgumentException("Source file extensions must start with '.'. Current: " + badExtensions);
        }

        if (resultFileExtension != null && !resultFileExtension.startsWith(".")) {
            throw new IllegalArgumentException("resultFileExtension file extension must start with '.'. Current: " + resultFileExtension);
        }

        return new ConfigTypeImpl(name, sourceExtensions, resultFileName, resultFileExtension);
    }
}