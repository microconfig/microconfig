package io.microconfig;

import io.microconfig.domain.Environment;
import io.microconfig.domain.Environments;
import io.microconfig.domain.impl.environment.provider.EnvironmentParserImpl;
import io.microconfig.domain.impl.environment.provider.FileBasedEnvironments;
import io.microconfig.io.formats.FileSystemIo;
import io.microconfig.io.formats.Io;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.File;

import static io.microconfig.io.FileUtils.canonical;

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
        return environments().withName(name);
    }

    private Environments environments() {
        return environments == null ?
                environments = new FileBasedEnvironments(new File(rootDir, ENV_DIR), new EnvironmentParserImpl(io)) :
                environments;
    }
}