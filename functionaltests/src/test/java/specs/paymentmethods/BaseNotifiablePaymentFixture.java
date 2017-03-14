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
     * @param paymentNames
     * @param txaction action name to wait
     * @return <b>true</b> if waited successfully for all notifications from {@code paymentNames} list
     * @throws Exception
     */
    public boolean receivedNotificationOfActionFor(final String paymentNames, final String txaction) throws Exception {
        final ImmutableList<String> paymentNamesList = ImmutableList.copyOf(thePaymentNamesSplitter.split(paymentNames));

        validatePaymentsAreSuccess(paymentNamesList);

        LOG.info("Start waiting {} seconds for {} notifications in {} test:",
                TimeUnit.MILLISECONDS.toSeconds(PAYONE_NOTIFICATION_TIMEOUT), paymentNamesList.size(), getClass().getSimpleName());

        NotificationTimeoutWaiter timer = new NotificationTimeoutWaiter(PAYONE_NOTIFICATION_TIMEOUT, RETRY_DELAY);

        Long numberOfPaymentsWithAppointedNotification = timer.start(
                () -> countPaymentsWithNotificationOfAction(paymentNamesList, txaction),
                num -> num == paymentNamesList.size());

        LOG.info("waited {} seconds to receive notifications for payments {} in {} test",
                TimeUnit.MILLISECONDS.toSeconds(timer.getLastDuration()),
                Arrays.toString(paymentNamesList.stream().map(this::getIdForLegibleName).toArray()),
                this.getClass().getSimpleName());

        return numberOfPaymentsWithAppointedNotification == paymentNamesList.size();
    }

}
