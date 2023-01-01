package network.parthenon.amcdb.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class IntervalRunnable implements Runnable {

    /**
     * Executor used to schedule task run.
     */
    protected ScheduledExecutorService executorService;

    /**
     * Name of the thread on which the executor will run.
     */
    protected final String threadName;

    /**
     * Creates a new IntervalRunnable with the specified thread name.
     * @param threadName
     */
    protected IntervalRunnable(String threadName) {
        this.threadName = threadName;
    }

    /**
     * Schedules operation at the specified interval.
     * @param intervalMillis Interval at which to run
     * @return ScheduledExecutorService managing the schedule
     */
    public ScheduledExecutorService start(long intervalMillis) {
        if(executorService != null) {
            throw new IllegalStateException("IntervalRunnable is already started!");
        }

        executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName(threadName);
            thread.setDaemon(true);
            return thread;
        });

        executorService.scheduleWithFixedDelay(this, 0, intervalMillis, TimeUnit.MILLISECONDS);

        return executorService;
    }
}
