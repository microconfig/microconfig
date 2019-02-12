package io.microconfig.commands.factory;

import io.microconfig.commands.Command;
import io.microconfig.commands.CompositeCommand;
import io.microconfig.commands.GenerateCListCommand;
import io.microconfig.commands.GenerateHelpCommand;
import io.microconfig.commands.postprocessors.SecretPropertiesPostProcessor;
import io.microconfig.commands.postprocessors.WebappPostProcessor;
import io.microconfig.properties.serializer.PropertiesDiffWriter;
import io.microconfig.properties.serializer.PropertiesSerializerImpl;

import java.io.File;
import java.util.List;

import static io.microconfig.commands.factory.PropertyType.*;

//todo2 environment mapping cr-psi->uat-sbrf
public class BuildAllCommandFactory {
    public static Command newBuildPropertiesCommand(File repoDir, File componentsDir) {
        BuildCommands buildCommands = BuildCommands.init(repoDir, componentsDir);

        return new CompositeCommand(List.of(
                buildCommands.newBuildCommand(SERVICE, new PropertiesDiffWriter(new PropertiesSerializerImpl(componentsDir, SERVICE.getResultFile()))),
                buildCommands.newBuildCommand(PROCESS, new WebappPostProcessor()),
                buildCommands.newBuildCommand(ENV),
                buildCommands.newBuildCommand(LOG4j),
                buildCommands.newBuildCommand(LOG4J2),
                buildCommands.newBuildCommand(SAP),
                buildCommands.newBuildCommand(SECRET, new SecretPropertiesPostProcessor()),
                new GenerateCListCommand(componentsDir, buildCommands.getEnvironmentProvider()),
                new GenerateHelpCommand(buildCommands.getEnvironmentProvider(), buildCommands.getComponentTree(), componentsDir.toPath())
        ));
    }
}