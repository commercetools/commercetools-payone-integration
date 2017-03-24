package specs;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.System.currentTimeMillis;

/**
 * Class to wait some time while some condition is <b>true</b>
 */
public class NotificationTimeoutWaiter {
    private final long timeoutDuration;
    private final long retryDelay;

    /**
     * If greater then 0 and {@code intermediateReporter} is set in {@link #start(Supplier, Function, Consumer)} -
     * call consumer every {@code intermediateReportDelay} msec during execution.
     */
    private final long intermediateReportDelay;

    private long lastDuration = -1;
    private long currentDuration = -1;

    /**
     * @param timeoutDurationMsec total time to wait
     * @param retryDelayMsec delay after which to retry
     */
    public NotificationTimeoutWaiter(long timeoutDurationMsec, long retryDelayMsec) {
        this.timeoutDuration = timeoutDurationMsec;
        this.retryDelay = retryDelayMsec;
        this.intermediateReportDelay = -1;
    }

    /**
     *
     * @param timeoutDurationMsec total time to wait
     * @param retryDelayMsec delay after which to retry
     * @param intermediateReportDelayMsec make intermediate reports every period of the time
     */
    public NotificationTimeoutWaiter(long timeoutDurationMsec, long retryDelayMsec, long intermediateReportDelayMsec) {
        this.timeoutDuration = timeoutDurationMsec;
        this.retryDelay = retryDelayMsec;
        this.intermediateReportDelay = intermediateReportDelayMsec;
    }

    /**
     * Block current thread until {@code exitCondition} is <b>true</b>, but not longer than {@link #timeoutDuration}
     * @param executeAction action to perform every time (at the beginning and after every {@link #retryDelay}
     * @param exitCondition if returns <b>true</b> - exit the function. The function accepts the last returned value
     *                      from {@code executeAction}
     * @param intermediateReporter consumer which accepts the last obtained result from {@code executeAction} and
     *                             prints makes some intermediate job every {@link #intermediateReportDelay} time.
     * @param <T> type of the value {@code executeAction} returns.
     * @return the last value from {@code executeAction}
     * @throws InterruptedException
     */
    public <T> T start(Supplier<T> executeAction, Function<T, Boolean> exitCondition,
                       @Nullable Consumer<T> intermediateReporter) throws InterruptedException {
        final long startTime = currentTimeMillis();
        lastDuration = -1;
        currentDuration = -1;

        long lastIntermediateReportTime = startTime;

        T res;
        while (true) {
            res = executeAction.get();
            if (exitCondition.apply(res)) {
                break;
            }

            long currentTime = currentTimeMillis();

            if (intermediateReportDelay > 0 && (currentTime - lastIntermediateReportTime) >= intermediateReportDelay
                    && intermediateReporter != null) {
                lastIntermediateReportTime = currentTime;
                intermediateReporter.accept(res);
            }

            Thread.sleep(retryDelay);

            currentDuration = currentTimeMillis() - startTime;

            if(currentDuration > timeoutDuration) {
                break;
            }
        }

        lastDuration = currentDuration;

        return res;
    }

    public <T> T start(Supplier<T> executeAction, Function<T, Boolean> exitCondition) throws InterruptedException {
        return start(executeAction, exitCondition, null);
    }

    public long getTimeoutDuration() {
        return timeoutDuration;
    }

    /**
     * Complete duration after {@code #start()} method completed. Before start and during execution the value is -1.
     * After complete this value is equal to {@link #currentDuration}.
     *
     * The value might be a bit bigger then {@link #timeoutDuration}.
     *
     * @return the time duration (in msec) last {@link #start(Supplier, Function)} call took.
     */
    public long getLastDuration() {
        return lastDuration;
    }

    /**
     * Current duration during {@code #start()} method execution. Before start the value is -1, during execution the
     * value grows to up to {@link #timeoutDuration}. After completion the value equals to {@link #lastDuration}.
     * @return the time current execution is lasting.
     */
    public long getCurrentDuration() {
        return currentDuration;
    }
}
