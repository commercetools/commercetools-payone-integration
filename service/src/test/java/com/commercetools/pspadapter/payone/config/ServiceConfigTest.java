package com.commercetools.pspadapter.payone.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

/**
 * @author fhaertig
 * @date 03.12.15
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
    public void getsCtProjectKey() {
        when(propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_PROJECT_KEY)).thenReturn("project X");
        assertThat(new ServiceConfig(propertyProvider).getCtProjectKey()).isEqualTo("project X");
    }

    @Test
    public void throwsInCaseOfMissingCtProjectKey() {
        assertThatThrowsInCaseOfMissingOrEmptyProperty(PropertyProvider.CT_PROJECT_KEY);
    }

    @Test
    public void getsCtClientId() {
        when(propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_CLIENT_ID)).thenReturn("id X");
        assertThat(new ServiceConfig(propertyProvider).getCtClientId()).isEqualTo("id X");
    }

    @Test
    public void throwsInCaseOfMissingCtClientId() {
        assertThatThrowsInCaseOfMissingOrEmptyProperty(PropertyProvider.CT_CLIENT_ID);
    }

    @Test
    public void getCtClientSecret() {
        when(propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_CLIENT_SECRET)).thenReturn("secret X");
        assertThat(new ServiceConfig(propertyProvider).getCtClientSecret()).isEqualTo("secret X");
    }

    @Test
    public void throwsInCaseOfMissingCtClientSecret() {
        assertThatThrowsInCaseOfMissingOrEmptyProperty(PropertyProvider.CT_CLIENT_SECRET);
    }

    private void assertThatThrowsInCaseOfMissingOrEmptyProperty(final String propertyName) {
        final IllegalStateException illegalStateException = new IllegalStateException();
        when(propertyProvider.getMandatoryNonEmptyProperty(propertyName)).thenThrow(illegalStateException);

        final Throwable throwable = catchThrowable(() -> new ServiceConfig(propertyProvider));

        assertThat(throwable).isSameAs(illegalStateException);
    }
}
