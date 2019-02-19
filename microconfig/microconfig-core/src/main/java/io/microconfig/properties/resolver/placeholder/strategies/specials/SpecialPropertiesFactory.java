package io.microconfig.properties.resolver.placeholder.strategies.specials;

import io.microconfig.properties.files.provider.ComponentTree;
import io.microconfig.properties.resolver.placeholder.strategies.SpecialPropertyResolverStrategy.SpecialProperty;
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

    public Map<String, SpecialProperty> specialPropertiesByKeys() {
        return of(
                new ConfigDir(componentTree.getConfigComponentsRoot().getParentFile()),
                new EnvProperty(),
                new FolderProperty(componentTree),
                new GroupProperty(),
                new IpProperty(),
                new NameProperty(),
                new OrderProperty(),
                new PortOffsetProperty(),
                new ServiceDirProperty(destinationComponentDir),
                new UserHomeProperty()
        ).collect(toMap(SpecialProperty::key, identity()));
    }

    public Set<String> keyNames() {
        return specialPropertiesByKeys().keySet();
    }
}