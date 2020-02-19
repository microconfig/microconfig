package io.microconfig.properties.serializer;

import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.properties.Property;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.function.Predicate;

import static io.microconfig.utils.FileUtils.extension;
import static io.microconfig.utils.FileUtils.filename;

@RequiredArgsConstructor
public class LegacyFilenameGenerator implements FilenameGenerator {
    private static final String LEGACY_ENV_EXTENSION = ".json";
    private static final String LEGACY_APPLICATION_NAME = "service";

    private final String resultFileName;
    private final FilenameGenerator filenameGenerator;
    private final EnvironmentProvider environmentProvider;

    @Override
    public File fileFor(String component, String env, Collection<Property> properties) {
        File original = filenameGenerator.fileFor(component, env, properties);
        return shouldRename(original, env) ? renameToLegacy(original) : original;
    }

    private boolean shouldRename(File original, String envName) {
        Predicate<File> applicationFile = f -> filename(f).equals(resultFileName);

        Predicate<String> legacyJsonEnv = env -> {
            try {
                return environmentProvider.getByName(envName)
                        .getSource()
                        .toString()
                        .endsWith(LEGACY_ENV_EXTENSION);
            } catch (RuntimeException e) {
                return false;
            }
        };

        return applicationFile.test(original) && legacyJsonEnv.test(envName);
    }

    private File renameToLegacy(File original) {
        return new File(original.getParent(), LEGACY_APPLICATION_NAME + extension(original));
    }
}