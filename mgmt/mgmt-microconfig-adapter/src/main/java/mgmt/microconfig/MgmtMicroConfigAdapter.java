package mgmt.microconfig;

import io.microconfig.commands.Command;
import io.microconfig.commands.build.BuildConfigCommand;
import io.microconfig.commands.build.BuildConfigPostProcessor;
import io.microconfig.commands.build.entry.BuildConfigMain;
import io.microconfig.commands.build.factory.MicroconfigFactory;
import io.microconfig.commands.build.postprocessors.CopyTemplatesPostProcessor;
import io.microconfig.commands.build.postprocessors.UpdateSecretsPostProcessor;
import io.microconfig.templates.CopyTemplatesServiceImpl;

import java.io.File;
import java.util.List;

import static io.microconfig.commands.Command.composite;
import static io.microconfig.configs.types.ConfigType.extensionAsName;
import static io.microconfig.configs.types.StandardConfigType.*;
import static io.microconfig.templates.RelativePathResolver.empty;
import static io.microconfig.templates.TemplatePattern.defaultPattern;

public class MgmtMicroConfigAdapter {
    public static void execute(String env, List<String> groups, File root, File componentsDir, List<String> components) {
        Command command = newBuildPropertiesCommand(root, componentsDir);
        BuildConfigMain.execute(command, env, groups, components);
    }

    private static Command newBuildPropertiesCommand(File repoDir, File componentsDir) {
        MicroconfigFactory factory = MicroconfigFactory.init(repoDir, componentsDir);

        BuildConfigCommand serviceCommon = factory.newBuildCommand(SERVICE.type(), copyTemplatesPostProcessor());
        factory = factory.withServiceInnerDir(".mgmt");
        return composite(
                serviceCommon,
                factory.newBuildCommand(PROCESS.type(), new WebappPostProcessor()),
                factory.newBuildCommand(DEPLOY.type()),
                factory.newBuildCommand(ENV.type()),
                factory.newBuildCommand(SECRET.type(), new UpdateSecretsPostProcessor(factory.getConfigIoService())),
                factory.newBuildCommand(LOG4j.type()),
                factory.newBuildCommand(LOG4J2.type()),
                factory.newBuildCommand(extensionAsName("sap")),
                new GenerateComponentListCommand(componentsDir, factory.getEnvironmentProvider()),
                new CopyHelpFilesCommand(factory.getEnvironmentProvider(), factory.getComponentTree(), componentsDir.toPath())
        );
    }

    private static BuildConfigPostProcessor copyTemplatesPostProcessor() {
        return new CopyTemplatesPostProcessor(
                new CopyTemplatesServiceImpl(defaultPattern().toBuilder().templatePrefix("mgmt.template.").build(), empty())
        );
    }
}
