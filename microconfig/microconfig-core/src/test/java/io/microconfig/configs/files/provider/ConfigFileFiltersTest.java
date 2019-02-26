package io.microconfig.configs.files.provider;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.function.Predicate;

import static io.microconfig.configs.files.provider.ConfigFileFilters.*;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigFileFiltersTest {
    @Test
    void test() {
        String extension = ".properties";
        File f1 = new File("service.properties");
        File f2 = new File("service.dev2.properties");
        File f3 = new File("service.dev2.test.properties");
        File f4 = new File("service.prod.dev2.dev4.properties");
        File f5 = new File("service.prod.dev4.properties");

        Predicate<File> defaultFilter = defaultComponentFilter(singleton(extension));
        assertTrue(defaultFilter.test(f1));
        assertFalse(defaultFilter.test(f2));
        assertFalse(defaultFilter.test(f3));
        assertFalse(defaultFilter.test(f4));
        assertFalse(defaultFilter.test(f5));

        Predicate<File> envFilter = envSpecificFilter(singleton(extension), "dev2");
        assertFalse(envFilter.test(f1));
        assertTrue(envFilter.test(f2));
        assertFalse(envFilter.test(f3));
        assertFalse(envFilter.test(f4));
        assertFalse(envFilter.test(f5));

        Predicate<File> envSharedFilter = envSharedFilter(singleton(extension), "dev2");
        assertFalse(envSharedFilter.test(f1));
        assertFalse(envSharedFilter.test(f2));
        assertTrue(envSharedFilter.test(f3));
        assertTrue(envSharedFilter.test(f4));
        assertFalse(envSharedFilter.test(f5));
    }
}