package io.microconfig.domain;

import java.util.List;

public interface ResolvedComponents {
    List<ResolvedComponent> asList();

    ResolvedComponent first();

    <T> T serialize(PropertySerializer<T> serializer);
}