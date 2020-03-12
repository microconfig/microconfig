package io.microconfig.domain.impl.properties;

import io.microconfig.domain.Property;
import io.microconfig.domain.Resolver;
import io.microconfig.domain.Resolver.Expression;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class PropertyImpl implements Property {
    private static final String TEMP_VALUE = "#var";

    private final String key;
    @With
    private final String value;
    private final boolean temp;

    public static Property parse(String keyValue, String envContext, PropertySource source) {
        return null;
    }

    public static Property property(String key, String value, String envContext, PropertySource source) {
        return null;
    }

    public static boolean containsYamlProperties(List<Property> properties) {
        return true;
    }

    public static boolean isComment(String line) {
        return line.startsWith("#");
    }

    @Override
    public Property resolveBy(Resolver resolver) {
        StringBuilder result = new StringBuilder(value);

        while (true) {
            Expression expression = resolver.parse(result);
            if (expression.getStartIndex() < 0) break;

            String resolved = expression.resolve();
            result.replace(expression.getStartIndex(), expression.getEndIndex() + 1, resolved);
        }

        return withValue(result.toString());
    }
}