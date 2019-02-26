package io.microconfig.commands.factory;

import io.microconfig.commands.BuildConfigPostProcessor;
import io.microconfig.commands.Command;
import io.microconfig.commands.CompositeCommand;
import io.microconfig.commands.postprocessors.CopyTemplatesPostProcessor;
import io.microconfig.commands.postprocessors.UpdateSecretsPostProcessor;
import io.microconfig.configs.files.io.ConfigIoServiceSelector;
import io.microconfig.configs.files.io.properties.PropertiesConfigIoService;
import io.microconfig.configs.files.io.yaml.YamlConfigIoService;
import io.microconfig.templates.CopyTemplatesServiceImpl;

import java.io.File;

import static io.microconfig.commands.factory.StandardConfigTypes.*;
import static io.microconfig.templates.RelativePathResolver.empty;
import static io.microconfig.templates.TemplatePattern.defaultPattern;
import static java.util.Arrays.asList;

public class BuildConfigCommandFactory {
    public static Command newBuildCommand(File repoDir, File destinationComponentDir) {
        MicroconfigFactory microconfigFactory = MicroconfigFactory.init(repoDir, destinationComponentDir);

        return new CompositeCommand(asList(
                microconfigFactory.newBuildCommand(SERVICE.type(), copyTemplatesPostProcessor()),
                microconfigFactory.newBuildCommand(PROCESS.type()),
                microconfigFactory.newBuildCommand(ENV.type()),
                microconfigFactory.newBuildCommand(LOG4j.type()),
                microconfigFactory.newBuildCommand(LOG4J2.type()),
                microconfigFactory.newBuildCommand(SECRET.type(), new UpdateSecretsPostProcessor(new ConfigIoServiceSelector(new YamlConfigIoService(), new PropertiesConfigIoService())))
        ));
    }

    private static BuildConfigPostProcessor copyTemplatesPostProcessor() {
        return new CopyTemplatesPostProcessor(new CopyTemplatesServiceImpl(defaultPattern(), empty()));
    }
}