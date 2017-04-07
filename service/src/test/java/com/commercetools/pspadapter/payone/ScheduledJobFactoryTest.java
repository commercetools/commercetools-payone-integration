package com.commercetools.pspadapter.payone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobDetail;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.commercetools.pspadapter.payone.ScheduledJobFactory.DELAY_START_IN_SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ScheduledJobFactoryTest {

    @Mock
    private IntegrationService integrationService;

    private ScheduledJobFactory scheduledJobFactory;

    @Before
    public void setUp() throws Exception {
        scheduledJobFactory = new ScheduledJobFactory();
    }

    @After
    public void tearDown() throws Exception {
        scheduledJobFactory.shutdown(false);
    }

    @Test(timeout=70000)
    public void jobIsExecuted4TimesInMinute() throws Exception {
        when(integrationService.getTenantFactories()).thenReturn(Collections.emptyList());

        // schedule to execute every 15 seconds and wait 60 seconds
        JobDetail scheduledJob = scheduledJobFactory.createScheduledJob(
                CronScheduleBuilder.cronSchedule("0/15 * * * * ? *"),
                ScheduledJobShortTimeframe.class,
                integrationService);

        Thread.sleep(62000); // +2 second gap to avoid the test fail on short time lags

        // verify the job called integrationService.getTenantFactories() 4 or 5 times in 1 minute
        verify(integrationService, atLeast(4)).getTenantFactories();

        // 5 times is possible when the job is started at 14, 29, 44 or 59 seconds,
        // then 2 seconds gap above will make one additional overlap on 15, 30, 45 or 00 seconds
        verify(integrationService, atMost(5)).getTenantFactories();

        scheduledJobFactory.deleteScheduledJob(scheduledJob.getKey());
    }

    @Test(timeout=10000)
    public void setAllScheduledItemsStartedListener_executed() throws Exception {
        JobDetail scheduledJob1 = scheduledJobFactory.createScheduledJob(
                CronScheduleBuilder.cronSchedule("0/15 * * * * ? *"),
                ScheduledJobShortTimeframe.class,
                integrationService);

        JobDetail scheduledJob2 = scheduledJobFactory.createScheduledJob(
                CronScheduleBuilder.cronSchedule("5 0 0/1 * * ? *"),
                ScheduledJobLongTimeframe.class,
                integrationService);

        // because value in the closure should be final, we use this mutable wrapper
        final AtomicBoolean allJobsRun = new AtomicBoolean(false);

        scheduledJobFactory.setAllScheduledItemsStartedListener(() -> {
            // when all jobs are run - set allJobsRun to true and notify the waiting thread.
            synchronized (this) {
                allJobsRun.set(true);
                this.notify();
            }
        });

        // wait at most DELAY_START_IN_SECONDS to start 2 scheduled jobs
        while (!allJobsRun.get()) {
            // +1 second to avoid lags effect
            synchronized (this) {
                this.wait((DELAY_START_IN_SECONDS + 1) * 1000);
            }
        }

        assertThat(allJobsRun.get()).isTrue();

        scheduledJobFactory.deleteScheduledJob(scheduledJob1.getKey());
        scheduledJobFactory.deleteScheduledJob(scheduledJob2.getKey());
    }

}

