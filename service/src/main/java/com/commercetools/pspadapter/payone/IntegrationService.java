package com.commercetools.pspadapter.payone;

import org.joda.time.DateTime;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class IntegrationService implements Job {

    private final PaymentQueryExecutor paymentQueryExecutor;

    IntegrationService(final PaymentQueryExecutor paymentQueryExecutor) {
        this.paymentQueryExecutor = paymentQueryExecutor;
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        paymentQueryExecutor.getPaymentsSince(new DateTime());
    }
}
