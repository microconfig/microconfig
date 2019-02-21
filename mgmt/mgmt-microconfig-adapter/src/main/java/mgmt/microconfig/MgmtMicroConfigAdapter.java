package mgmt.microconfig;

import io.microconfig.commands.*;
import io.microconfig.commands.factory.BuildCommands;
import io.microconfig.commands.postprocessors.CopyTemplatesPostProcessor;
import io.microconfig.commands.postprocessors.SecretPropertiesPostProcessor;
import io.microconfig.io.BaseConfigIoService;
import io.microconfig.templates.CopyTemplatesServiceImpl;

import java.io.File;
import java.util.List;

import static io.microconfig.commands.factory.ConfigType.*;
import static io.microconfig.templates.RelativePathResolver.empty;
import static io.microconfig.templates.TemplatePattern.defaultPattern;
import static java.util.Arrays.asList;

public class MgmtMicroConfigAdapter {
    public static void execute(String env, List<String> groups, File root, File componentsDir, List<String> components) {
        Command command = newBuildPropertiesCommand(root, componentsDir);
        BuildConfigMain.execute(command, env, groups, components);
    }

    private static Command newBuildPropertiesCommand(File repoDir, File componentsDir) {
        BuildCommands commands = BuildCommands.init(repoDir, componentsDir);

        BuildPropertiesCommand serviceCommon = commands.newBuildCommand(SERVICE, copyTemplatesPostProcessor());
        commands = commands.withServiceInnerDir(".mgmt");
        return new CompositeCommand(asList(
                serviceCommon,
                commands.newBuildCommand(PROCESS, new WebappPostProcessor()),
                commands.newBuildCommand(ENV),
                commands.newBuildCommand(LOG4j),
                commands.newBuildCommand(LOG4J2),
                commands.newBuildCommand(SAP),
                commands.newBuildCommand(SECRET, new SecretPropertiesPostProcessor(BaseConfigIoService.getInstance())),
                new GenerateComponentListCommand(componentsDir, commands.getEnvironmentProvider()),
                new GenerateHelpCommand(commands.getEnvironmentProvider(), commands.getComponentTree(), componentsDir.toPath())
        ));
    }

    private static PropertiesPostProcessor copyTemplatesPostProcessor() {
        return new CopyTemplatesPostProcessor(new CopyTemplatesServiceImpl(defaultPattern().toBuilder().templatePrefix("mgmt.template.").build(), empty()));
    }

}
