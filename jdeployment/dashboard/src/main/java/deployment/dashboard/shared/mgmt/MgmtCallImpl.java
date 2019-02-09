package deployment.dashboard.shared.mgmt;

import deployment.dashboard.shared.ssh.SshClient;
import deployment.util.Logger;
import deployment.util.LoggerUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static deployment.util.Logger.align;
import static deployment.util.OsUtil.isWindows;
import static deployment.util.ProcessUtil.executeAndReadOutput;

@Service
@RequiredArgsConstructor
public class MgmtCallImpl implements MgmtCall {
    private final SshClient sshClient;

    @Override
    public String executeLocally(String command) {
        return call("mgmt", command);
    }

    @Override
    public String executeRemotely(String group, String command) {
        return call(remoteCommand(group, command));
    }

    @Override
    public void executeRemotely(String group, String command, OutputStream outputStream) {
        flushBigEmptyText(outputStream);
        call(outputStream, remoteCommand(group, command));
    }

    private String[] remoteCommand(String group, String command) {
        return new String[]{"mgmt", "executeRemotely", group, "mgmt", command};
    }

    private String call(String... command) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        call(stream, command);
        return stream.toString();
    }

    private void call(OutputStream outputStream, String... command) {
        if (isWindows()) {
            sshClient.exec(outputStream, command);
        } else {
            executeAndReadOutput(outputStream, command);
        }
    }

    //hack to force flushing on ui
    private void flushBigEmptyText(OutputStream outputStream) {
        try {
            outputStream.write((align("", 1000) + "\n").getBytes());
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}