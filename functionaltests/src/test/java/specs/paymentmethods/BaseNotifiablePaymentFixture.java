package specs.paymentmethods;

import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.payments.Payment;
import org.concordion.api.MultiValueResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import specs.NotificationTimeoutWaiter;
import specs.response.BasePaymentFixture;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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
        final List<String> paymentNamesList = thePaymentNamesSplitter.splitToList(paymentNames);
        final String paymentIds = paymentNamesList.stream().map(this::getIdForLegibleName).collect(joining(", "));

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

    /**
     * Used to wait for second notification (paid) if the previous one (appointed) has been successful.
     * @param paymentNames
     * @param txaction current txAction to wait (paid)
     * @param prevTxaction previous txAction which must be successful
     * @return <b>true</b> if previous and current txactions completed successfully.
     */
    public boolean receivedNextNotificationOfActionFor(final String paymentNames, final String txaction,
                                                          final String prevTxaction) throws Exception {
        final List<String> paymentNamesList = thePaymentNamesSplitter.splitToList(paymentNames);
        long prevTxactionCount = countPaymentsWithNotificationOfAction(paymentNamesList, prevTxaction);
        boolean preTxactionIsSuccess = prevTxactionCount == paymentNamesList.size();

        if (!preTxactionIsSuccess) {
            LOG.warn("Previous txAction [{}] was not success: expected {}, but found {}. Waiting for [{}] action is skipped!",
                    prevTxaction, paymentNamesList.size(), prevTxactionCount, txaction);
            return false;
        }

        return receivedNotificationOfActionFor(paymentNames, txaction);
    }

    private static long msecToSec(long msec) {
        return TimeUnit.MILLISECONDS.toSeconds(msec);
    }

    public String fetchOrderPaymentState(String paymentId) {
        Order order = executeBlocking(orderService.getOrderByPaymentId(paymentId))
                .orElseThrow(() -> new RuntimeException(format("Order for payment [%s] not found", paymentId)));

        return ofNullable(order.getPaymentState()).map(PaymentState::toSphereName).orElse(null);
    }

    protected Map<String, String> fetchCreditCardPaymentDetails(final String paymentName) throws InterruptedException, ExecutionException {

        final Payment payment = fetchPaymentByLegibleName(paymentName);

        final long appointedNotificationCount = getTotalNotificationCountOfAction(payment, "appointed");
        final long paidNotificationCount = getTotalNotificationCountOfAction(payment, "paid");

        final String transactionId = getIdOfLastTransaction(payment);

        return ImmutableMap.<String, String> builder()
                .put("appointedNotificationCount", Long.toString(appointedNotificationCount))
                .put("paidNotificationCount", Long.toString(paidNotificationCount))
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("version", payment.getVersion().toString())
                .build();
    }

    public MultiValueResult fetchBasicPaymentDetails(final String paymentName, final String txaction) throws ExecutionException {

        Payment payment = fetchPaymentByLegibleName(paymentName);
        long appointedNotificationCount = getTotalNotificationCountOfAction(payment, txaction);

        final String transactionId = getIdOfLastTransaction(payment);
        return MultiValueResult.multiValueResult()
                .with("notificationCount", Long.toString(appointedNotificationCount))
                .with("transactionState", getTransactionState(payment, transactionId))
                .with("version", payment.getVersion().toString());
    }

    public long getInteractionNotificationOfActionCount(final String paymentName, final String txaction) throws ExecutionException {
        Payment payment = fetchPaymentByLegibleName(paymentName);
        return getTotalNotificationCountOfAction(payment, txaction);
    }

}
