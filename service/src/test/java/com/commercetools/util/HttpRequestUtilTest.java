package com.commercetools.util;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Test;

import static com.commercetools.util.HttpRequestUtil.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class HttpRequestUtilTest {

    private static final String HTTP_HTTPBIN_ORG_GET = "http://httpbin.org/get";
    private static final String HTTP_HTTPBIN_ORG_POST = "http://httpbin.org/post";

    @Test
    public void executeGetRequest_isSuccessful() throws Exception {
        HttpResponse response = executeGetRequest(HTTP_HTTPBIN_ORG_GET);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
    }

    @Test
    public void executePostRequest_isSuccessful() throws Exception {
        HttpResponse response = executePostRequest(HTTP_HTTPBIN_ORG_POST, singletonList(nameValue("a", "b")));
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
    }

    @Test
    public void executeGetRequestToString_returnsString() throws Exception {
        assertThat(executeGetRequestToString(HTTP_HTTPBIN_ORG_GET)).contains(HTTP_HTTPBIN_ORG_GET);
    }

    @Test
    public void executePostRequestToString_returnsStringContainingRequestArguments() throws Exception {
        assertThat(executePostRequestToString(HTTP_HTTPBIN_ORG_POST, asList(
                nameValue("aaa", "bbb"),
                nameValue("ccc", 89456677823452345L))))
                .contains(HTTP_HTTPBIN_ORG_POST, "aaa", "bbb", "ccc", "89456677823452345");
    }
}
