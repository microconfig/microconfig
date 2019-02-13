package io.microconfig.properties.files.provider;

import org.junit.Test;

import java.io.File;

import static io.microconfig.properties.files.provider.PropertyFilter.newDefaultComponentFilter;
import static io.microconfig.properties.files.provider.PropertyFilter.newEnvComponentFilter;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class PropertyFilterTest {
    @Test
    public void test() {
        String extension = ".properties";
        File f1 = new File("service.properties");
        File f2 = new File("service.dev2.properties");
        File f3 = new File("service.dev2.test.properties");

        PropertyFilter defaultFilter = newDefaultComponentFilter(extension);
        assertTrue(defaultFilter.test(f1));
        assertFalse(defaultFilter.test(f2));
        assertFalse(defaultFilter.test(f3));

        File f4 = new File("service.prod.dev2.dev4.properties");
        File f5 = new File("service.prod.dev4.properties");
        PropertyFilter envFilter = newEnvComponentFilter("dev2", extension);
        assertFalse(envFilter.test(f1));
        assertTrue(envFilter.test(f2));
        assertTrue(envFilter.test(f3));
        assertTrue(envFilter.test(f4));
        assertFalse(envFilter.test(f5));
    }
}