package io.microconfig.domain.impl.properties;

import io.microconfig.domain.BuildPropertiesStep;
import io.microconfig.domain.ConfigBuildResult;
import io.microconfig.domain.ConfigBuildResults;
import io.microconfig.domain.ConfigTypeFilter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class CompositeBuildPropertiesStep implements BuildPropertiesStep {
    private final List<BuildPropertiesStep> steps;

    @Override
    public ConfigBuildResults forEachConfigType() {
        return new ConfigBuildResultsImpl(
                forEachStep(BuildPropertiesStep::forEachConfigType)
        );
    }

    @Override
    public ConfigBuildResults forConfigType(ConfigTypeFilter filter) {
        return new ConfigBuildResultsImpl(
                forEachStep(step -> step.forConfigType(filter))
        );
    }

    private List<ConfigBuildResult> forEachStep(Function<BuildPropertiesStep, ConfigBuildResults> method) {
        return steps.stream()
                .map(method)
                .map(ConfigBuildResults::asList)
                .flatMap(List::stream)
                .collect(toList());
    }
}
