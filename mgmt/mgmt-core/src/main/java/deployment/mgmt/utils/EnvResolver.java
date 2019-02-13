package deployment.mgmt.utils;

import java.util.Map;
import java.util.function.UnaryOperator;

import static java.io.File.pathSeparator;
import static java.util.stream.Collectors.toUnmodifiableMap;

public class EnvResolver {
    public static Map<String, String> resolveEnvVariable(Map<String, String> nameToValue) {
        UnaryOperator<String> replaceOsSeparator = v -> v.replaceAll("[:;]", pathSeparator);

        UnaryOperator<String> resolvePlaceholders = v -> {
            int variableStartIndex = v.indexOf('$');
            if (variableStartIndex < 0) return v;
            int variableEndIndex = v.indexOf(pathSeparator, variableStartIndex);
            if (variableEndIndex < 0) {
                variableEndIndex = v.length();
            }

            String variableName = v.substring(variableStartIndex, variableEndIndex);
            String variableValue = System.getenv(variableName.substring(1));
            if (variableValue == null) {
                variableValue = "";
            }

            return v.replace(variableName, variableValue);
        };

        return nameToValue
                .entrySet().stream()
                .collect(toUnmodifiableMap(
                        Map.Entry::getKey,
                        e -> replaceOsSeparator.andThen(resolvePlaceholders).apply(e.getValue())
                ));
    }
}
