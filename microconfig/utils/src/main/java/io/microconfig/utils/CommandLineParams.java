package io.microconfig.utils;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static io.microconfig.utils.Logger.error;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public class CommandLineParams {
    private final Map<String, String> keyToValue;

    public static CommandLineParams parse(String[] args) {
        Map<String, String> keyToValue = Stream.of(args)
                .map(a -> a.split("="))
                .peek(pair -> {
                    if (pair.length != 2) {
                        throw new IllegalStateException("Incorrect command line param " + Arrays.toString(pair));
                    }
                }).collect(toMap(p -> p[0], p -> p[1]));

        return new CommandLineParams(keyToValue);
    }

    public String value(String key) {
        return keyToValue.get(key);
    }

    public String requiredValue(String key, String npeMessage) {
        String value = value(key);
        if (value == null) {
            error(npeMessage);
            System.exit(-1);
        }
        return value;
    }
}