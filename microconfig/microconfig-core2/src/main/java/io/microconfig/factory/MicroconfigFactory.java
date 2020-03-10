package io.microconfig.factory;

import io.microconfig.domain.Environment;
import io.microconfig.domain.impl.environment.filebased.EnvironmentParserImpl;
import io.microconfig.domain.impl.environment.filebased.Environments;
import io.microconfig.domain.impl.environment.filebased.FileBasedEnvironments;
import io.microconfig.utils.reader.FsIo;
import io.microconfig.utils.reader.Io;
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
    private Io io = new FsIo();
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
