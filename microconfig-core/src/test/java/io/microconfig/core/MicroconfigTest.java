package io.microconfig.core;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static io.microconfig.core.ClasspathReader.classpathFile;
import static io.microconfig.core.Microconfig.searchConfigsIn;
import static io.microconfig.core.configtypes.ConfigTypeFilters.configType;
import static io.microconfig.core.configtypes.StandardConfigType.APPLICATION;
import static io.microconfig.core.properties.serializers.PropertySerializers.asString;
import static io.microconfig.utils.FileUtils.walk;
import static io.microconfig.utils.IoUtils.readFully;
import static io.microconfig.utils.StringUtils.unixLikePath;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class MicroconfigTest {
    File root = classpathFile("repo");
    Microconfig microconfig = searchConfigsIn(root);

    @TestFactory
    List<DynamicTest> findTests() {
        try (Stream<Path> stream = walk(classpathFile("repo").toPath())) {
            return stream.map(Path::toFile)
                    .filter(this::isExpectation)
                    .map(this::toTest)
                    .collect(toList());
        }
    }

    @Test
    void testIncorrectRootDir() {
        assertThrows(IllegalArgumentException.class, () -> searchConfigsIn(new File("missingDir")));
    }

    private boolean isExpectation(File file) {
        return //file.getParentFile().getName().equals("ic1") &&
                file.getName().startsWith("expect.") ||
                        file.getName().startsWith("exception.");
    }

    private DynamicTest toTest(File expectation) {
        String component = getComponentName(expectation);
        String env = getEnvName(expectation);
        return toTest(expectation, component, env);
    }

    private DynamicTest toTest(File expectation, String component, String env) {
        return dynamicTest(component + "[" + env + "]", () ->
                assertEquals(
                        readExpectation(expectation).trim(),
                        build(component, env).trim()
                ));
    }

    private String getComponentName(File expectation) {
        String[] parts = expectation.getName().split("\\.");
        return parts.length == 3 ? parts[2] : expectation.getParentFile().getName();
    }

    private String getEnvName(File expectation) {
        return expectation.getName().split("\\.")[1];
    }

    private String build(String component, String env) {
        try {
            return microconfig.environments().getOrCreateByName(env)
                    .getOrCreateComponentWithName(component)
                    .getPropertiesFor(configType(APPLICATION))
                    .resolveBy(microconfig.resolver())
                    .first()
                    .save(asString());
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    private String readExpectation(File expectation) {
        return readFully(expectation)
                .replace("${configDir}", expectation.getParentFile().getAbsolutePath())
                .replace("${configRoot}", toPath(root))
                .replace("${resultDir}", toPath(new File(root, "build")))
                .replace("${userHome}", toPath(new File(System.getProperty("user.home"))))
                .replace("${user}", System.getenv("USER"))
                .replace("${space}", " ")
                .replace("#todo", "");
    }

    private String toPath(File file) {
        return unixLikePath(file.getAbsolutePath());
    }
}