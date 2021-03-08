package io.microconfig;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.util.List;

import static io.microconfig.utils.FileUtils.getName;
import static io.microconfig.utils.IoUtils.readFully;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MicroconfigMainTest {
    @TempDir
    File destinationDir;

    final static List<String> components = asList("component","component2");

    private String root;

    @BeforeEach
    void setup(){
        root = getClass().getClassLoader().getResource("repo").getFile();
    }

    @Test
    void should_generate_config_for_single_env_when_e_param_given() {
        MicroconfigMain.main("-e", "env1", "-r", escape(root), "-d", escape(destinationDir.getAbsolutePath()));
        checkConfigGeneratedFor("component", null);
        checkConfigGeneratedFor("component2", null);
    }

    @Test
    void should_generate_config_for_multiple_envs_when_envs_param_given() {
        List<String> environments = asList("env2","env3");
        MicroconfigMain.main("-envs", String.join(",",environments), "-r", escape(root), "-d", escape(destinationDir.getAbsolutePath()));
        for (String component : components) {
            for(String environment : environments){
                checkConfigGeneratedFor(component, environment);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"env1, *", "*, env1","*"})
    void should_generate_config_for_all_envs_when_star_in_envs_list(String envs) {
        MicroconfigMain.main("-envs", envs, "-r", escape(root), "-d", escape(destinationDir.getAbsolutePath()));
        for (String component : components) {
            for(String environment: asList("env1","env2")){
                checkConfigGeneratedFor(component, environment);
            }
        }
    }

    @Test
    void should_not_generate_config_for_excluded_envs(){
        String envs = "env1, !env2, env3";
        MicroconfigMain.main("-envs", envs, "-r", escape(root), "-d", escape(destinationDir.getAbsolutePath()));
        for (String component : components) {
            for(String environment: asList("env1","env3")){
                checkConfigGeneratedFor(component, environment);
            }
            checkConfigNotGeneratedFor(component, "env2");
        }
    }

    private String escape(String name) {
        return "\"" + name + "\"";
    }

    private void checkConfigGeneratedFor(String component, String nestedDirectory){
        checkComponentBuildResult(component, nestedDirectory, true);
    }

    private void checkConfigNotGeneratedFor(String component, String nestedDirectory){
        checkComponentBuildResult(component, nestedDirectory, false);
    }

    private void checkComponentBuildResult(String component, String nestedDirectory, boolean shouldFileExist) {
        checkFileExist(buildFilePath(component, "application.yaml", nestedDirectory), shouldFileExist);
        checkFileExist(buildFilePath(component, "deploy.yaml", nestedDirectory), shouldFileExist);
    }

    private String buildFilePath(String component, String fileName, String nestedDirectory){
        String result = String.format("%s/%s/", component, fileName);
        if(nestedDirectory!=null){
            result = String.format("%s/%s", nestedDirectory, result);
        }
        return result;
    }

    private void checkFileExist(String filePath, boolean shouldFileExist) {
        File resultFile = new File(destinationDir, filePath);
        assertEquals(shouldFileExist, resultFile.exists());
        if(shouldFileExist) {
            assertEquals("key: " + getName(resultFile), readFully(resultFile).trim());
        }
    }

    @Ignore
    @Test
    void should_throw_error_if_env_and_envs_both_empty(){
        //TODO would be good to test that an exception occurrs when env and envs are both empty
    }
}