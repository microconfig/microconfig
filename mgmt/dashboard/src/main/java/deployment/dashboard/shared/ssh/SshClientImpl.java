package deployment.dashboard.shared.ssh;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import mgmt.utils.ThreadUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

import static java.lang.String.join;

@Slf4j
@Lazy
@Service
public class SshClientImpl implements SshClient {
    private final String ip;
    private final String user;

    public SshClientImpl(@Value("${ssh.ip}") String ip,
                         @Value("${ssh.user}") String user) {
        this.ip = ip;
        this.user = user;
    }

    @Override
    public void exec(OutputStream outputStream, String... args) {
        doWithSession(session -> {
            try {
                Channel channel = session.openChannel("exec");
                ChannelExec channelExec = (ChannelExec) channel;
                channelExec.setInputStream(null);
                channelExec.setOutputStream(null);
                channelExec.setErrStream(null);

                channelExec.setCommand(join(" ", args));
                channel.connect();

                read(channel, channel.getInputStream(), outputStream);
            } catch (JSchException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void doWithSession(Consumer<Session> sessionConsumer) {
        JSch jsch = new JSch();

        Session session;
        try {
            session = jsch.getSession(user, ip);
        } catch (JSchException e) {
            throw new RuntimeException();
        }

        try {
            session.setPassword(user);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            sessionConsumer.accept(session);
        } catch (JSchException e) {
            throw new RuntimeException(e);
        } finally {
            session.disconnect();
        }
    }

    private void read(Channel channel, InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(buffer, 0, buffer.length);
                if (i < 0) break;

                out.write(buffer, 0, i);
                out.flush();
            }

            if (channel.isClosed()) {
                if (in.available() > 0) continue;
                break;
            }

            ThreadUtils.sleepMs(100);
        }
    }
}