package io.microconfig.domain.impl.environment.filebased;


import io.microconfig.domain.Environment;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.service.ioservice.ConfigFormat.YAML;
import static io.microconfig.utils.CollectionUtils.singleValue;
import static io.microconfig.utils.FileUtils.walk;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public class FileBasedEnvironments implements Environments {
    private final File envDir;
    private final EnvironmentParser parser;

    public FileBasedEnvironments(File envDir, EnvironmentParser parser) {
        this.envDir = envDir;
        this.parser = parser;

        if (!envDir.exists()) {
            throw new IllegalArgumentException("Env directory doesn't exist " + envDir);
        }
    }

    @Override
    public List<Environment> all() {
        return envFiles(withYamlExtension())
                .stream()
                .map(f -> parser.parse(getEnvName(f), f))
                .collect(toList());
    }

    @Override
    public Set<String> environmentNames() {
        return envFiles(withYamlExtension())
                .stream()
                .map(this::getEnvName)
                .collect(toCollection(TreeSet::new));
    }

    @Override
    public Environment byName(String name) {
        return parser.parse(name, envFile(name));
//                .processInclude(this)
//                .verifyUniqueComponentNames();
    }

    private String getEnvName(File file) {
        String name = file.getName();
        return name.substring(0, name.lastIndexOf('.'));
    }

    private File envFile(String name) {
        List<File> files = envFiles(withName(name));

        if (files.size() > 1) {
            throw new IllegalArgumentException("Found several env files with name " + name);
        }
        if (files.isEmpty()) {
            throw new EnvironmentDoesNotExistException("Can't find env with name " + name);
        }
        return singleValue(files);
    }

    private List<File> envFiles(Predicate<File> predicate) {
        try (Stream<Path> stream = walk(envDir.toPath())) {
            return stream
                    .map(Path::toFile)
                    .filter(predicate)
                    .collect(toList());
        }
    }

    private Predicate<File> withName(String envName) {
        return f -> f.getName().equals(envName + YAML.extension());
    }

    private Predicate<File> withYamlExtension() {
        return f -> f.getName().endsWith(YAML.extension());
    }
}
