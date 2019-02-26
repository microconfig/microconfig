package io.microconfig.utils;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.microconfig.utils.Logger.error;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
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
        return keyToValue.get(key);
    }

    public List<String> listValue(String key) {
        String value = value(key);
        return value == null ? emptyList()
                : of(value.split(","))
                .map(String::trim)
                .collect(toList());
    }

    public String requiredValue(String key, String npeMessage) {
        String value = value(key);
        if (value == null) {
            error(npeMessage);
            System.exit(-1);
        }
        return value;
    }

    public void putToSystem(String key) {
        String value = value(key);
        if (value != null) {
            System.setProperty(key, value);
        }
    }
}