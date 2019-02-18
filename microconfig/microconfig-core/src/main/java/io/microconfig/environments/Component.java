package io.microconfig.environments;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode(of = "name")
@RequiredArgsConstructor
public class Component {
    private final String name; //alias, must be unique among env
    private final String type;//folder name

    public static Component byNameAndType(String name, String type) {
        return new Component(name, type);
    }

    public static Component byType(String type) {
        return new Component(type, type);
    }

    @Override
    public String toString() {
        return name + ":" + type;
    }
}
