package io.microconfig.utils;

import java.util.Map;

import static io.microconfig.utils.Logger.error;
import static io.microconfig.utils.StringUtils.isEmpty;
import static java.lang.System.*;

public class SystemPropertiesUtils {
    public static boolean hasSystemFlag(String name) {
        return hasTrueValue(name, getProperties());
    }

    public static boolean hasTrueValue(String property, Map<?, ?> prop) {
        return "true".equals(prop.get(property));
    }

    public static String getRequiredProperty(String propertyName) {
        String property = getProperty(propertyName);
        if (isEmpty(property) || "?".equals(property)) {
            error("Please specify -D" + propertyName + " param");
            exit(-1);
        }
        return property.trim();
    }
}