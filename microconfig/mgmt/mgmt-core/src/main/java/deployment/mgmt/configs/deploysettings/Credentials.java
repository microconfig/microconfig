package deployment.mgmt.configs.deploysettings;

import deployment.util.StringUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Base64;

@Getter
@RequiredArgsConstructor
public class Credentials {
    private final String user;
    private final String password;

    private final String basicAuthorization;

    public static Credentials parse(String nexusCredentials) {
        if (StringUtils.isEmpty(nexusCredentials)
                || nexusCredentials.equals(":")
                || nexusCredentials.equals("?:?")
                || nexusCredentials.equals("=:=")
        ) {
            return new Credentials(null, null, null);
        }

        String[] parts = nexusCredentials.split(":");
        return new Credentials(parts[0], parts[1], "Basic " + new String(Base64.getEncoder().encode(nexusCredentials.getBytes())));
    }

    public boolean isEmpty() {
        return user == null;
    }
}