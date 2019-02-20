package deployment.mgmt.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static deployment.mgmt.utils.EnvResolver.resolveEnvVariable;
import static java.io.File.pathSeparator;
import static java.util.Map.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnvResolverTest {
    @Test
    public void testResolveEnvVariable() {
        Map<String, String> actual = resolveEnvVariable(of("ERL_DIR", "/home/rpbin/erlang/bin",
                "LD_LIBRARY_PATH", "$LD_LIBRARY_PATH:/home/rpbin/numerix15.0.1",
                "NX_LICENSE_DIR", "/home/rpbin/numerix/license",
                "PATH", "$PATH:/home/rpbin/numerix15.0.1:/home/rpbin/mongodb:/home/rpbin/erlang/bin")
        );

        assertEquals(of("ERL_DIR", "/home/rpbin/erlang/bin",
                "LD_LIBRARY_PATH", ";/home/rpbin/numerix15.0.1".replace(";", pathSeparator),
                "NX_LICENSE_DIR", "/home/rpbin/numerix/license",
                "PATH", System.getenv("PATH") + ";/home/rpbin/numerix15.0.1;/home/rpbin/mongodb;/home/rpbin/erlang/bin".replace(";", pathSeparator)
                ), actual
        );
    }
}