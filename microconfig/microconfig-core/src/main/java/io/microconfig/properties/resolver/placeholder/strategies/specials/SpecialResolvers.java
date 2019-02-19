package io.microconfig.properties.resolver.placeholder.strategies.specials;

import io.microconfig.environments.ComponentGroup;
import io.microconfig.properties.resolver.placeholder.strategies.SpecialResolverStrategy.SpecialKey;

import java.io.File;
import java.util.Map;

import static io.microconfig.utils.FileUtils.userHomeString;
import static io.microconfig.utils.StringUtils.unixLikePath;
import static java.util.Optional.of;

public class SpecialResolvers {
    public static SpecialKey portOffset(Map<String, SpecialKey> registrator) {
        SpecialKey specialKey = (SpecialKey) (component, environment) -> environment.getPortOffset().map(Object::toString);
        return specialKey;
    }

    public static SpecialKey ip() {
        return (component, environment) -> environment.getGroupByComponentName(component.getName()).flatMap(ComponentGroup::getIp);
    }

    public static SpecialKey group() {
        return (component, environment) -> environment.getGroupByComponentName(component.getName()).map(ComponentGroup::getName);
    }

    public static SpecialKey order() {
        return (component, environment) -> environment.getGroupByComponentName(component.getName())
                .map(cg -> "" + 1 + cg.getComponentNames().indexOf(component.getName()));
    }

    public static SpecialKey name() {
        return (component, environment) -> of(component.getName());
    }

    public static SpecialKey env() {
        return (component, environment) -> of(environment.getName());
    }

    public static SpecialKey folder() {
        return (component, environment) -> {
            //                return componentTree.getFolder(component.getType())
//                        .map(file -> file.getAbsolutePath())
//                        .orElse(null);
            return of("folder");
        };
    }

    public static SpecialKey userHome() {
        return (component, environment) -> of(unixLikePath(userHomeString()));
    }

    public static SpecialKey configDir() {
        return (component, environment) -> {
            return of(new File("destinationComponentDir", component.getName()).getAbsolutePath()); //todo
        };
    }

    public static SpecialKey serviceDir() {
        return (component, environment) -> {
            return of(new File("destinationComponentDir", component.getName()).getAbsolutePath()); //todo
        };
    }
}