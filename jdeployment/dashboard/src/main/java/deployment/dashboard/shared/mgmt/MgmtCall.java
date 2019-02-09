package deployment.dashboard.shared.mgmt;

import java.io.OutputStream;

public interface MgmtCall {
    String executeLocally(String command);

    String executeRemotely(String group, String command);

    void executeRemotely(String group, String command, OutputStream outputStream);
}