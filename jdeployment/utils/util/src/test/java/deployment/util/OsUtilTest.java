package deployment.util;

import org.junit.Test;

import java.util.Map;

import static deployment.util.OsUtil.resolveEnvVariable;
import static java.util.Map.of;
import static org.junit.Assert.assertEquals;

public class OsUtilTest {
    @Test
    public void testResolveEnvVariable() {
        Map<String, String> actual = resolveEnvVariable(of("ERL_DIR", "/home/rpbin/erlang/bin",
                "LD_LIBRARY_PATH", "$LD_LIBRARY_PATH:/home/rpbin/numerix15.0.1",
                "NX_LICENSE_DIR", "/home/rpbin/numerix/license",
                "PATH", "$PATH:/home/rpbin/numerix15.0.1:/home/rpbin/mongodb:/home/rpbin/erlang/bin")
        );

        assertEquals(of("ERL_DIR", "/home/rpbin/erlang/bin",
                "LD_LIBRARY_PATH", ";/home/rpbin/numerix15.0.1",
                "NX_LICENSE_DIR", "/home/rpbin/numerix/license",
                "PATH", System.getenv("PATH") + ";/home/rpbin/numerix15.0.1;/home/rpbin/mongodb;/home/rpbin/erlang/bin")
                , actual
        );
    }
}