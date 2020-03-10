package io.microconfig.domain;

import java.util.List;

public interface Component {
    String getName();

    List<ResolvedProperties> resolvePropertiesForEachConfigType();

    ResolvedProperties resolvePropertiesForConfigType(ConfigTypeSupplier configTypeSupplier);
}