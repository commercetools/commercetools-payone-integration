package com.commercetools.pspadapter.payone;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.listeners.SchedulerListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author fhaertig
 * @since 04.12.15
 */
public class ScheduledJobFactory {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationService.class);

    static final int DELAY_START_IN_SECONDS = 5;

    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    private final Scheduler scheduler;

    /**
     * How many items are scheduled but not started yet.
     */
    private int scheduledItemsCount = 0;

    /**
     * Single listener for started scheduled items. This instance is added to {@code scheduler#getListenerManager()}
     * if {@code allScheduledItemsStarted} is set.
     */
    private SchedulerListenerSupport schedulerStartedListener;

    /**
     * Callback to call when all scheduled jobs are started (e.g. when {@code scheduledItemsCount} becomes 0.
     */
    private Runnable allScheduledItemsStarted;

    public ScheduledJobFactory() throws SchedulerException {
        scheduler = new StdSchedulerFactory().getScheduler();
    }

    public JobDetail createScheduledJob(
            final CronScheduleBuilder cronScheduleBuilder,
            final Class<? extends ScheduledJob> jobClass,
            final IntegrationService integrationService) throws SchedulerException {

        JobDataMap dataMap = new JobDataMap();
        dataMap.put(ScheduledJob.INTEGRATION_SERVICE, integrationService);

        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withSchedule(cronScheduleBuilder)
                .usingJobData(dataMap)
                .build();

        scheduledItemsCount++;
        scheduler.startDelayed(DELAY_START_IN_SECONDS);

        JobDetail jobDetail = JobBuilder.newJob(jobClass).build();
        Date date = scheduler.scheduleJob(jobDetail, trigger);

        LOG.info("Starting scheduled job '{}' at {}", jobClass.getSimpleName(), DATE_FORMATTER.format(date));

        return jobDetail;
    }

    /**
     * Set a callback which to call when all the scheduled items (jobs) are started.
     * @param schedulerListener nullable callback to run
     * @throws SchedulerException
     */
    public void setAllScheduledItemsStartedListener(Runnable schedulerListener) throws SchedulerException {
        allScheduledItemsStarted = schedulerListener;
        if (allScheduledItemsStarted != null && schedulerStartedListener == null) {
            schedulerStartedListener = new SchedulerStartListener(this::countNotifyAndCleanupListeners);
            scheduler.getListenerManager().addSchedulerListener(schedulerStartedListener);
        }

        if (allScheduledItemsStarted == null && schedulerStartedListener != null) {
            scheduler.getListenerManager().removeSchedulerListener(schedulerStartedListener);
            schedulerStartedListener = null;
        }
    }

    /**
     * So far expected to be used only in the tests.
     */
    void shutdown(boolean waitForJobsToComplete) throws SchedulerException {
        scheduler.shutdown(waitForJobsToComplete);
    }

    /**
     * So far expected to be used only in the tests.
     */
    boolean deleteScheduledJob(JobKey jobKey) throws SchedulerException {
        return scheduler.deleteJob(jobKey);
    }

    /**
     * Count how many times the scheduled jobs are started, and if all started - cleanup the listeners and callbacks.
     */
    private void countNotifyAndCleanupListeners() {
        this.scheduledItemsCount--;
        if (scheduledItemsCount == 0 && allScheduledItemsStarted != null) {
            try {
                allScheduledItemsStarted.run();
                setAllScheduledItemsStartedListener(null);
            } catch (SchedulerException e) {
                LOG.error("Scheduler startup listening error", e);
            }

            allScheduledItemsStarted = null;
        }
    }
}

/**
 * The simplest {@link SchedulerListener#schedulerStarted} listener implementation.
 */
class SchedulerStartListener extends SchedulerListenerSupport {

    private Runnable startListener;

    SchedulerStartListener(Runnable startListener) {
        this.startListener = startListener;
    }

    @Override
    public void schedulerStarted() {
        startListener.run();
    }
}
