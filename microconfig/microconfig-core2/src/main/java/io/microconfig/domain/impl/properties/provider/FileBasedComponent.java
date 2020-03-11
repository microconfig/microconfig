package io.microconfig.domain.impl.properties.provider;

import io.microconfig.domain.*;
import io.microconfig.service.tree.ComponentTree;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.microconfig.service.tree.ConfigFileFilters.*;


@RequiredArgsConstructor
public class FileBasedComponent implements Component {
    private final String env;
    private final String componentName;

    private final ComponentTree componentTree;

    @Override
    public String getName() {
        return componentName;
    }

    @Override
    public ConfigBuildResults buildPropertiesFor(ConfigTypeFilter filter) {
        return null;
    }
}