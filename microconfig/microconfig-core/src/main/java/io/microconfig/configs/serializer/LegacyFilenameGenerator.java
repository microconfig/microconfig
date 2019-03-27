package io.microconfig.configs.serializer;

import io.microconfig.configs.Property;
import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.utils.FileUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.function.Predicate;

import static io.microconfig.commands.buildconfig.factory.StandardConfigType.APPLICATION;
import static io.microconfig.utils.FileUtils.extension;

@RequiredArgsConstructor
public class LegacyFilenameGenerator implements FilenameGenerator {
    private static final String LEGACY_APPLICATION_NAME = "service";

    private final FilenameGenerator filenameGenerator;
    private final EnvironmentProvider environmentProvider;

    @Override
    public File fileFor(String component, String env, Collection<Property> properties) {
        File original = filenameGenerator.fileFor(component, env, properties);
        return shouldRename(original, env) ? renameToLegacy(original) : original;
    }

    private boolean shouldRename(File original, String envName) {
        Predicate<File> applicationFile = f -> filename(f).equals(APPLICATION.type().getResultFileName());
        Predicate<String> jsonEnv = env -> {
            Object source = environmentProvider.getByName(envName).getSource();
            return source != null && source.toString().endsWith(".json");
        };

        return applicationFile.test(original) && jsonEnv.test(envName);
    }

    private File renameToLegacy(File original) {
        return new File(original.getParent(), LEGACY_APPLICATION_NAME + extension(original));
    }

    private String filename(File original) {
        String name = original.getName();
        return name.substring(0, name.lastIndexOf('.'));
    }
}