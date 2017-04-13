package specs.scheduler;

import com.commercetools.pspadapter.payone.config.ServiceConfig;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentStatus;
import org.apache.commons.lang3.StringUtils;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import specs.response.BasePaymentFixture;

import static java.util.Optional.ofNullable;

@RunWith(ConcordionRunner.class)
public class ScheduledJobShortFixture extends BasePaymentFixture {

    private static Logger LOG = LoggerFactory.getLogger(ScheduledJobShortFixture.class);

    public MultiValueResult createPayment(String paymentName,
                                          String paymentMethod,
                                          String transactionType,
                                          String centAmount,
                                          String currencyCode) throws Exception {
        Payment payment = createAndSavePayment(paymentName, paymentMethod, transactionType, centAmount, currencyCode);
        return MultiValueResult.multiValueResult()
                .with("id", payment.getId())
                .with("version", payment.getVersion())
                .with("paymentStatus", ofNullable(payment.getPaymentStatus()).map(PaymentStatus::getInterfaceCode).orElse(null))
                .with("interfaceInteractionsSize", payment.getInterfaceInteractions().size())
                .with("createdAt", payment.getCreatedAt())
                .with("lastModifiedAt", payment.getLastModifiedAt())
                .with("isNotModified", payment.getCreatedAt().equals(payment.getLastModifiedAt()));
    }

    /**
     * Note, this value should be corresponded (<b>not less than</b>) with service's {@link ServiceConfig#getScheduledJobCronForShortTimeFramePoll()}
     * @return short scheduled job timeout.
     */
    public int waitSecondsTimeout() {
        return 40; // we make 10 seconds overlap to let the job complete
    }


    public MultiValueResult verifyPaymentIsHandled(String paymentName) throws Exception {
        Payment payment = fetchPaymentByLegibleName(paymentName);

        // wait once, if necessary
        if (payment.getPaymentStatus() == null || StringUtils.isBlank(payment.getPaymentStatus().getInterfaceCode())) {

            // how much time to wait till the next scheduled job is definitely started
            long secondsToWait = payment.getCreatedAt()
                                        .plusSeconds(waitSecondsTimeout())
                                        .minusSeconds(System.currentTimeMillis() / 1000).toEpochSecond();

            if (secondsToWait > 0) {
                LOG.info("Wait {} seconds for scheduled job to run", secondsToWait);
                Thread.sleep(secondsToWait * 1000);
            }

            payment = fetchPaymentByLegibleName(paymentName);
        }

        return MultiValueResult.multiValueResult()
                .with("version", payment.getVersion())
                .with("paymentStatus", ofNullable(payment.getPaymentStatus()).map(PaymentStatus::getInterfaceCode).orElse(null))
                .with("interfaceInteractionsSize", payment.getInterfaceInteractions().size())
                .with("lastModifiedAt", payment.getLastModifiedAt())
                .with("hasBeenModified", payment.getCreatedAt().compareTo(payment.getLastModifiedAt()) < 0);
    }
}
