package io.microconfig.utils;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.microconfig.utils.Logger.error;
import static io.microconfig.utils.StringUtils.isEmpty;
import static io.microconfig.utils.StringUtils.splitToList;

@RequiredArgsConstructor
public class CommandLineParamParser {
    private final Map<String, String> keyToValue;

    public static CommandLineParamParser parse(String... args) {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) {
            String key = args[i];
            String value = args[i + 1];
            if (!key.startsWith("-")) {
                printErrorAndExit("key '" + key + "' must start with -");
            }

            params.put(key.substring(1), value);
        }
        return new CommandLineParamParser(params);
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
        if (value != null) {
            return value.startsWith("\"") && value.endsWith("\"") ?
                    value.substring(1, value.length() - 1) : value;
        }

        printErrorAndExit(npeMessage);
        throw new AssertionError("Impossible");
    }

    private static void checkKeyAndValue(String[] pair) {
        if (pair.length != 2) {
            printErrorAndExit("Incorrect command line param " + Arrays.toString(pair));
        }
    }

    private static void printErrorAndExit(String npeMessage) {
        error(npeMessage);
        System.exit(-1);
    }
}