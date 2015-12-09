package com.commercetools.pspadapter.payone;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author fhaertig
 * @date 07.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class ScheduledJobTest {

    @Mock
    private IntegrationService integrationService;

    @Mock
    private PaymentQueryExecutor paymentQueryExecutor;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Test
    public void createScheduledJob() throws JobExecutionException {
        ScheduledJob job = new ScheduledJob();
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(ScheduledJob.SERVICE_KEY, integrationService);
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(dataMap);
        when(integrationService.getPaymentQueryExecutor()).thenReturn(paymentQueryExecutor);

        job.execute(jobExecutionContext);
        verify(integrationService).getPaymentQueryExecutor();
    }
}
