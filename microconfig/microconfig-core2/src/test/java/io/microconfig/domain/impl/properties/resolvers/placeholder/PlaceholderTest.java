package io.microconfig.domain.impl.properties.resolvers.placeholder;

import org.junit.jupiter.api.Test;

import java.io.File;

import static io.microconfig.ClasspathUtils.classpathFile;
import static io.microconfig.MicroconfigFactory.searchConfigsIn;

class PlaceholderTest {
    @Test
    void resolve() {
        String resolved = searchConfigsIn(rootDir())
                .getResolver()
                .resolve("#{1+2} #{2+4}");
        System.out.println(resolved);
    }

    private File rootDir() {
        return classpathFile("test-props");
    }
}