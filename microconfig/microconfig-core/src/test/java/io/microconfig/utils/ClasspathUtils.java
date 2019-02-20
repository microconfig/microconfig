package io.microconfig.utils;

import java.io.File;
import java.net.URL;

import static java.util.Objects.requireNonNull;

public class ClasspathUtils {
    public static File getClasspathFile(String name) {
        URL url = requireNonNull(ClasspathUtils.class.getClassLoader().getResource(name), () -> "File doesnt exists: " + name);
        return new File(url.getFile());
    }
}
