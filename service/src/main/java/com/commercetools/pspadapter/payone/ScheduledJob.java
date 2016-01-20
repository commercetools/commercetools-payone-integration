package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.exceptions.NoCartLikeFoundException;
import io.sphere.sdk.payments.Payment;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.ZonedDateTime;
import java.util.ConcurrentModificationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author fhaertig
 * @since 07.12.15
 */
public class ScheduledJob implements Job {

    public static final Logger LOG = LogManager.getLogger(ScheduledJob.class);

    public static final String SERVICE_KEY = "INTEGRATIONSERVICE";
    public static final String DISPATCHER_KEY = "DISPATCHERSUPPLIER";

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        IntegrationService service = (IntegrationService) dataMap.get(SERVICE_KEY);

        final PaymentDispatcher paymentDispatcher = (PaymentDispatcher) dataMap.get(DISPATCHER_KEY);
        final ZonedDateTime sinceDate = ZonedDateTime.now().minusDays(2);

        final CommercetoolsQueryExecutor queryExecutor = service.getCommercetoolsQueryExecutor();
        final Consumer<Payment> paymentConsumer = payment -> {
            try {
                final PaymentWithCartLike paymentWithCartLike = queryExecutor.getPaymentWithCartLike(payment.getId(), CompletableFuture.completedFuture(payment));
                paymentDispatcher.accept(paymentWithCartLike);
            } catch (final NoCartLikeFoundException ex) {
                LOG.debug(String.format("Could not dispatch payment with ID \"%s\": %s", payment.getId(), ex.getMessage()));
            } catch (final ConcurrentModificationException ex) {
                LOG.info(String.format("Could not dispatch payment with ID \"%s\": The payment is currently processed by someone else.", payment.getId()));
                LOG.debug(ex);
            }
        };

        queryExecutor.consumePaymentCreatedMessages(sinceDate, paymentConsumer);
        queryExecutor.consumePaymentTransactionAddedMessages(sinceDate, paymentConsumer);
    }
}
