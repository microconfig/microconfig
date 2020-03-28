package io.microconfig.core.properties.repository;

import io.microconfig.core.configtypes.ConfigType;

import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;

public interface ConfigFileRepository {
    Stream<ConfigFile> getConfigFilesFor(String component, String environment, ConfigType configType);

    Optional<File> getFolderOf(String component);
}