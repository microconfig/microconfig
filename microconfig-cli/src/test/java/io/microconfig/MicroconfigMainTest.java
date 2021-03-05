package io.microconfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static io.microconfig.utils.FileUtils.getName;
import static io.microconfig.utils.IoUtils.readFully;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicroconfigMainTest {
    @TempDir
    File destinationDir;

    final static List<String> components = asList("component","component2");

    private static Stream<Arguments> provideEnvironments(){
        return Stream.of(
                Arguments.of(Collections.singletonList("env1")),
                Arguments.of(Collections.singletonList("env2")),
                Arguments.of(asList("env1","env2"))
        );
    }

    @ParameterizedTest()
    @MethodSource("provideEnvironments")
    void main(List<String> environments) {
        String root = getClass().getClassLoader().getResource("repo").getFile();
        MicroconfigMain.main("-e", String.join(",",environments), "-r", escape(root), "-d", escape(destinationDir.getAbsolutePath()));

        for (String component : components) {
            for(String environment : environments){
                checkComponentBuildResult(component, environment);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"env1, *", "*, env2", "*"})
    void config_for_all_envs_generated_when_star_in_env_list(String envs) {
        String root = getClass().getClassLoader().getResource("repo").getFile();
        MicroconfigMain.main("-e", envs, "-r", escape(root), "-d", escape(destinationDir.getAbsolutePath()));
        for (String component : components) {
            for(String environment: asList("env1","env2")){
                checkComponentBuildResult(component, environment);
            }
        }
    }

    private String escape(String name) {
        return "\"" + name + "\"";
    }

    private void checkComponentBuildResult(String component, String env) {
        checkFileExist(component, "application.yaml", env);
        checkFileExist(component, "deploy.yaml", env);
    }

    private void checkFileExist(String name, final String fileName, final String env) {
        File resultFile = new File(destinationDir, String.format("%s/%s/%s", env, name, fileName));
        assertTrue(resultFile.exists());
        assertEquals("key: " + getName(resultFile), readFully(resultFile).trim());
    }
}