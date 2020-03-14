package io.microconfig;

import lombok.var;

import java.io.File;
import java.util.List;

import static io.microconfig.MicroconfigFactory.searchConfigsIn;
import static io.microconfig.domain.impl.helpers.ConfigTypeFilters.eachConfigType;
import static io.microconfig.domain.impl.helpers.PropertySerializers.toFileIn;

/**
 * Command line params example: *
 * -r configs/repo -d build -e dev
 * <p>
 * VM speedup params:
 * -Xverify:none -XX:TieredStopAtLevel=1
 */
//todo update documentation
public class MicroconfigMain {
    public static void main(String[] args) {
        var params = MicroconfigParams.parse(args);

        File rootDir = params.rootDir();
        File destinationDir = params.destinationDir();
        String env = params.env();
        List<String> groups = params.groups();
        List<String> services = params.services();

        var configs = searchConfigsIn(rootDir);
        configs.inEnvironment(env).findComponentsFrom(groups, services)
                .getPropertiesFor(eachConfigType())
                .resolveBy(configs.resolver())
                .save(toFileIn(destinationDir));
    }
}