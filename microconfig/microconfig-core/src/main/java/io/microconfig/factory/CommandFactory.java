package io.microconfig.factory;

import io.microconfig.commands.Command;
import io.microconfig.commands.buildconfig.BuildConfigPostProcessor;
import io.microconfig.commands.buildconfig.features.templates.CopyTemplatesPostProcessor;
import io.microconfig.commands.buildconfig.features.templates.CopyTemplatesServiceImpl;

import java.io.File;

import static io.microconfig.commands.Command.composite;
import static io.microconfig.factory.StandardConfigTypes.*;

class CommandFactory {
    static Command newBuildCommand(File rootDir, File destinationComponentDir) {
        MicroconfigFactory factory = MicroconfigFactory.init(rootDir, destinationComponentDir);

        return composite(
                factory.newBuildCommand(APPLICATION.type(), copyTemplatesPostProcessor()),
                factory.newBuildCommand(PROCESS.type()),
                factory.newBuildCommand(DEPLOY.type()),
                factory.newBuildCommand(ENV.type()),
                factory.newBuildCommand(SECRET.type()),
                factory.newBuildCommand(LOG4j.type()),
                factory.newBuildCommand(LOG4J2.type())
        );
    }

    private static BuildConfigPostProcessor copyTemplatesPostProcessor() {
        return new CopyTemplatesPostProcessor(new CopyTemplatesServiceImpl());
    }
}