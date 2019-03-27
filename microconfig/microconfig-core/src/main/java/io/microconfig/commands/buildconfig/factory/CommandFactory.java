package io.microconfig.commands.buildconfig.factory;

import io.microconfig.commands.Command;
import io.microconfig.commands.buildconfig.BuildConfigPostProcessor;
import io.microconfig.configs.io.ioservice.ConfigIoService;
import io.microconfig.features.secrets.SecretServiceImpl;
import io.microconfig.features.secrets.UpdateSecretsPostProcessor;
import io.microconfig.features.templates.CopyTemplatesPostProcessor;
import io.microconfig.features.templates.CopyTemplatesServiceImpl;

import java.io.File;

import static io.microconfig.commands.Command.composite;
import static io.microconfig.commands.buildconfig.factory.StandardConfigType.*;
import static io.microconfig.utils.FileUtils.userHome;

public class CommandFactory {
    public static Command newBuildCommand(File rootDir, File destinationComponentDir) {
        MicroconfigFactory factory = MicroconfigFactory.init(rootDir, destinationComponentDir);

        return composite(
                factory.newBuildCommand(APPLICATION.type(), copyTemplatesPostProcessor()),
                factory.newBuildCommand(PROCESS.type()),
                factory.newBuildCommand(DEPLOY.type()),
                factory.newBuildCommand(ENV.type()),
                factory.newBuildCommand(SECRET.type(), updateSecretsPostProcessor(factory.getConfigIoService())),
                factory.newBuildCommand(LOG4j.type()),
                factory.newBuildCommand(LOG4J2.type())
        );
    }

    private static BuildConfigPostProcessor copyTemplatesPostProcessor() {
        return new CopyTemplatesPostProcessor(new CopyTemplatesServiceImpl());
    }

    public static BuildConfigPostProcessor updateSecretsPostProcessor(ConfigIoService configIoService) {
        File secretFile = new File(userHome(), "/secret/" + SECRET.type().getResultFileName() + ".properties");
        return new UpdateSecretsPostProcessor(new SecretServiceImpl(secretFile, configIoService));
    }
}