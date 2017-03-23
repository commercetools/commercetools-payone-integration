package specs.paymentmethods;

import com.google.common.collect.ImmutableList;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.PaymentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import specs.NotificationTimeoutWaiter;
import specs.response.BasePaymentFixture;

import java.util.concurrent.TimeUnit;

import static com.commercetools.pspadapter.payone.util.CompletionUtil.executeBlocking;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
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
                num -> LOG.info("Intermediate report: waited {}/{} sec, received {}/{} notifications of action [{}] in test [{}]",
                        msecToSec(timer.getCurrentDuration()), msecToSec(timer.getTimeoutDuration()),
                        num, notificationsToWait,
                        txaction,
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

    public String fetchOrderPaymentState(String paymentId) {
        Order order = executeBlocking(orderService.getOrderByPaymentId(paymentId))
                .orElseThrow(() -> new RuntimeException(format("Order for payment [%s] not found", paymentId)));

        return ofNullable(order.getPaymentState()).map(PaymentState::toSphereName).orElse(null);
    }

}
