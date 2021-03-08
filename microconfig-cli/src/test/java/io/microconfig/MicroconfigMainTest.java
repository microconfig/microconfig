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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        checkComponentBuildResult("component", null);
        checkComponentBuildResult("component2", null);
    }

    @Test
    void should_generate_config_for_multiple_envs_when_envs_param_given() {
        List<String> environments = asList("env2","env3");
        MicroconfigMain.main("-envs", String.join(",",environments), "-r", escape(root), "-d", escape(destinationDir.getAbsolutePath()));
        for (String component : components) {
            for(String environment : environments){
                checkComponentBuildResult(component, environment);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"env1, *", "*, env1","*"})
    void should_generate_config_for_all_envs_when_star_in_envs_list(String envs) {
        MicroconfigMain.main("-envs", envs, "-r", escape(root), "-d", escape(destinationDir.getAbsolutePath()));
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
        String targetPath;
        if(env!=null){
            targetPath =String.format("%s/%s/%s", env, name, fileName);
        } else {
            targetPath = String.format("%s/%s", name, fileName);
        }
        File resultFile = new File(destinationDir, targetPath);
        assertTrue(resultFile.exists());
        assertEquals("key: " + getName(resultFile), readFully(resultFile).trim());
    }

    @Ignore
    @Test
    void should_throw_error_if_env_and_envs_both_empty(){
        //TODO would be good to test that an exception occurrs when env and envs are both empty
    }
}