package mgmt.microconfig;

import io.microconfig.commands.*;
import io.microconfig.commands.factory.MicroconfigFactory;
import io.microconfig.commands.postprocessors.CopyTemplatesPostProcessor;
import io.microconfig.commands.postprocessors.UpdateSecretsPostProcessor;
import io.microconfig.templates.CopyTemplatesServiceImpl;

import java.io.File;
import java.util.List;

import static io.microconfig.commands.factory.ConfigType.extensionAsName;
import static io.microconfig.commands.factory.StandardConfigType.*;
import static io.microconfig.templates.RelativePathResolver.empty;
import static io.microconfig.templates.TemplatePattern.defaultPattern;
import static java.util.Arrays.asList;

public class MgmtMicroConfigAdapter {
    public static void execute(String env, List<String> groups, File root, File componentsDir, List<String> components) {
        Command command = newBuildPropertiesCommand(root, componentsDir);
        BuildConfigMain.execute(command, env, groups, components);
    }

    private static Command newBuildPropertiesCommand(File repoDir, File componentsDir) {
        MicroconfigFactory factory = MicroconfigFactory.init(repoDir, componentsDir);

        BuildConfigCommand serviceCommon = factory.newBuildCommand(SERVICE.type(), copyTemplatesPostProcessor());
        factory = factory.withServiceInnerDir(".mgmt");
        return new CompositeCommand(asList(
                serviceCommon,
                factory.newBuildCommand(PROCESS.type(), new WebappPostProcessor()),
                factory.newBuildCommand(DEPLOY.type()),
                factory.newBuildCommand(ENV.type()),
                factory.newBuildCommand(SECRET.type(), new UpdateSecretsPostProcessor(factory.getConfigIo())),
                factory.newBuildCommand(LOG4j.type()),
                factory.newBuildCommand(LOG4J2.type()),
                factory.newBuildCommand(extensionAsName("sap")),
                new GenerateComponentListCommand(componentsDir, factory.getEnvironmentProvider()),
                new CopyHelpFilesCommand(factory.getEnvironmentProvider(), factory.getComponentTree(), componentsDir.toPath())
        ));
    }

    private static BuildConfigPostProcessor copyTemplatesPostProcessor() {
        return new CopyTemplatesPostProcessor(
                new CopyTemplatesServiceImpl(defaultPattern().toBuilder().templatePrefix("mgmt.template.").build(), empty())
        );
    }
}
