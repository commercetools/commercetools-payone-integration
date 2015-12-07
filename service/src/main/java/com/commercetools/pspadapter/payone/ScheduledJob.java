package com.commercetools.pspadapter.payone;

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

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        IntegrationService service = (IntegrationService) dataMap.get(SERVICE_KEY);

        service.getPaymentQueryExecutor().getPaymentsSince(ZonedDateTime.now());
    }
}
