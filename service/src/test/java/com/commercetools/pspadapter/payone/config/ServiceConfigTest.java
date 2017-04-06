package com.commercetools.pspadapter.payone.config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    private PropertyProvider propertyProvider;


    @Before
    public void setUp() {
        when(propertyProvider.getProperty(anyString())).thenReturn(Optional.of(dummyValue));
        when(propertyProvider.getMandatoryNonEmptyProperty(anyString())).thenReturn(dummyValue);
    }


    @Test
    public void getsScheduledJobCronForShortTimeFramePoll() {
        when(propertyProvider.getProperty(PropertyProvider.SHORT_TIME_FRAME_SCHEDULED_JOB_CRON))
                .thenReturn(Optional.of("short cron"));
        assertThat(new ServiceConfig(propertyProvider).getScheduledJobCronForShortTimeFramePoll())
                .isEqualTo("short cron");
    }

    @Test
    public void getsDefaultScheduledJobCronForShortTimeFramePollIfPropertyIsNotProvided() {
        when(propertyProvider.getProperty(PropertyProvider.SHORT_TIME_FRAME_SCHEDULED_JOB_CRON))
                .thenReturn(Optional.empty());
        assertThat(new ServiceConfig(propertyProvider).getScheduledJobCronForShortTimeFramePoll())
                .isEqualTo("0/30 * * * * ? *");
    }

    @Test
    public void getsScheduledJobCronForLongTimeFramePoll() {
        when(propertyProvider.getProperty(PropertyProvider.LONG_TIME_FRAME_SCHEDULED_JOB_CRON))
                .thenReturn(Optional.of("long cron"));
        assertThat(new ServiceConfig(propertyProvider).getScheduledJobCronForLongTimeFramePoll())
                .isEqualTo("long cron");
    }

    @Test
    public void getsDefaultScheduledJobCronForLongTimeFramePollIfPropertyIsNotProvided() {
        when(propertyProvider.getProperty(PropertyProvider.LONG_TIME_FRAME_SCHEDULED_JOB_CRON))
                .thenReturn(Optional.empty());
        assertThat(new ServiceConfig(propertyProvider).getScheduledJobCronForLongTimeFramePoll())
                .isEqualTo("5 0 0/1 * * ? *");
    }
}