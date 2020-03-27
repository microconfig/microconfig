package io.microconfig.factory;

import io.microconfig.commands.ConfigCommand;
import io.microconfig.commands.buildconfigs.BuildConfigPostProcessor;
import io.microconfig.commands.buildconfigs.features.templates.CopyTemplatesPostProcessor;
import io.microconfig.commands.buildconfigs.features.templates.CopyTemplatesServiceImpl;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;

import static io.microconfig.commands.ConfigCommand.composite;

@RequiredArgsConstructor
public class BuildConfigCommandFactory {
    private final ConfigsTypeProvider configsTypeProvider;

    public ConfigCommand newCommand(File rootDir, File destinationComponentDir) {
        List<ConfigType> configTypes = configsTypeProvider.getConfigTypes(rootDir);
        BuildConfigPostProcessor postProcessor = copyTemplatesPostProcessor();

        MicroconfigFactory factory = MicroconfigFactory.init(rootDir, destinationComponentDir);
        return composite(
                configTypes.stream()
                        .map(factory::newBuildCommand)
                        .toArray(ConfigCommand[]::new)
        );
    }

    private BuildConfigPostProcessor copyTemplatesPostProcessor() {
        return new CopyTemplatesPostProcessor(new CopyTemplatesServiceImpl());
    }
}