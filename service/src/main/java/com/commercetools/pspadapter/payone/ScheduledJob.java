package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.ZonedDateTime;

/**
 * @author fhaertig
 * @date 07.12.15
 */
public class ScheduledJob implements Job {

    public static final String SERVICE_KEY = "INTEGRATIONSERVICE";
    public static final String DISPATCHER_KEY = "DISPATCHERSUPPLIER";

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        IntegrationService service = (IntegrationService) dataMap.get(SERVICE_KEY);

        final PaymentDispatcher paymentDispatcher = (PaymentDispatcher) dataMap.get(DISPATCHER_KEY);
        final ZonedDateTime sinceDate = ZonedDateTime.now().minusDays(2);

        final CommercetoolsQueryExecutor queryExecutor = service.getCommercetoolsQueryExecutor();
        queryExecutor.consumePaymentCreatedMessages(sinceDate, paymentDispatcher);
        queryExecutor.consumePaymentTransactionAddedMessages(sinceDate, paymentDispatcher);
    }
}
