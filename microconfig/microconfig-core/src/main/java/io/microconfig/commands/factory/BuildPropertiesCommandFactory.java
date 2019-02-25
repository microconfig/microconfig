package io.microconfig.commands.factory;

import io.microconfig.commands.Command;
import io.microconfig.commands.CompositeCommand;
import io.microconfig.commands.PropertiesPostProcessor;
import io.microconfig.commands.postprocessors.CopyTemplatesPostProcessor;
import io.microconfig.commands.postprocessors.SecretPropertiesPostProcessor;
import io.microconfig.properties.io.ConfigIoServiceSelector;
import io.microconfig.properties.io.properties.PropertiesConfigIoService;
import io.microconfig.properties.io.yaml.YamlConfigIoService;
import io.microconfig.templates.CopyTemplatesServiceImpl;

import java.io.File;

import static io.microconfig.commands.factory.ConfigType.*;
import static io.microconfig.templates.RelativePathResolver.empty;
import static io.microconfig.templates.TemplatePattern.defaultPattern;
import static java.util.Arrays.asList;

public class BuildPropertiesCommandFactory {
    public static Command newBuildPropertiesCommand(File repoDir, File destinationComponentDir) {
        MicroconfigFactory microconfigFactory = MicroconfigFactory.init(repoDir, destinationComponentDir);

        return new CompositeCommand(asList(
                microconfigFactory.newBuildCommand(SERVICE, copyTemplatesPostProcessor()),
                microconfigFactory.newBuildCommand(PROCESS),
                microconfigFactory.newBuildCommand(ENV),
                microconfigFactory.newBuildCommand(LOG4j),
                microconfigFactory.newBuildCommand(LOG4J2),
                microconfigFactory.newBuildCommand(SECRET, new SecretPropertiesPostProcessor(new ConfigIoServiceSelector(new YamlConfigIoService(), new PropertiesConfigIoService())))
        ));
    }

    private static PropertiesPostProcessor copyTemplatesPostProcessor() {
        return new CopyTemplatesPostProcessor(new CopyTemplatesServiceImpl(defaultPattern(), empty()));
    }
}