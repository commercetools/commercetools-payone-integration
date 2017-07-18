package com.commercetools.pspadapter.payone.domain.payone;

import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
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
        assertThat(payonePostService.getObjectMapWithExpandedLists(ImmutableMap.of())).hasSize(0);

        final Map<String, Object> simple = payonePostService.getObjectMapWithExpandedLists(ImmutableMap.of("foo", "bar"));
        assertThat(simple).hasSize(1);
        assertThat(simple.get("foo")).isEqualTo("bar");

        // for now only string/numeric values are tested

        final Map<String, Object> withExpandedLists = payonePostService.getObjectMapWithExpandedLists(
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

        assertThat(withExpandedLists).hasSize(15);
        assertThat(withExpandedLists.get("foo")).isEqualTo("bar");
        assertThat(withExpandedLists.get("woot")).isEqualTo("wootValue");
        assertThat(withExpandedLists.get("a")).isEqualTo(42);
        assertThat(withExpandedLists.get("empty")).isEqualTo("");
        assertThat(withExpandedLists.get("boolTrue")).isEqualTo(true);
        assertThat(withExpandedLists.get("boolFalse")).isEqualTo(false);
        assertThat(withExpandedLists.get("list1[1]")).isEqualTo(1);
        assertThat(withExpandedLists.get("list1[2]")).isEqualTo(2);
        assertThat(withExpandedLists.get("list1[3]")).isEqualTo(3);
        assertThat(withExpandedLists.get("listString[1]")).isEqualTo("ein");
        assertThat(withExpandedLists.get("listString[2]")).isEqualTo("zwei");
        assertThat(withExpandedLists.get("listString[3]")).isEqualTo("drei");
        assertThat(withExpandedLists.get("listDoubles[1]")).isEqualTo(3.14);
        assertThat(withExpandedLists.get("listDoubles[2]")).isEqualTo(2.71);
        assertThat(withExpandedLists.get("listDoubles[3]")).isEqualTo(9.81);


        final Map<String, Object> withEmptyLists = payonePostService.getObjectMapWithExpandedLists(
                ImmutableMap.of("foo", new ArrayList<>(),
                        "bar", new LinkedList<>()));

        assertThat(withEmptyLists.size()).isEqualTo(2);
        assertThat(withEmptyLists.get("foo[]")).isEqualTo("");
        assertThat(withEmptyLists.get("bar[]")).isEqualTo("");
    }
}