package io.microconfig.domain.impl.properties;

import io.microconfig.domain.BuildPropertiesStep;
import io.microconfig.domain.ConfigTypeFilter;
import io.microconfig.domain.ResultComponent;
import io.microconfig.domain.ResultComponents;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class CompositeBuildPropertiesStep implements BuildPropertiesStep {
    private final List<BuildPropertiesStep> steps;

    @Override
    public ResultComponents forEachConfigType() {
        return new ResultComponentsImpl(
                forEachStep(BuildPropertiesStep::forEachConfigType)
        );
    }

    @Override
    public ResultComponents forConfigType(ConfigTypeFilter filter) {
        return new ResultComponentsImpl(
                forEachStep(step -> step.forConfigType(filter))
        );
    }

    private List<ResultComponent> forEachStep(Function<BuildPropertiesStep, ResultComponents> method) {
        return steps.stream()
                .map(method)
                .map(ResultComponents::asList)
                .flatMap(List::stream)
                .collect(toList());
    }
}
