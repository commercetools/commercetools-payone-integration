package com.commercetools.pspadapter.payone;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
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
            final Class<? extends Job> job,
            final String jobKey) throws SchedulerException {
        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity(jobKey)
                .withSchedule(cronScheduleBuilder)
                .build();

        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();
        scheduler.scheduleJob(JobBuilder.newJob(job).build(), trigger);
        return scheduler;
    }
}
