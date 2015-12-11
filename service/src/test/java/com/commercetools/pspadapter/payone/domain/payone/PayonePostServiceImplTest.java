package com.commercetools.pspadapter.payone.domain.payone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
    public void shouldBuildRequestParamsString() throws PayoneException {
        Map<String, String> requestParams = new HashMap<String, String>();
        requestParams.put("paramA", "a");
        requestParams.put("paramB", "b");
        StringBuffer result = payonePostService.buildRequestParamsString(requestParams);
        assertThat(result.length()).isGreaterThan(0);
        assertThat(result.toString()).isEqualTo("paramA=a&paramB=b");
    }

    @Test
    public void shouldReturnEmptyParamsString() throws PayoneException {
        Map<String, String> requestParams = Collections.emptyMap();
        StringBuffer result = payonePostService.buildRequestParamsString(requestParams);
        assertThat(result.length()).isEqualTo(0);
        assertThat(result.toString()).isEqualTo("");
    }

    @Test
    public void shouldBuildMapFromServerResponse() throws UnsupportedEncodingException {
        List<String> serverResponse = Lists.newArrayList();
        serverResponse.add("paramA=a");
        serverResponse.add("redirecturl=https://www.redirect.de/xxx");
        Map<String, String> result = payonePostService.buildMapFromResultParams(serverResponse);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result).containsKey("paramA");
        assertThat(result).containsValue("a");
        assertThat(result.get("redirecturl")).isEqualTo("https://www.redirect.de/xxx");
    }

    @Test
    public void shouldReturnEmptyMap() throws UnsupportedEncodingException {
        List<String> serverResponse = ImmutableList.of("=", "x=");
        Map<String, String> result = payonePostService.buildMapFromResultParams(serverResponse);
        assertThat(result.isEmpty()).isTrue();
    }
}