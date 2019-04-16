package io.microconfig.factory;

import java.io.File;
import java.util.List;

public interface ConfigsTypeProvider {
    List<ConfigType> getConfigTypes(File rootDir);
}