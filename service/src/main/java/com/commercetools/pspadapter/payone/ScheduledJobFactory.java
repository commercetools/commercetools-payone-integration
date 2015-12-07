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
 * @date 04.12.15
 */
public class ScheduledJobFactory {

    public static Scheduler createScheduledJob(
            final CronScheduleBuilder cronScheduleBuilder,
            final IntegrationService integrationService,
            final String jobKey) throws SchedulerException {

        JobDataMap dataMap = new JobDataMap();
        dataMap.put(ScheduledJob.SERVICE_KEY, integrationService);
        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity(jobKey)
                .withSchedule(cronScheduleBuilder)
                .usingJobData(dataMap)
                .build();


        scheduler.start();
        scheduler.scheduleJob(JobBuilder.newJob(ScheduledJob.class).build(), trigger);
        return scheduler;
    }
}
