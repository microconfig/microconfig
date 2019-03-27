package deployment.mgmt.microconfig;

import io.microconfig.commands.Command;
import io.microconfig.commands.buildconfig.BuildConfigCommand;
import io.microconfig.factory.BuildConfigMain;
import io.microconfig.factory.MicroconfigFactory;

import java.io.File;
import java.util.List;

import static io.microconfig.commands.Command.composite;
import static io.microconfig.factory.CommandFactory.updateSecretsPostProcessor;
import static io.microconfig.factory.ConfigType.extensionAsName;
import static io.microconfig.factory.StandardConfigTypes.*;

public class MgmtMicroConfigAdapter {
    static final String MGMT = ".mgmt";

    public static void execute(String env, List<String> groups, List<String> components, File sourcesRootDir, File destinationComponentDir) {
        Command command = newBuildPropertiesCommand(sourcesRootDir, destinationComponentDir);
        BuildConfigMain.execute(command, env, groups, components);
    }

    private static Command newBuildPropertiesCommand(File sourcesRootDir, File destinationComponentDir) {
        MicroconfigFactory factory = MicroconfigFactory.init(sourcesRootDir, destinationComponentDir);

        BuildConfigCommand serviceCommon = factory.newBuildCommand(APPLICATION.type());
        factory = factory.withServiceInnerDir(MGMT);
        return composite(
                serviceCommon,
                factory.newBuildCommand(PROCESS.type(), new WebappPostProcessor()),
                factory.newBuildCommand(DEPLOY.type()),
                factory.newBuildCommand(ENV.type()),
                factory.newBuildCommand(SECRET.type(), updateSecretsPostProcessor(factory.getConfigIoService())),
                factory.newBuildCommand(LOG4j.type()),
                factory.newBuildCommand(LOG4J2.type()),
                factory.newBuildCommand(extensionAsName("sap")),
                new GenerateComponentListCommand(destinationComponentDir, factory.getEnvironmentProvider()),
                new CopyHelpFilesCommand(factory.getEnvironmentProvider(), factory.getComponentTree(), destinationComponentDir.toPath())
        );
    }
}