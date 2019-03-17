package io.microconfig.utils;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.microconfig.utils.Logger.error;
import static io.microconfig.utils.StringUtils.isEmpty;
import static io.microconfig.utils.StringUtils.splitToList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.of;

@RequiredArgsConstructor
public class CommandLineParams {
    private final Map<String, String> keyToValue;

    public static CommandLineParams parse(String[] args) {
        Map<String, String> keyToValue = of(args)
                .map(a -> a.split("="))
                .peek(pair -> {
                    if (pair.length != 2) {
                        error("Incorrect command line param " + Arrays.toString(pair));
                        System.exit(-1);
                    }
                }).collect(toMap(p -> p[0], p -> p[1]));

        return new CommandLineParams(keyToValue);
    }

    public String value(String key) {
        String v = keyToValue.get(key);
        return isEmpty(v) ? null : v;
    }

    public List<String> listValue(String key) {
        return splitToList(value(key), ",");
    }

    public String requiredValue(String key, String npeMessage) {
        String value = value(key);
        if (value == null) {
            error(npeMessage);
            System.exit(-1);
        }
        return value.startsWith("\"")
                && value.endsWith("\"") ? value.substring(1, value.length() - 1) : value;
    }

    public void putToSystem(String key) {
        String value = value(key);
        if (value != null) {
            System.setProperty(key, value);
        }
    }
}