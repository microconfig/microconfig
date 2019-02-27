package io.microconfig.commands.buildconfig.factory;

import io.microconfig.commands.Command;
import io.microconfig.commands.buildconfig.BuildConfigPostProcessor;
import io.microconfig.commands.buildconfig.postprocessors.CopyTemplatesPostProcessor;
import io.microconfig.commands.buildconfig.postprocessors.UpdateSecretsPostProcessor;
import io.microconfig.configs.io.ioservice.ConfigIoService;
import io.microconfig.features.secrets.SecretServiceImpl;
import io.microconfig.features.templates.CopyTemplatesServiceImpl;

import java.io.File;

import static io.microconfig.commands.Command.composite;
import static io.microconfig.commands.buildconfig.factory.StandardConfigType.*;
import static io.microconfig.features.templates.RelativePathResolver.empty;
import static io.microconfig.features.templates.TemplatePattern.defaultPattern;
import static io.microconfig.utils.FileUtils.userHome;

public class CommandFactory {
    public static Command newBuildCommand(File rootDir, File destinationComponentDir) {
        MicroconfigFactory factory = MicroconfigFactory.init(rootDir, destinationComponentDir);

        return composite(
                factory.newBuildCommand(SERVICE.type(), copyTemplatesPostProcessor()),
                factory.newBuildCommand(PROCESS.type()),
                factory.newBuildCommand(DEPLOY.type()),
                factory.newBuildCommand(ENV.type()),
                factory.newBuildCommand(SECRET.type(), updateSecretsPostProcessor(factory.getConfigIoService())),
                factory.newBuildCommand(LOG4j.type()),
                factory.newBuildCommand(LOG4J2.type())
        );
    }

    public static BuildConfigPostProcessor copyTemplatesPostProcessor() {
        return new CopyTemplatesPostProcessor(new CopyTemplatesServiceImpl(defaultPattern(), empty()));
    }

    public static BuildConfigPostProcessor updateSecretsPostProcessor(ConfigIoService configIoService) {
        File secretFile = new File(userHome(), "/secret/" + SECRET.getResultFileName() + ".properties");
        return new UpdateSecretsPostProcessor(new SecretServiceImpl(secretFile, configIoService));
    }
}