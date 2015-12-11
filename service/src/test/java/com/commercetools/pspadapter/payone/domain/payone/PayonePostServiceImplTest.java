package com.commercetools.pspadapter.payone.domain.payone;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PayonePostServiceImplTest {

    public static final String PAYONE_SERVER_API_URL = "http://some.url.org/payone";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private PayonePostServiceImpl payonePostService;

    @Before
    public void setup() throws PayoneException {
        payonePostService = PayonePostServiceImpl.of(PAYONE_SERVER_API_URL);
    }

    @Test
    public void shouldThrowConfigurationExceptionIfUrlIsEmptyOnInitialization() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("The server api url must not be null or empty.");
        PayonePostServiceImpl.of("");
    }

    @Test
    public void shouldThrowConfigurationExceptionIfUrlIsNullOnInitialization() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("The server api url must not be null or empty.");
        PayonePostServiceImpl.of(null);
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
        assertThat(result.containsKey("paramA"));
        assertThat(result.containsValue("a"));
        assertThat(result.get("redirecturl")).isEqualTo("https://www.redirect.de/xxx");
    }

    @Test
    public void shouldReturnEmptyMap() throws UnsupportedEncodingException {
        List<String> serverResponse = Lists.newArrayList();
        serverResponse.add("=");
        serverResponse.add("x=");
        Map<String, String> result = payonePostService.buildMapFromResultParams(serverResponse);
        assertThat(result.isEmpty()).isTrue();
    }
}