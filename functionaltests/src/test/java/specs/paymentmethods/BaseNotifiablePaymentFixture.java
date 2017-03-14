package specs.paymentmethods;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import specs.NotificationTimeoutWaiter;
import specs.response.BasePaymentFixture;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

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

        validatePaymentsNotFailed(paymentNamesList);

        final long timeout = PAYONE_NOTIFICATION_TIMEOUT;

        LOG.info("Start waiting {} seconds for {} notifications in {} test:",
                TimeUnit.MILLISECONDS.toSeconds(timeout), paymentNamesList.size(), getClass().getSimpleName());

        NotificationTimeoutWaiter timer = new NotificationTimeoutWaiter(timeout, RETRY_DELAY);

        Long numberOfPaymentsWithAppointedNotification = timer.start(
                () -> countPaymentsWithNotificationOfAction(paymentNamesList, txaction),
                num -> num == paymentNamesList.size());

        boolean success = numberOfPaymentsWithAppointedNotification == paymentNamesList.size();

        String logMessage = String.format("waited %d seconds to receive notifications for payments %s in %s test",
                TimeUnit.MILLISECONDS.toSeconds(timer.getLastDuration()),
                Arrays.toString(paymentNamesList.stream().map(this::getIdForLegibleName).toArray()),
                getClass().getSimpleName());

        if (success) {
            LOG.info("Successfully " + logMessage);
        } else {
            LOG.error("Failure " + logMessage);
        }

        return success;
    }

}
