package io.microconfig;

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
import static io.microconfig.utils.FileUtils.getName;
import static io.microconfig.utils.FileUtils.walk;
import static io.microconfig.utils.IoUtils.readFully;
import static io.microconfig.utils.Logger.*;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.partitioningBy;

public class MicroconfigTest {
    private final File root = classpathFile("repo");
    private final Microconfig microconfig = searchConfigsIn(root);

    @Test
    void testAllComponents() {
        try (Stream<Path> stream = walk(classpathFile("repo").toPath())) {
            Map<Boolean, Long> resultToCount = stream.map(Path::toFile)
                    .filter(this::isExpectation)
                    .map(this::execute)
                    .collect(partitioningBy(r -> r, counting()));
            info("\n\nSucceed: " + resultToCount.get(true) + ", Failed: " + resultToCount.get(false));
        }
    }

    private boolean isExpectation(File file) {
        return file.getName().endsWith(".expect");
    }

    //highlight error
    private boolean execute(File expectation) {
        String component = getComponentName(expectation);
        String env = getEnvName(expectation);
        String actual = build(component, env);
        String expected = readExpectation(expectation);

        boolean result = expected.equals(actual);
        if (result) {
            announce("Succeed '" + component + "'");
        } else {
            info(red("Failed '" + component + "'. Expected/Actual:")
                    + "\n" + expected
                    + "\n***\n" + actual
            );
        }
        return result;
    }

    private String getComponentName(File expectation) {
        String name = expectation.getName();
        return name.contains(":") ?
                name.substring(0, name.indexOf(':')) :
                expectation.getParentFile().getName();
    }

    private String getEnvName(File expectation) {
        String name = expectation.getName();
        return name.contains(":") ?
                name.substring(name.indexOf(':') + 1, name.lastIndexOf('.')) :
                getName(expectation);
    }

    private String build(String component, String env) {
        try {
            return microconfig.environments()
                    .getOrCreateByName(env)
                    .getOrCreateComponentWithName(component)
                    .getPropertiesFor(configType(APPLICATION))
                    .resolveBy(microconfig.resolver())
                    .first()
                    .save(asString());
        } catch (RuntimeException e) {
            error("Failed '" + component + ":[" + env + "]'");
            throw e;
        }
    }

    private String readExpectation(File expectation) {
        return readFully(expectation)
                .replace("${currentDir}", expectation.getParentFile().getAbsolutePath())
                .replace("${componentsDir}", new File(root, "components").getAbsolutePath());
    }
}
