package com.commercetools.pspadapter.payone.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static com.commercetools.pspadapter.payone.config.PropertyProvider.TENANTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * @author fhaertig
 * @since 03.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceConfigTest {

    private static final String dummyValue = "123";

    @Spy
    private PropertyProvider propertyProvider;

    @Test
    public void getsScheduledJobCronForShortTimeFramePoll() {
        mockDummyDefault(propertyProvider);
        when(propertyProvider.getProperty(PropertyProvider.SHORT_TIME_FRAME_SCHEDULED_JOB_CRON))
                .thenReturn(Optional.of("short cron"));
        assertThat(new ServiceConfig(propertyProvider).getScheduledJobCronForShortTimeFramePoll())
                .isEqualTo("short cron");
    }

    @Test
    public void getsDefaultScheduledJobCronForShortTimeFramePollIfPropertyIsNotProvided() {
        mockDummyDefault(propertyProvider);
        when(propertyProvider.getProperty(PropertyProvider.SHORT_TIME_FRAME_SCHEDULED_JOB_CRON))
                .thenReturn(Optional.empty());
        assertThat(new ServiceConfig(propertyProvider).getScheduledJobCronForShortTimeFramePoll())
                .isEqualTo("0/30 * * * * ? *");
    }

    @Test
    public void getsScheduledJobCronForLongTimeFramePoll() {
        mockDummyDefault(propertyProvider);
        when(propertyProvider.getProperty(PropertyProvider.LONG_TIME_FRAME_SCHEDULED_JOB_CRON))
                .thenReturn(Optional.of("long cron"));
        assertThat(new ServiceConfig(propertyProvider).getScheduledJobCronForLongTimeFramePoll())
                .isEqualTo("long cron");
    }

    @Test
    public void getsDefaultScheduledJobCronForLongTimeFramePollIfPropertyIsNotProvided() {
        mockDummyDefault(propertyProvider);
        when(propertyProvider.getProperty(PropertyProvider.LONG_TIME_FRAME_SCHEDULED_JOB_CRON))
                .thenReturn(Optional.empty());
        assertThat(new ServiceConfig(propertyProvider).getScheduledJobCronForLongTimeFramePoll())
                .isEqualTo("5 0 0/1 * * ? *");
    }

    @Test
    public void getsSingleTenantName() {
        when(propertyProvider.getProperty(TENANTS)).thenReturn(Optional.of("testTenantName"));
        assertThat(new ServiceConfig(propertyProvider).getTenants()).containsOnly("testTenantName");
    }

    @Test
    public void getsMultipleTenantNameAndTrimsWhitespaces() {
        when(propertyProvider.getProperty(TENANTS))
                .thenReturn(Optional.of("\ttestTenantName1,   secondTestTenant ; andOneMoreTenant  , andTheLastOne "));
        assertThat(new ServiceConfig(propertyProvider).getTenants())
                .containsOnly("testTenantName1", "secondTestTenant", "andOneMoreTenant", "andTheLastOne");
    }

    @Test
    public void throwsExceptionIfTenantNameIsEmpty() throws Exception {
        when(propertyProvider.getProperty(TENANTS)).thenReturn(Optional.empty());
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new ServiceConfig(propertyProvider).getTenants())
                .withMessageContaining(TENANTS);

        when(propertyProvider.getProperty(TENANTS)).thenReturn(Optional.of(""));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new ServiceConfig(propertyProvider).getTenants())
                .withMessageContaining(TENANTS);

        when(propertyProvider.getProperty(TENANTS)).thenReturn(Optional.of("   "));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new ServiceConfig(propertyProvider).getTenants())
                .withMessageContaining(TENANTS);

        when(propertyProvider.getProperty(TENANTS)).thenReturn(Optional.of(" ,  "));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new ServiceConfig(propertyProvider).getTenants())
                .withMessageContaining(TENANTS);

        when(propertyProvider.getProperty(TENANTS)).thenReturn(Optional.of("one,,two"));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new ServiceConfig(propertyProvider).getTenants())
                .withMessageContaining(TENANTS);

        when(propertyProvider.getProperty(TENANTS)).thenReturn(Optional.of(" one ,  , two"));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new ServiceConfig(propertyProvider).getTenants())
                .withMessageContaining(TENANTS);
    }

    private static void mockDummyDefault(PropertyProvider propertyProvider) {
        doReturn(Optional.of(dummyValue)).when(propertyProvider).getProperty(anyString());
    }
}