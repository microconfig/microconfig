package io.microconfig.domain;

import java.util.List;

public interface ResolvedComponents {
    List<ResolvedComponent> asList();

    <T> T serialize(PropertySerializer<T> serializer);
}