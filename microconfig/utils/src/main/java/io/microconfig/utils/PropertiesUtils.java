package io.microconfig.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Stream;

import static io.microconfig.utils.FileUtils.createFile;
import static io.microconfig.utils.IoUtils.lines;
import static io.microconfig.utils.Logger.error;
import static io.microconfig.utils.StreamUtils.toLinkedMap;
import static java.lang.System.*;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class PropertiesUtils {
    public static Map<String, String> readProperties(File file) {
        if (!file.exists()) return emptyMap();

        try (Stream<String> lines = lines(file.toPath())) {
            return lines
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(s -> !s.startsWith("#"))
                    .map(s -> {
                        int i = s.indexOf('=');
                        if (i < 0) {
                            throw new IllegalArgumentException("Property must contain '='. Bad property: " + s + " in " + file);
                        }
                        return new String[]{s.substring(0, i).trim(), s.substring(i + 1)};
                    })
                    .collect(toLinkedMap(s -> s[0], s -> s[1]));
        }
    }

    public static void writeProperties(File file, Map<String, String> keyToValue) {
        String content = keyToValue.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(joining("\n"));

        FileUtils.write(file, content);
    }

    public static Properties loadProperties(File file) {
        Properties properties = new Properties();
        if (!file.exists()) return properties;

        try (InputStream stream = new FileInputStream(file)) {
            properties.load(stream);
        } catch (Exception e) {
            throw new RuntimeException("Can't load source properties: " + file, e);
        }
        return properties;
    }

    public static boolean hasSystemFlag(String name) {
        return hasTrueValue(name, getProperties());
    }

    public static boolean hasTrueValue(String property, Map<?, ?> prop) {
        return "true".equals(prop.get(property));
    }

    /*
      this method read http\:// like http://
      readProperties method read http\:// like http\://
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> loadPropertiesAsMap(File file) {
        return new TreeMap(loadProperties(file));
    }

    public static String getRequiredProperty(String propertyName) {
        String property = getProperty(propertyName);
        if (property == null || property.isEmpty() || "?".equals(property)) {
            error("Please specify -D" + propertyName + " param");
            exit(-1);
        }
        return property.trim();
    }

    public static void append(File file, Map<String, String> properties) {
        List<String> lines = properties.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(toList());
        lines.add(0, "\n");

        try {
            createFile(file);
            Files.write(file.toPath(), lines, APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}