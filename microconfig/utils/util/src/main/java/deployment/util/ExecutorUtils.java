package deployment.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

public class ExecutorUtils {
    public static <T> T executeInParallel(Callable<T> task, int threadCount) {
        ForkJoinPool pool = new ForkJoinPool(threadCount);
        try {
            return pool.submit(task).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            pool.shutdown();
        }
    }
}