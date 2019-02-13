package io.microconfig.commands.factory;

import io.microconfig.commands.Command;
import io.microconfig.commands.CompositeCommand;
import io.microconfig.properties.serializer.PropertiesDiffWriter;
import io.microconfig.properties.serializer.PropertiesSerializerImpl;

import java.io.File;

import static io.microconfig.commands.factory.PropertyType.*;
import static java.util.Arrays.asList;

public class BuildPropertiesCommandFactory {
    public static Command newBuildPropertiesCommand(File repoDir, File componentsDir) {
        BuildCommands buildCommands = BuildCommands.init(repoDir, componentsDir);

        return new CompositeCommand(asList(
                buildCommands.newBuildCommand(SERVICE, new PropertiesDiffWriter(new PropertiesSerializerImpl(componentsDir, SERVICE.getResultFile()))),
                buildCommands.newBuildCommand(PROCESS),
                buildCommands.newBuildCommand(ENV),
                buildCommands.newBuildCommand(LOG4j),
                buildCommands.newBuildCommand(LOG4J2),
                buildCommands.newBuildCommand(SECRET, new SecretPropertiesPostProcessor())
        ));
    }
}