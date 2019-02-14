package mgmt.microconfig;

import io.microconfig.commands.BuildConfigMain;
import io.microconfig.commands.Command;
import io.microconfig.commands.CompositeCommand;
import io.microconfig.commands.factory.BuildCommands;
import io.microconfig.commands.postprocessors.SecretPropertiesPostProcessor;
import io.microconfig.properties.serializer.PropertiesDiffWriter;
import io.microconfig.properties.serializer.PropertiesSerializerImpl;

import java.io.File;
import java.util.List;

import static io.microconfig.commands.factory.PropertyType.*;

public class MgmtMicroConfigAdapter {
    public static void execute(String env, List<String> groups, File root, File componentsDir, List<String> components) {
        Command command = newBuildPropertiesCommand(root, componentsDir);
        BuildConfigMain.execute(command, env, groups, components);
    }

    public static BuildCommands mgmtBuildCommands(File repoDir, File componentsDir) {
        return BuildCommands.init(repoDir, componentsDir, ".mgmt");
    }

    private static Command newBuildPropertiesCommand(File repoDir, File componentsDir) {
        BuildCommands buildCommands = mgmtBuildCommands(repoDir, componentsDir);

        return new CompositeCommand(List.of(
                buildCommands.newBuildCommand(SERVICE, new PropertiesDiffWriter(new PropertiesSerializerImpl(componentsDir, SERVICE.getResultFile()))),
                buildCommands.newBuildCommand(PROCESS, new WebappPostProcessor()),
                buildCommands.newBuildCommand(ENV),
                buildCommands.newBuildCommand(LOG4j),
                buildCommands.newBuildCommand(LOG4J2),
                buildCommands.newBuildCommand(SAP),
                buildCommands.newBuildCommand(SECRET, new SecretPropertiesPostProcessor()),
                new GenerateComponentListCommand(componentsDir, buildCommands.getEnvironmentProvider()),
                new GenerateHelpCommand(buildCommands.getEnvironmentProvider(), buildCommands.getComponentTree(), componentsDir.toPath())
        ));
    }
}
