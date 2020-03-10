package io.microconfig.domain.impl.environment;

import io.microconfig.domain.BuildPropertiesStep;
import io.microconfig.domain.ConfigTypeFilter;
import io.microconfig.domain.ResultComponents;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class CompositeBuildPropertiesStep implements BuildPropertiesStep {
    private final List<BuildPropertiesStep> steps;

    @Override
    public ResultComponents forEachConfigType() {
        return null;
    }

    @Override
    public ResultComponents forConfigType(ConfigTypeFilter filter) {
        return null;
    }
}
