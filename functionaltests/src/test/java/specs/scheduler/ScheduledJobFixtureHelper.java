package specs.scheduler;

import com.commercetools.pspadapter.payone.config.ServiceConfig;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentStatus;
import org.apache.commons.lang3.StringUtils;
import org.concordion.api.MultiValueResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import specs.response.BasePaymentFixture;

import static java.util.Optional.ofNullable;

/**
 * Helper class to test scheduled jobs on both tenants.
 * <p>
 * The tenants are represented and {@link #paymentFixture} instance, implementing respectively the first (base) or the
 * second tenant {@link specs.multitenancy.BaseTenant2Fixture}.
 */
public class ScheduledJobFixtureHelper {

    private static Logger LOG = LoggerFactory.getLogger(ScheduledJobShortFixture.class);

    private final BasePaymentFixture paymentFixture;

    public ScheduledJobFixtureHelper(BasePaymentFixture paymentFixture) {
        this.paymentFixture = paymentFixture;
    }

    /**
     * Create a payment, but don't handle it
     * @param paymentName      test payment alias
     * @param paymentMethod    payone payment method name
     * @param transactionType  payone payment transaction type
     * @param centAmount       payment amount
     * @param currencyCode     payment currency
     * @return map of created payment properties
     * @throws Exception any exceptions in the tests
     */
    public MultiValueResult createPayment(String paymentName,
                                                 String paymentMethod,
                                                 String transactionType,
                                                 String centAmount,
                                                 String currencyCode) throws Exception {

        Payment payment = paymentFixture.createAndSavePayment(paymentName, paymentMethod, transactionType, centAmount, currencyCode);

        return MultiValueResult.multiValueResult()
                .with("id", payment.getId())
                .with("version", payment.getVersion())
                .with("paymentStatus", ofNullable(payment.getPaymentStatus()).map(PaymentStatus::getInterfaceCode).orElse(null))
                .with("interfaceInteractionsSize", payment.getInterfaceInteractions().size())
                .with("transactionsSize", payment.getTransactions().size())
                .with("createdAt", payment.getCreatedAt())
                .with("lastModifiedAt", payment.getLastModifiedAt())
                .with("isNotModified", payment.getCreatedAt().equals(payment.getLastModifiedAt()));
    }

    /**
     * Verify the {@code paymentName} is automatically handled by (short) scheduled task.
     * @param paymentName payment alias
     * @return map of the payment properties after waiting scheduled task.
     * @throws Exception any exceptions in the tests
     */
    public MultiValueResult verifyPaymentIsHandled(String paymentName) throws Exception {
        Payment payment = paymentFixture.fetchPaymentByLegibleName(paymentName);

        // wait once, if necessary
        if (payment.getPaymentStatus() == null || StringUtils.isBlank(payment.getPaymentStatus().getInterfaceCode())) {

            // how much time to wait till the next scheduled job is definitely started
            long secondsToWait = payment.getCreatedAt()
                    .plusSeconds(waitSecondsTimeout())
                    .minusSeconds(System.currentTimeMillis() / 1000).toEpochSecond();

            if (secondsToWait > 0) {
                LOG.info("Wait up to {} seconds for scheduled job to run", secondsToWait);
                Thread.sleep(secondsToWait * 1000);
            }

            payment = paymentFixture.fetchPaymentByLegibleName(paymentName);
        }

        return MultiValueResult.multiValueResult()
                .with("version", payment.getVersion())
                .with("paymentStatus", ofNullable(payment.getPaymentStatus()).map(PaymentStatus::getInterfaceCode).orElse(null))
                .with("interfaceInteractionsSize", payment.getInterfaceInteractions().size())
                .with("transactionsSize", payment.getTransactions().size())
                .with("lastModifiedAt", payment.getLastModifiedAt())
                .with("hasBeenModified", payment.getCreatedAt().compareTo(payment.getLastModifiedAt()) < 0);
    }

    /**
     * A maximum duration we wait for a payment is handled by (short) scheduled task.
     * <p>
     * <b>Note:</b><ul>
     *     <li>this test is depended on actual service's {@link ServiceConfig#getScheduledJobCronForShortTimeFramePoll()}
     * value, which is configured by {@code SHORT_TIME_FRAME_SCHEDULED_JOB_CRON} environment variable
     * (or fallback to default one). We expect the short time-frame is "every 30 seconds" (<b>{@code 0/30 * * * * ? *}</b>.
     * Adjust this value if you change short time-frame schedule in the Heroku service.</li>
     *     <li>We added 10 second gap to let the job complete if any lags occurred.</li>
     * </ul>
     *
     * @return 40 (seconds)
     */
    public static int waitSecondsTimeout() {
        return 40;
    }

}
