package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.exceptions.NoCartLikeFoundException;
import io.sphere.sdk.payments.Payment;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ConcurrentModificationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.commercetools.pspadapter.tenant.TenantLoggerUtil.createTenantKeyValue;
import static java.lang.String.format;

/**
 * @author fhaertig
 * @since 07.12.15
 */
public abstract class ScheduledJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledJob.class);

    static final String INTEGRATION_SERVICE = "INTEGRATION_SERVICE";

    @Override
    public void execute(final JobExecutionContext context) {

        final JobDataMap dataMap = context.getMergedJobDataMap();

        final IntegrationService integrationService = (IntegrationService) dataMap.get(INTEGRATION_SERVICE);
        final ZonedDateTime sinceDate = getSinceDateTime();

        integrationService.getTenantFactories().forEach(tenantFactory -> {
            final CommercetoolsQueryExecutor queryExecutor = tenantFactory.getCommercetoolsQueryExecutor();
            final PaymentDispatcher paymentDispatcher = tenantFactory.getPaymentDispatcher();


            final Consumer<Payment> paymentConsumer = payment -> {
                try {
                    final PaymentWithCartLike paymentWithCartLike = queryExecutor
                        .getPaymentWithCartLike(payment.getId(), CompletableFuture.completedFuture(payment));
                    paymentDispatcher.dispatchPayment(paymentWithCartLike);
                } catch (final NoCartLikeFoundException | ConcurrentModificationException ex) {
                    /**
                     * Both exceptions are valid cases which are not considered errors (thus logged in debug).
                     * NoCartLikeFoundException could happen if the payment was made through a different point of sale
                     * (i.e. there is no cart) but with the same payone account.
                     *
                     * ConcurrentModificationException could happen if the job tries to process a payment which is being
                     * processed by the /handlePayment endpoint.
                     */
                    LOG.debug(
                        createTenantKeyValue(tenantFactory.getTenantName()),
                        format("Error dispatching payment with id '%s'", payment.getId()),
                        ex);
                } catch (final Exception ex) {
                    LOG.error(
                        createTenantKeyValue(tenantFactory.getTenantName()),
                        format("Error dispatching payment with id '%s'", payment.getId()),
                        ex);
                }
            };

            queryExecutor.consumePaymentCreatedMessages(sinceDate, paymentConsumer);
            queryExecutor.consumePaymentTransactionAddedMessages(sinceDate, paymentConsumer);
        });
    }

    protected abstract ZonedDateTime getSinceDateTime();
}
