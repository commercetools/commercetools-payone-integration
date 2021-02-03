package com.commercetools.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.sphere.sdk.http.HttpStatusCode;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Test;

import java.util.Map;

import static com.commercetools.pspadapter.payone.domain.payone.PayonePostServiceImpl.executeGetRequest;
import static com.commercetools.pspadapter.payone.domain.payone.PayonePostServiceImpl.executePostRequest;
import static com.commercetools.pspadapter.payone.domain.payone.PayonePostServiceImpl.executePostRequestToString;
import static com.commercetools.util.PayoneHttpClientConfigurationUtil.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class PayoneHttpClientConfigurationUtilTest {

    static final String HTTPS_HTTPBIN_ORG = "https://httpbin.org/";
    static final String HTTP_HTTPBIN_ORG_GET = HTTPS_HTTPBIN_ORG + "get";
    static final String HTTPS_HTTPBIN_ORG_POST = HTTPS_HTTPBIN_ORG + "post";

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    @Test
    public void executeGetRequest_isSuccessful() throws Exception {
        HttpResponse response = executeGetRequest(HTTP_HTTPBIN_ORG_GET);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
    }

    @Test
    public void executeGetRequestToString_returnsString() throws Exception {
        assertThat(executeGetRequestToString(HTTP_HTTPBIN_ORG_GET)).contains(HTTP_HTTPBIN_ORG_GET);
    }

    @Test
    public void executePostRequest_isSuccessful() throws Exception {
        HttpResponse response = executePostRequest(HTTPS_HTTPBIN_ORG_POST, singletonList(nameValue("a", "b")));
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
    }

    @Test
    public void executePostRequestToString_returnsStringContainingRequestArguments() throws Exception {
        assertThat(executePostRequestToString(HTTPS_HTTPBIN_ORG_POST, asList(
                nameValue("aaa", "bbb"),
                nameValue("ccc", 89456677823452345L))))
                .contains(HTTPS_HTTPBIN_ORG_POST, "aaa", "bbb", "ccc", "89456677823452345");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shortPostRequestWithArguments_shouldSuccess() throws Exception {
        final HttpResponse httpResponse = executePostRequest(HTTPS_HTTPBIN_ORG_POST, ImmutableList.of(
                nameValue("a", "b"),
                nameValue("hello", 22),
                nameValue("Fußgängerübergänge", "Пішохідні переходи"),
                nameValue("Їжачок", "قنفذ"),
                nameValue("инь-янь", "陰陽")));

        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatusCode.OK_200);

        String responseString = responseToString(httpResponse);
        final JsonNode formNode = ofNullable(jsonMapper.readTree(responseString)).map(node -> node.get("form")).orElse(null);
        assertThat(formNode).isNotNull();
        assertThat(formNode.isObject()).isTrue();
        final Map<String, Object> map = jsonMapper.convertValue(formNode, Map.class);
        assertThat(map).containsOnly(
                entry("a", "b"),
                entry("hello", "22"),
                entry("Fußgängerübergänge", "Пішохідні переходи"),
                entry("Їжачок", "قنفذ"),
                entry("инь-янь", "陰陽"));
    }

    @Test
    public void shortPostRequestWithoutArguments_shouldSuccess() throws Exception {
        final HttpResponse httpResponse = executePostRequest(HTTPS_HTTPBIN_ORG_POST, null);
        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatusCode.OK_200);
    }
}
