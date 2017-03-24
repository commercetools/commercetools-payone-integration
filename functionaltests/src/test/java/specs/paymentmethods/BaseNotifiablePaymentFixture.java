package specs.paymentmethods;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import specs.NotificationTimeoutWaiter;
import specs.response.BasePaymentFixture;

import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.joining;

public class BaseNotifiablePaymentFixture extends BasePaymentFixture {

    private final Logger LOG;

    public BaseNotifiablePaymentFixture() {
        this.LOG = LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Check the payments are created successfully and wait for theirs update notifications from Payone.
     * @param paymentNames payments to check
     * @param txaction action name to wait
     * @return <b>true</b> if waited successfully for all notifications from {@code paymentNames} list
     */
    public boolean receivedNotificationOfActionFor(final String paymentNames, final String txaction) throws Exception {
        final ImmutableList<String> paymentNamesList = ImmutableList.copyOf(thePaymentNamesSplitter.split(paymentNames));
        String paymentIds = paymentNamesList.stream().map(this::getIdForLegibleName).collect(joining(", "));

        validatePaymentsNotFailed(paymentNamesList);

        final long timeout = PAYONE_NOTIFICATION_TIMEOUT;

        final String simpleClassName = getClass().getSimpleName();
        final int notificationsToWait = paymentNamesList.size();
        LOG.info("Start waiting {} seconds for {} notifications of action [{}] for payments [{}] in [{}] test:",
                msecToSec(timeout), notificationsToWait, txaction,
                paymentIds,
                simpleClassName);

        NotificationTimeoutWaiter timer = new NotificationTimeoutWaiter(timeout, RETRY_DELAY, INTERMEDIATE_REPORT_DELAY);

        Long numberOfPaymentsWithAppointedNotification = timer.start(
                () -> countPaymentsWithNotificationOfAction(paymentNamesList, txaction),
                num -> num == notificationsToWait,
                num -> LOG.info("Intermediate report: waited {}/{} sec, received {}/{} notifications in test [{}]",
                        msecToSec(timer.getCurrentDuration()), msecToSec(timer.getTimeoutDuration()),
                        num, notificationsToWait,
                        simpleClassName));

        boolean success = numberOfPaymentsWithAppointedNotification == notificationsToWait;


        String logMessage = String.format("waited %d seconds to receive %d/%d notifications of action [%s] for payments %s in %s test",
                msecToSec(timer.getLastDuration()),
                numberOfPaymentsWithAppointedNotification, notificationsToWait,
                txaction,
                paymentIds,
                simpleClassName);

        if (success) {
            LOG.info("Successfully " + logMessage);
        } else {
            LOG.error("Failure " + logMessage);
        }

        return success;
    }

    private static long msecToSec(long msec) {
        return TimeUnit.MILLISECONDS.toSeconds(msec);
    }

}
