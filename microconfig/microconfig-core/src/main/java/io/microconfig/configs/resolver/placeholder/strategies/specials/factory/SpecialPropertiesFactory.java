package io.microconfig.configs.resolver.placeholder.strategies.specials.factory;

import io.microconfig.configs.files.tree.ComponentTree;
import io.microconfig.configs.resolver.placeholder.strategies.EnvSpecificResolveStrategy.EnvProperty;
import io.microconfig.configs.resolver.placeholder.strategies.GeneralPropertiesResolveStrategy.GeneralProperty;
import io.microconfig.configs.resolver.placeholder.strategies.specials.envbased.GroupProperty;
import io.microconfig.configs.resolver.placeholder.strategies.specials.envbased.IpProperty;
import io.microconfig.configs.resolver.placeholder.strategies.specials.envbased.OrderProperty;
import io.microconfig.configs.resolver.placeholder.strategies.specials.envbased.PortOffsetProperty;
import io.microconfig.configs.resolver.placeholder.strategies.specials.general.*;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Map;
import java.util.Set;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.of;

@RequiredArgsConstructor
public class SpecialPropertiesFactory {
    private final ComponentTree componentTree;
    private final File destinationComponentDir;

    public Map<String, GeneralProperty> general() {
        return of(
                new ConfigDir(componentTree.getRootDir()),
                new FolderProperty(componentTree),
                new NameProperty(),
                new ServiceDirProperty(destinationComponentDir),
                new UserHomeProperty()
        ).collect(toMap(GeneralProperty::key, identity()));
    }

    public Map<String, EnvProperty> envBased() {
        return of(
                new io.microconfig.configs.resolver.placeholder.strategies.specials.envbased.EnvProperty(),
                new GroupProperty(),
                new IpProperty(),
                new OrderProperty(),
                new PortOffsetProperty()
        ).collect(toMap(EnvProperty::key, identity()));
    }

    public Set<String> keys() {
        return general().keySet();
    }
}