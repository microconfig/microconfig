package io.microconfig.core;

import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

import static io.microconfig.utils.IoUtils.readFully;

public class ClasspathReader {
    public static File classpathFile(String name) {
        try {
            return new ClassPathResource(name).getFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String read(String file) {
        return readFully(classpathFile(file));
    }
}