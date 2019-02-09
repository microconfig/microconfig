package deployment.configs.command.factory;

import deployment.configs.command.Command;
import deployment.configs.command.CompositeCommand;
import deployment.configs.command.GenerateCListCommand;
import deployment.configs.command.GenerateHelpCommand;
import deployment.configs.command.postprocessors.SecretPropertiesPostProcessor;
import deployment.configs.command.postprocessors.WebappPostProcessor;
import deployment.configs.properties.serializer.PropertiesDiffWriter;
import deployment.configs.properties.serializer.PropertiesSerializerImpl;

import java.io.File;
import java.util.List;

import static deployment.configs.command.factory.PropertyType.*;

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