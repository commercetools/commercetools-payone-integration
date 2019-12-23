package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.exceptions.NoCartLikeFoundException;
import io.sphere.sdk.payments.Payment;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ConcurrentModificationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.commercetools.pspadapter.tenant.TenantLoggerUtil.LOG_PREFIX;
import static com.commercetools.util.CorrelationIdUtil.generateUniqueCorrelationId;

/**
 * @author fhaertig
 * @since 07.12.15
 */
public abstract class ScheduledJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledJob.class);

    public static final String INTEGRATION_SERVICE = "INTEGRATION_SERVICE";

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        final String correlationId = generateUniqueCorrelationId();
        JobDataMap dataMap = context.getMergedJobDataMap();

        final IntegrationService integrationService = (IntegrationService) dataMap.get(INTEGRATION_SERVICE);

        final ZonedDateTime sinceDate = getSinceDateTime();

        integrationService.getTenantFactories().forEach(tenantFactory -> {
            final CommercetoolsQueryExecutor queryExecutor = tenantFactory.getCommercetoolsQueryExecutor();
            final PaymentDispatcher paymentDispatcher = tenantFactory.getPaymentDispatcher();


            final Consumer<Payment> paymentConsumer = payment -> {
                try {
                    final PaymentWithCartLike paymentWithCartLike = queryExecutor
                        .getPaymentWithCartLike(payment.getId(), CompletableFuture.completedFuture(payment),
                            correlationId);
                    paymentDispatcher.dispatchPayment(paymentWithCartLike);
                } catch (final NoCartLikeFoundException ex) {
                    LOG.debug(LOG_PREFIX + " Could not dispatch payment with ID \"{}\": {}",
                            tenantFactory.getTenantName(),  payment.getId(), ex.getMessage());
                } catch (final ConcurrentModificationException ex) {
                    LOG.info(LOG_PREFIX + " Could not dispatch payment with ID \"{}\": The payment is currently processed by someone else.",
                            tenantFactory.getTenantName(), payment.getId());
                } catch (final Throwable ex) {
                    LOG.error(LOG_PREFIX + " Error dispatching payment with ID \"{}\"",
                            tenantFactory.getTenantName(), payment.getId(), ex);
                }
            };

            queryExecutor.consumePaymentCreatedMessages(sinceDate, paymentConsumer, correlationId);
            queryExecutor.consumePaymentTransactionAddedMessages(sinceDate, paymentConsumer,
                correlationId);
        });
    }

    protected abstract ZonedDateTime getSinceDateTime();
}
