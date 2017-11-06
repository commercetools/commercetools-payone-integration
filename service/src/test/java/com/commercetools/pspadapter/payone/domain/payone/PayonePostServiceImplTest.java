package com.commercetools.pspadapter.payone.domain.payone;

import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

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

    @Test
    public void getObjectMapWithExpandedLists() {
        assertThat(payonePostService.getNameValuePairsWithExpandedLists(ImmutableMap.of())).hasSize(0);

        final List<BasicNameValuePair> simple = payonePostService.getNameValuePairsWithExpandedLists(ImmutableMap.of("foo", "bar"));
        assertThat(simple).hasSize(1);
        assertThat(simple).contains(new BasicNameValuePair("foo", "bar"));

        // for now only string/numeric values are tested

        final List<BasicNameValuePair> withExpandedLists = payonePostService.getNameValuePairsWithExpandedLists(
                ImmutableMap.<String, Object>builder()
                        .put("foo", "bar")
                        .put("woot", "wootValue")
                        .put("list1", ImmutableList.of(1, 2, 3))
                        .put("a", 42)
                        .put("empty", "")
                        .put("boolTrue", true)
                        .put("boolFalse", false)
                        .put("listString", new LinkedList<>(ImmutableList.of("ein", "zwei", "drei")))
                        .put("listDoubles", asList(3.14, 2.71, 9.81))
                        .build());

        assertThat(withExpandedLists).containsExactlyInAnyOrder(
                new BasicNameValuePair("foo", "bar"),
                new BasicNameValuePair("woot", "wootValue"),
                new BasicNameValuePair("a", "42"),
                new BasicNameValuePair("empty", ""),
                new BasicNameValuePair("boolTrue", "true"),
                new BasicNameValuePair("boolFalse", "false"),
                new BasicNameValuePair("list1[1]", "1"),
                new BasicNameValuePair("list1[2]", "2"),
                new BasicNameValuePair("list1[3]", "3"),
                new BasicNameValuePair("listString[1]", "ein"),
                new BasicNameValuePair("listString[2]", "zwei"),
                new BasicNameValuePair("listString[3]", "drei"),
                new BasicNameValuePair("listDoubles[1]", "3.14"),
                new BasicNameValuePair("listDoubles[2]", "2.71"),
                new BasicNameValuePair("listDoubles[3]", "9.81"));


        final List<BasicNameValuePair> withEmptyLists = payonePostService.getNameValuePairsWithExpandedLists(
                ImmutableMap.of("foo", new ArrayList<>(),
                        "bar", new LinkedList<>()));

        assertThat(withEmptyLists).containsExactlyInAnyOrder(
                new BasicNameValuePair("foo[]", ""),
                new BasicNameValuePair("bar[]", ""));
    }
}