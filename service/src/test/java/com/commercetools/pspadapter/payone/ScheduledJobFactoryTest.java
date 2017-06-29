package com.commercetools.pspadapter.payone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static Logger LOG = LoggerFactory.getLogger(ScheduledJobFactoryTest.class);

    @Before
    public void setUp() throws Exception {
        scheduledJobFactory = new ScheduledJobFactory();
    }

    @After
    public void tearDown() throws Exception {
        scheduledJobFactory.shutdown(false);
    }

    @Test(timeout=40000)
    public void jobIsExecuted3TimesIn30Second() throws Exception {
        when(integrationService.getTenantFactories()).thenReturn(Collections.emptyList());

        // schedule to execute every 10 seconds and wait about 30 seconds
        JobDetail scheduledJob = scheduledJobFactory.createScheduledJob(
                CronScheduleBuilder.cronSchedule("0/10 * * * * ? *"),
                ScheduledJobShortTimeframe.class,
                integrationService);

        LOG.info("Wait 30 seconds to allow the job execute");

        Thread.sleep(32000); // +2 second gap to avoid the test fail on short time lags

        LOG.info("Waited 30 seconds");

        // verify the job called integrationService.getTenantFactories() 3 or 4 times in 30 seconds
        verify(integrationService, atLeast(3)).getTenantFactories();

        // 4 times is possible when the job is started at 09, 19, ..., 59 seconds,
        // then 2 seconds gap above will make one additional overlapped call at 40, 50, ..., 30 seconds
        verify(integrationService, atMost(4)).getTenantFactories();

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

        final int waitTimeout = DELAY_START_IN_SECONDS + 1;
        LOG.info("Wait at most {} to start 2 scheduled jobs", waitTimeout);
        while (!allJobsRun.get()) {
            // +1 second to avoid lags effect
            synchronized (this) {
                this.wait((waitTimeout) * 1000);
            }
        }

        assertThat(allJobsRun.get()).isTrue();

        scheduledJobFactory.deleteScheduledJob(scheduledJob1.getKey());
        scheduledJobFactory.deleteScheduledJob(scheduledJob2.getKey());
    }

}

