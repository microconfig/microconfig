package io.microconfig.commands.factory;

import io.microconfig.commands.BuildConfigPostProcessor;
import io.microconfig.commands.Command;
import io.microconfig.commands.CompositeCommand;
import io.microconfig.commands.postprocessors.CopyTemplatesPostProcessor;
import io.microconfig.commands.postprocessors.UpdateSecretsPostProcessor;
import io.microconfig.configs.files.io.ConfigIoService;
import io.microconfig.templates.CopyTemplatesServiceImpl;

import java.io.File;

import static io.microconfig.commands.factory.StandardConfigType.*;
import static io.microconfig.templates.RelativePathResolver.empty;
import static io.microconfig.templates.TemplatePattern.defaultPattern;
import static java.util.Arrays.asList;

public class BuildConfigCommandFactory {
    public static Command newBuildCommand(File rootDir, File destinationComponentDir) {
        MicroconfigFactory factory = MicroconfigFactory.init(rootDir, destinationComponentDir);

        return new CompositeCommand(asList(
                factory.newBuildCommand(SERVICE.type(), copyTemplatesPostProcessor()),
                factory.newBuildCommand(PROCESS.type()),
                factory.newBuildCommand(DEPLOY.type()),
                factory.newBuildCommand(ENV.type()),
                factory.newBuildCommand(SECRET.type(), updateSecretsPostProcessor(factory.getConfigIoService())),
                factory.newBuildCommand(LOG4j.type()),
                factory.newBuildCommand(LOG4J2.type())
        ));
    }

    private static BuildConfigPostProcessor copyTemplatesPostProcessor() {
        return new CopyTemplatesPostProcessor(new CopyTemplatesServiceImpl(defaultPattern(), empty()));
    }

    private static BuildConfigPostProcessor updateSecretsPostProcessor(ConfigIoService configIoService) {
        return new UpdateSecretsPostProcessor(configIoService);
    }
}