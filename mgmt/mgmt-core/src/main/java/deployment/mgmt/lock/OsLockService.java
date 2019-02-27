package deployment.mgmt.lock;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import static io.microconfig.utils.ConsoleColor.yellow;
import static io.microconfig.utils.IoUtils.firstLineOrEmpty;
import static io.microconfig.utils.Logger.*;
import static io.microconfig.utils.TimeUtils.secAfter;
import static java.lang.ProcessHandle.current;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.nio.ByteBuffer.wrap;
import static mgmt.utils.LoggerUtils.oneLineInfo;
import static mgmt.utils.ThreadUtils.sleepSec;

@RequiredArgsConstructor
public class OsLockService implements LockService {
    private final DeployFileStructure deployFileStructure;
    private volatile FileLock currentLock;

    @Override
    public void lockAndExecute(Runnable task) {
        try {
            currentLock = lock(deployFileStructure.deploy().getLockFile());
            writeCurrentPid();
            task.run();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            unlock();
        }
    }

    @Override
    public synchronized void unlock() {
        try {
            FileLock lock = this.currentLock;
            if (lock == null) return;
            if (lock.channel().isOpen()) {
                lock.release();
            }
            this.currentLock = null;
        } catch (IOException e) {
            throw new RuntimeException("Exception during mgmt lock release");
        }
    }

    private FileLock lock(File file) throws IOException {
        FileChannel channel = new RandomAccessFile(file, "rwd").getChannel();
        FileLock lock = channel.tryLock();
        if (lock != null) return lock;

        warn("Another instance of mgmt has been running for " + secAfter(file.lastModified()) + "...");
        warn("You can kill it manually by pid or with 'mgmt kill-all-java'.");

        long waitStartTime = currentTimeMillis();
        while (true) {
            oneLineInfo(yellow("Waiting completion of mgmt process with pid " + firstLineOrEmpty(file) + "... " + secAfter(waitStartTime)));
            sleepSec(1);

            if ((lock = channel.tryLock()) != null) {
                logLineBreak();
                return lock;
            }
        }
    }

    private synchronized void writeCurrentPid() throws IOException {
        FileLock lock = this.currentLock;
        if (lock == null) return;
        lock.channel()
                .position(0)
                .write(wrap(align(valueOf(current().pid()), 20).getBytes()));
    }
}