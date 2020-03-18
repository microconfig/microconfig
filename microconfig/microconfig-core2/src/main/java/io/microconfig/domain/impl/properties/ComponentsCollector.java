package io.microconfig.domain.impl.properties;

import io.microconfig.domain.Component;
import io.microconfig.domain.Components;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.stream.Collector.Characteristics.UNORDERED;

public class ComponentsCollector implements Collector<Component, List<Component>, Components> {

    public static ComponentsCollector toComponents() {
        return new ComponentsCollector();
    };

    @Override
    public Supplier<List<Component>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<Component>, Component> accumulator() {
        return List::add;
    }

    @Override
    public BinaryOperator<List<Component>> combiner() {
        return (x, y) -> {
            x.addAll(y);
            return x;
        };
    }

    @Override
    public Function<List<Component>, Components> finisher() {
        return ComponentsImpl::new;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(UNORDERED);
    }
}
