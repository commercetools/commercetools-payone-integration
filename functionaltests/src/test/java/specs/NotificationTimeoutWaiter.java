package specs;

import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.System.currentTimeMillis;

/**
 * Class to wait some time while some condition is <b>true</b>
 */
public class NotificationTimeoutWaiter {
    private final long timeoutDuration;
    private final long retryDelay;

    private long lastDuration = -1;

    /**
     * @param timeoutDurationMsec total time to wait
     * @param retryDelayMsec delay after which to retry
     */
    public NotificationTimeoutWaiter(long timeoutDurationMsec, long retryDelayMsec) {
        this.timeoutDuration = timeoutDurationMsec;
        this.retryDelay = retryDelayMsec;
    }

    /**
     * Block current thread until {@code exitCondition} is <b>true</b>, but not longer than {@link #timeoutDuration}
     * @param executeAction action to perform every time (at the beginning and after every {@link #retryDelay}
     * @param exitCondition if returns <b>true</b> - exit the function. The function accepts the last returned value
     *                      from {@code executeAction}
     * @param <T> type of the value {@code executeAction} returns.
     * @return the last value from {@code executeAction}
     * @throws InterruptedException
     */
    public <T> T start(Supplier<T> executeAction, Function<T, Boolean> exitCondition) throws InterruptedException {
        final long timeoutPoint = currentTimeMillis() + timeoutDuration;
        long currentTime = currentTimeMillis();
        lastDuration = -1;

        T res;
        while (true) {
            res = executeAction.get();
            if (exitCondition.apply(res)) {
                break;
            }

            Thread.sleep(retryDelay);

            if((currentTime = currentTimeMillis()) > timeoutPoint) {
                break;
            }
        }

        lastDuration = timeoutDuration - (timeoutPoint - currentTime);

        return res;
    }

    /**
     * @return the time duration (in msec) last {@link #start(Supplier, Function)} call took.
     */
    public long getLastDuration() {
        return lastDuration;
    }
}
