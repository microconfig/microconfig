package io.microconfig.domain;


import java.util.List;
import java.util.Set;

public interface Environments {
    List<Environment> all();

    Environment byName(String name);

    Set<String> environmentNames();
}