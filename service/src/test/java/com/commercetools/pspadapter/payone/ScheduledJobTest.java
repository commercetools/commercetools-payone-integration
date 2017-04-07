package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.tenant.TenantFactory;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author fhaertig
 * @since 07.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class ScheduledJobTest {

    @Mock
    private IntegrationService integrationService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private TenantFactory factoryOne;

    @Mock
    private TenantFactory factoryTwo;

    @Mock
    private CommercetoolsQueryExecutor commercetoolsQueryExecutorOne;

    @Mock
    private CommercetoolsQueryExecutor commercetoolsQueryExecutorTwo;

    private List<TenantFactory> tenantFactories;

    @Before
    public void setUp() throws Exception {
        tenantFactories = ImmutableList.of(factoryOne, factoryTwo);
    }

    @Test
    public void createScheduledJob() throws JobExecutionException {
        final ZonedDateTime zonedDateTime = ZonedDateTime.now().minusMinutes(2);

        final ScheduledJob job = new ScheduledJob() {
            @Override
            protected ZonedDateTime getSinceDateTime() {
                return zonedDateTime;
            }
        };

        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(ScheduledJob.INTEGRATION_SERVICE, integrationService);

        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(dataMap);
        when(integrationService.getTenantFactories()).thenReturn(tenantFactories);

        when(factoryOne.getCommercetoolsQueryExecutor()).thenReturn(commercetoolsQueryExecutorOne);
        when(factoryTwo.getCommercetoolsQueryExecutor()).thenReturn(commercetoolsQueryExecutorTwo);

        job.execute(jobExecutionContext);

        // verify the job called consumePaymentCreatedMessages and consumePaymentTransactionAddedMessages
        // for both tenant executors called from factories

        verify(commercetoolsQueryExecutorOne).consumePaymentCreatedMessages(eq(zonedDateTime), any());
        verify(commercetoolsQueryExecutorOne).consumePaymentTransactionAddedMessages(eq(zonedDateTime), any());

        verify(commercetoolsQueryExecutorTwo).consumePaymentCreatedMessages(eq(zonedDateTime), any());
        verify(commercetoolsQueryExecutorTwo).consumePaymentTransactionAddedMessages(eq(zonedDateTime), any());

    }
}
