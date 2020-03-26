package io.microconfig;

import io.microconfig.core.properties.impl.PropertyResolveException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static io.microconfig.ClasspathUtils.classpathFile;
import static io.microconfig.Microconfig.searchConfigsIn;
import static io.microconfig.core.configtypes.impl.ConfigTypeFilters.configType;
import static io.microconfig.core.configtypes.impl.StandardConfigType.APPLICATION;
import static io.microconfig.core.properties.impl.PropertySerializers.asString;
import static io.microconfig.utils.ConsoleColor.red;
import static io.microconfig.utils.FileUtils.walk;
import static io.microconfig.utils.IoUtils.readFully;
import static io.microconfig.utils.Logger.*;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.partitioningBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MicroconfigTest {
    private final File root = classpathFile("repo");
    private final Microconfig microconfig = searchConfigsIn(root);

    public static void main(String[] args) {
        new MicroconfigTest()
                .findAndExecute("mergeList");
    }

    @Test
    void findAndExecuteAll() {
        findAndExecute(null);
    }

    @Test
    void testCyclicDetect() {
        assertThrows(PropertyResolveException.class, () -> build("cyclicDetect", "uat"));
    }

    private void findAndExecute(String component) {
        try (Stream<Path> stream = walk(classpathFile("repo").toPath())) {
            Map<Boolean, Long> resultToCount = stream.map(Path::toFile)
                    .filter(this::isExpectation)
                    .filter(f -> component == null || f.getName().equals(component))
                    .map(this::execute)
                    .collect(partitioningBy(r -> r, counting()));
            info("\n\nSucceed: " + resultToCount.get(true) + ", Failed: " + resultToCount.get(false));

            if (resultToCount.get(false) != 0) {
                throw new AssertionError();
            }
        }
    }

    private boolean isExpectation(File file) {
        return file.getName().startsWith("expect.");
    }

    //todo highlight error
    private boolean execute(File expectation) {
        String component = getComponentName(expectation);
        String env = getEnvName(expectation);
        String a = doBuild(component, env).trim();
        String e = readExpectation(expectation).trim();

        boolean result = e.equals(a);
        if (result) {
            announce("Succeed '" + component + "'");
        } else {
            info(red("Failed '" + component + "'. Expected/Actual:")
                    + "\n" + e
                    + "\n***\n" + a
            );
        }
        return result;
    }

    private String getComponentName(File expectation) {
        String[] parts = expectation.getName().split("\\.");
        return parts.length == 3 ? parts[2] : expectation.getParentFile().getName();
    }

    private String getEnvName(File expectation) {
        return expectation.getName().split("\\.")[1];
    }

    private String doBuild(String component, String env) {
        try {
            return build(component, env);
        } catch (RuntimeException e) {
            error("Failed '" + component + ":[" + env + "]'");
            throw e;
        }
    }

    private String build(String component, String env) {
        return microconfig.environments()
                .getOrCreateByName(env)
                .getOrCreateComponentWithName(component)
                .getPropertiesFor(configType(APPLICATION))
                .resolveBy(microconfig.resolver())
                .first()
                .save(asString());
    }

    private String readExpectation(File expectation) {
        return readFully(expectation)
                .replace("${currentDir}", expectation.getParentFile().getAbsolutePath())
                .replace("${componentsDir}", new File(root, "components").getAbsolutePath())
                .replace("${space}", " ")
                .replace("#todo", "");
    }
}