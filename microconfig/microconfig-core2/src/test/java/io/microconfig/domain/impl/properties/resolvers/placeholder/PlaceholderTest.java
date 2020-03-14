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
                .findStatementIn("#{1+2}")
                .orElseThrow(IllegalStateException::new)
                .resolve();

        System.out.println(resolved);
    }

    private File rootDir() {
        return classpathFile("test-props");
    }
}