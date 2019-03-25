package deployment.mgmt.microconfig;

import deployment.mgmt.configs.updateconfigs.OldConfigsRelativePathResolver;
import io.microconfig.commands.Command;
import io.microconfig.commands.buildconfig.BuildConfigCommand;
import io.microconfig.commands.buildconfig.BuildConfigPostProcessor;
import io.microconfig.commands.buildconfig.entry.BuildConfigMain;
import io.microconfig.commands.buildconfig.factory.MicroconfigFactory;
import io.microconfig.features.templates.CopyTemplatesPostProcessor;
import io.microconfig.features.templates.CopyTemplatesServiceImpl;

import java.io.File;
import java.util.List;

import static io.microconfig.commands.Command.composite;
import static io.microconfig.commands.buildconfig.factory.CommandFactory.updateSecretsPostProcessor;
import static io.microconfig.commands.buildconfig.factory.ConfigType.extensionAsName;
import static io.microconfig.commands.buildconfig.factory.StandardConfigType.*;
import static io.microconfig.features.templates.TemplatePattern.defaultPattern;

public class MgmtMicroConfigAdapter {
    static final String MGMT = ".mgmt";

    public static void execute(String env, List<String> groups, File root, File componentsDir, List<String> components) {
        Command command = newBuildPropertiesCommand(root, componentsDir);
        BuildConfigMain.execute(command, env, groups, components);
    }

    private static Command newBuildPropertiesCommand(File repoDir, File componentsDir) {
        MicroconfigFactory factory = MicroconfigFactory.init(repoDir, componentsDir);

        BuildConfigCommand serviceCommon = factory.newBuildCommand(SERVICE.type(), copyTemplatesPostProcessor(repoDir));
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
                new GenerateComponentListCommand(componentsDir, factory.getEnvironmentProvider()),
                new CopyHelpFilesCommand(factory.getEnvironmentProvider(), factory.getComponentTree(), componentsDir.toPath())
        );
    }

    private static BuildConfigPostProcessor copyTemplatesPostProcessor(File configsRoot) {
        return new CopyTemplatesPostProcessor(
                new CopyTemplatesServiceImpl(
                        defaultPattern().withTemplatePrefix("mgmt.template."),
                        new OldConfigsRelativePathResolver(configsRoot.getParentFile())
                )
        );
    }
}