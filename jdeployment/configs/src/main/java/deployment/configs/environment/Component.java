package deployment.configs.environment;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode(of = "name")
@RequiredArgsConstructor
public class Component {
    private final String name; //should be unique among env
    private final String type;//name of component folder

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
