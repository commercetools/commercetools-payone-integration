package com.commercetools.pspadapter.payone;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author fhaertig
 * @since 04.12.15
 */
public class ScheduledJobFactory {

    public static final Logger LOG = LogManager.getLogger(IntegrationService.class);

    private static final int DELAY_START_IN_SECONDS = 10;

    public static void createScheduledJob(
            final CronScheduleBuilder cronScheduleBuilder,
            final Class<? extends ScheduledJob> jobClass,
            final IntegrationService integrationService,
            final PaymentDispatcher paymentDispatcher) throws SchedulerException {

        JobDataMap dataMap = new JobDataMap();
        dataMap.put(ScheduledJob.SERVICE_KEY, integrationService);
        dataMap.put(ScheduledJob.DISPATCHER_KEY, paymentDispatcher);
        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withSchedule(cronScheduleBuilder)
                .usingJobData(dataMap)
                .build();

        scheduler.startDelayed(DELAY_START_IN_SECONDS);
        Date date = scheduler.scheduleJob(JobBuilder.newJob(jobClass).build(), trigger);
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        LOG.info(String.format("Starting scheduled job '%s' at %s", jobClass.getSimpleName(), df.format(date)));
    }
}
