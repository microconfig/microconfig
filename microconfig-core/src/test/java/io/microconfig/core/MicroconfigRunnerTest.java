package io.microconfig.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static io.microconfig.core.ClasspathReader.classpathFile;
import static io.microconfig.utils.IoUtils.readFully;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicroconfigRunnerTest {
    @TempDir
    File destinationDir;

    @Test
    void testBuild() {
        File root = classpathFile("repo");
        new MicroconfigRunner(root, destinationDir).build("var", emptyList(), emptyList());

        //todo test diff configTypes
        File resultFile = new File(destinationDir, "var/application.properties");
        assertTrue(resultFile.exists());
        assertEquals("c=3", readFully(resultFile));
    }
}