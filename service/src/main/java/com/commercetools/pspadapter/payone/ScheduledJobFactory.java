package com.commercetools.pspadapter.payone;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 * @author fhaertig
 * @since 04.12.15
 */
public class ScheduledJobFactory {

    private static final int DELAY_START_IN_SECONDS = 10;

    public static Scheduler createScheduledJob(
            final CronScheduleBuilder cronScheduleBuilder,
            final IntegrationService integrationService,
            final String jobKey,
            final PaymentDispatcher paymentDispatcher) throws SchedulerException {

        JobDataMap dataMap = new JobDataMap();
        dataMap.put(ScheduledJob.SERVICE_KEY, integrationService);
        dataMap.put(ScheduledJob.DISPATCHER_KEY, paymentDispatcher);
        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity(jobKey)
                .withSchedule(cronScheduleBuilder)
                .usingJobData(dataMap)
                .build();

        scheduler.startDelayed(DELAY_START_IN_SECONDS);
        scheduler.scheduleJob(JobBuilder.newJob(ScheduledJob.class).build(), trigger);
        return scheduler;
    }
}
