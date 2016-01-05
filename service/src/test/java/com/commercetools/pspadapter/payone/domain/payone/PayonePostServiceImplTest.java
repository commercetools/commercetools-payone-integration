package com.commercetools.pspadapter.payone.domain.payone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class PayonePostServiceImplTest {

    public static final String PAYONE_SERVER_API_URL = "http://some.url.org/payone";

    private PayonePostServiceImpl payonePostService;

    @Before
    public void setup() throws PayoneException {
        payonePostService = PayonePostServiceImpl.of(PAYONE_SERVER_API_URL);
    }

    @Test
    public void shouldThrowConfigurationExceptionIfUrlIsEmptyOnInitialization() {
        final Throwable throwable = catchThrowable(() -> PayonePostServiceImpl.of(""));

        assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The server api url must not be null or empty.");
    }

    @Test
    public void shouldThrowConfigurationExceptionIfUrlIsNullOnInitialization() {
        final Throwable throwable = catchThrowable(() -> PayonePostServiceImpl.of(null));

        assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The server api url must not be null or empty.");
    }

    @Test
    public void shouldInitServiceWithCorrectUrl() {
        assertThat(payonePostService.getServerAPIURL()).isEqualTo(PAYONE_SERVER_API_URL);
    }


    @Test
    public void shouldBuildMapFromServerResponse() throws UnsupportedEncodingException {
        String serverResponse = "paramA=a\nredirecturl=https://www.redirect.de/xxx\nstatus=SUCCESSFUL";
        Map<String, String> result = payonePostService.buildMapFromResultParams(serverResponse);
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(3);
        assertThat(result).containsEntry("paramA", "a");
        assertThat(result).containsEntry("redirecturl", "https://www.redirect.de/xxx");
        assertThat(result).containsEntry("status", "SUCCESSFUL");
    }

    @Test
    public void shouldReturnEmptyMap() throws UnsupportedEncodingException {
        String serverResponse = "=x=";
        Map<String, String> result = payonePostService.buildMapFromResultParams(serverResponse);
        assertThat(result).isEmpty();
    }
}