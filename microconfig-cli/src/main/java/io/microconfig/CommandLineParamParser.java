package io.microconfig;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.microconfig.utils.Logger.error;
import static io.microconfig.utils.StringUtils.isEmpty;
import static io.microconfig.utils.StringUtils.split;

@RequiredArgsConstructor
public class CommandLineParamParser {
    private final Map<String, String> keyToValue;

    public static CommandLineParamParser parse(String... args) {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            String key = args[i];
            String value = i < args.length - 1 ? args[i + 1] : null;
            if (!key.startsWith("-")) {
                printErrorAndExit("key '" + key + "' must start with -");
            }

            params.put(key.substring(1), value);
        }
        return new CommandLineParamParser(params);
    }

    public String value(String key) {
        return valueOr(key, null);
    }

    public String valueOr(String key, String defaultValue) {
        String value = keyToValue.get(key);
        if (isEmpty(value)) return defaultValue;

        return value.startsWith("\"") && value.endsWith("\"") ?
                value.substring(1, value.length() - 1) : value;
    }

    public List<String> listValue(String key) {
        return split(value(key), ",");
    }

    public String requiredValue(String key, String npeMessage) {
        String value = value(key);
        if (value != null) return value;

        printErrorAndExit(npeMessage);
        throw new AssertionError("Impossible");
    }

    private static void printErrorAndExit(String npeMessage) {
        error(npeMessage);
        System.exit(-1);
    }

    public boolean booleanValue(String key) {
        return "true".equals(value(key));
    }

    public boolean contains(String key) {
        return keyToValue.containsKey(key);
    }
}