package io.microconfig.core;

import io.microconfig.core.environments.repository.EnvironmentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static io.microconfig.core.ClasspathReader.classpathFile;
import static io.microconfig.utils.IoUtils.readFully;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicroconfigRunnerTest {
    @TempDir
    File destinationDir;

    @Test
    void testBuild() {
        File root = classpathFile("repo");
        new MicroconfigRunner(root, destinationDir).build("var", emptyList(), emptyList());

        File resultFile = new File(destinationDir, "var/service.properties");
        assertTrue(resultFile.exists());
        assertEquals("c=3", readFully(resultFile));
    }

    @Test
    void testBuildAbstract() {
        File root = classpathFile("repo");
        assertThrows(EnvironmentException.class,
                () -> new MicroconfigRunner(root, destinationDir).build("abstract", emptyList(), emptyList()));
    }
}