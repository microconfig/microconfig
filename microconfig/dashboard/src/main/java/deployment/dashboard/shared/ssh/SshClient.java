package deployment.dashboard.shared.ssh;

import java.io.OutputStream;

public interface SshClient {
    void exec(OutputStream outputStream, String... args);
}