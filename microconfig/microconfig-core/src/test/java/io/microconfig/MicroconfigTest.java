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
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.Logger.info;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.partitioningBy;

public class MicroconfigTest {
    private final Microconfig microconfig = searchConfigsIn(classpathFile("repo"));

    @Test
    void testAllComponents() {
        try (Stream<Path> stream = walk(classpathFile("repo").toPath())) {
            Map<Boolean, Long> resultToCount = stream.map(Path::toFile)
                    .filter(this::isTest)
                    .map(this::execute)
                    .collect(partitioningBy(r -> r, counting()));
            info("\n\nSucceed: " + resultToCount.get(true) + ", Failed: " + resultToCount.get(false));
        }
    }

    private boolean isTest(File file) {
        return file.getName().endsWith(".test");
    }

    //todo aliases. highlight error
    private boolean execute(File expectation) {
        String component = expectation.getParentFile().getName();
        String actual = build(component, getName(expectation));
        String expected = readExpectation(expectation);

        boolean result = expected.equals(actual);
        if (result) {
            announce("Succeed: '" + component + "'");
        } else {
            info(red("Failed: '" + component + "'. Expected/Actual:")
                    + "\n" + expected
                    + "\n***\n" + actual
            );
        }
        return result;
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
                .replace("${currentDir}", expectation.getParentFile().getAbsolutePath());
    }
}
