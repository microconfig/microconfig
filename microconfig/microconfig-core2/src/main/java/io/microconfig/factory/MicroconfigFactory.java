package io.microconfig.factory;

import io.microconfig.domain.Environment;
import io.microconfig.domain.impl.environment.provider.EnvironmentParserImpl;
import io.microconfig.domain.impl.environment.provider.Environments;
import io.microconfig.domain.impl.environment.provider.FileBasedEnvironments;
import io.microconfig.service.io.FileSystemIo;
import io.microconfig.service.io.Io;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.File;

import static io.microconfig.utils.FileUtils.canonical;

@Accessors(fluent = true)
@RequiredArgsConstructor
public class MicroconfigFactory {
    public static final String ENV_DIR = "envs";

    private final File rootDir;
    @Setter
    private Io io = new FileSystemIo();
    private Environments environments;

    public static MicroconfigFactory searchConfigsIn(File rootDir) {
        return new MicroconfigFactory(canonical(rootDir));
    }

    public Environment inEnvironment(String name) {
        return getEnvironments().byName(name);
    }

    private Environments getEnvironments() {
        return environments == null ?
                environments = new FileBasedEnvironments(new File(rootDir, ENV_DIR), new EnvironmentParserImpl(io)) :
                environments;
    }
}
