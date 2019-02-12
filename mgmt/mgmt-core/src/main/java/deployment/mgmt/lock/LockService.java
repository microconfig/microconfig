package deployment.mgmt.lock;

public interface LockService {
    void lockAndExecute(Runnable task);

    void unlock();
}
