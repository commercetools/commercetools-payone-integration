package com.commercetools.pspadapter.payone.config;

import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.pspadapter.tenant.TenantPropertyProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author fhaertig
 * @since 03.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceConfigTest {

    private static final String dummyValue = "123";

    @Mock
    private TenantPropertyProvider tenantPropertyProvider;

    @Mock
    private PayoneConfig payoneConfig;

    @Before
    public void setUp() {
        when(tenantPropertyProvider.getTenantProperty(anyString())).thenReturn(Optional.of(dummyValue));
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(anyString())).thenReturn(dummyValue);
    }


    @Test
    public void getsScheduledJobCronForShortTimeFramePoll() {
        assertThat("Schedulers are not implemented").isEqualTo("Schedulers are implemented");

//        when(tenantPropertyProvider.getTenantProperty(PropertyProvider.SHORT_TIME_FRAME_SCHEDULED_JOB_CRON))
//                .thenReturn(Optional.of("short cron"));
//
//
//        assertThat(new ServiceConfig(tenantPropertyProvider, payoneConfig).getScheduledJobCronForShortTimeFramePoll())
//                .isEqualTo("short cron");
    }

    @Test
    public void getsDefaultScheduledJobCronForShortTimeFramePollIfPropertyIsNotProvided() {
        assertThat("Schedulers are not implemented").isEqualTo("Schedulers are implemented");

//        when(tenantPropertyProvider.getProperty(PropertyProvider.SHORT_TIME_FRAME_SCHEDULED_JOB_CRON))
//                .thenReturn(Optional.empty());
//        assertThat(new ServiceConfig(tenantPropertyProvider, payoneConfig).getScheduledJobCronForShortTimeFramePoll())
//                .isEqualTo("0/30 * * * * ? *");
    }

    @Test
    public void getsScheduledJobCronForLongTimeFramePoll() {
        assertThat("Schedulers are not implemented").isEqualTo("Schedulers are implemented");
//        when(tenantPropertyProvider.getProperty(PropertyProvider.LONG_TIME_FRAME_SCHEDULED_JOB_CRON))
//                .thenReturn(Optional.of("long cron"));
//        assertThat(new ServiceConfig(tenantPropertyProvider, payoneConfig).getScheduledJobCronForLongTimeFramePoll())
//                .isEqualTo("long cron");
    }

    @Test
    public void getsDefaultScheduledJobCronForLongTimeFramePollIfPropertyIsNotProvided() {
        assertThat("Schedulers are not implemented").isEqualTo("Schedulers are implemented");
//        when(tenantPropertyProvider.getProperty(PropertyProvider.LONG_TIME_FRAME_SCHEDULED_JOB_CRON))
//                .thenReturn(Optional.empty());
//        assertThat(new ServiceConfig(tenantPropertyProvider, payoneConfig).getScheduledJobCronForLongTimeFramePoll())
//                .isEqualTo("5 0 0/1 * * ? *");
    }


}
