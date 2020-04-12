package io.microconfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static io.microconfig.utils.FileUtils.getName;
import static io.microconfig.utils.IoUtils.readFully;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicroconfigMainTest {
    @TempDir
    File destinationDir;

    @Test
    void main() {
        String root = getClass().getClassLoader().getResource("repo").getFile();
        MicroconfigMain.main("-e", "test", "-r", escape(root), "-d", escape(destinationDir.getAbsolutePath()));

        checkComponentBuildResult("component");
        checkComponentBuildResult("component2");
    }

    private String escape(String name) {
        return "\"" + name + "\"";
    }

    private void checkComponentBuildResult(String component) {
        checkFileExist(component, "application.yaml");
        checkFileExist(component, "deploy.yaml");
    }

    private void checkFileExist(String name, final String fileName) {
        File resultFile = new File(destinationDir, name + "/" + fileName);
        assertTrue(resultFile.exists());
        assertEquals("key: " + getName(resultFile) + "\n", readFully(resultFile));
    }
}