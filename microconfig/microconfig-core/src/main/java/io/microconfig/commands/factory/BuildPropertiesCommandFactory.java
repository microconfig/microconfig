package io.microconfig.commands.factory;

import io.microconfig.commands.Command;
import io.microconfig.commands.CompositeCommand;
import io.microconfig.commands.PropertiesPostProcessor;
import io.microconfig.commands.postprocessors.CopyTemplatesPostProcessor;
import io.microconfig.commands.postprocessors.SecretPropertiesPostProcessor;
import io.microconfig.io.BaseConfigIoService;
import io.microconfig.templates.CopyTemplatesServiceImpl;

import java.io.File;

import static io.microconfig.commands.factory.ConfigType.*;
import static io.microconfig.templates.RelativePathResolver.empty;
import static io.microconfig.templates.TemplatePattern.defaultPattern;
import static java.util.Arrays.asList;

public class BuildPropertiesCommandFactory {
    public static Command newBuildPropertiesCommand(File repoDir, File destinationComponentDir) {
        BuildCommands buildCommands = BuildCommands.init(repoDir, destinationComponentDir);

        return new CompositeCommand(asList(
                buildCommands.newBuildCommand(SERVICE, copyTemplatesPostProcessor()),
                buildCommands.newBuildCommand(PROCESS),
                buildCommands.newBuildCommand(ENV),
                buildCommands.newBuildCommand(LOG4j),
                buildCommands.newBuildCommand(LOG4J2),
                buildCommands.newBuildCommand(SECRET, new SecretPropertiesPostProcessor(BaseConfigIoService.getInstance()))
        ));
    }

    private static PropertiesPostProcessor copyTemplatesPostProcessor() {
        return new CopyTemplatesPostProcessor(new CopyTemplatesServiceImpl(defaultPattern(), empty()));
    }
}