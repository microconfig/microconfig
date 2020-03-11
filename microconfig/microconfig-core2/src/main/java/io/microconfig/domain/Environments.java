package io.microconfig.domain;


import java.util.List;
import java.util.Set;

public interface Environments {
    List<Environment> all();

    Set<String> environmentNames();

    Environment byName(String name);
}