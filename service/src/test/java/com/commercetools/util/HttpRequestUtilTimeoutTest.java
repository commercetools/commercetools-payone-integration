package com.commercetools.util;

import io.sphere.sdk.http.HttpStatusCode;
import org.apache.http.HttpResponse;
import org.junit.Test;

import static com.commercetools.util.HttpRequestUtil.*;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

public class HttpRequestUtilTimeoutTest {
    /**
     * Url pattern to <a href="http://slowwly.robertomurray.co.uk/">slowwly</a> http mocking resource.
     * <p>
     * <b>%d</b> is a placeholder for response delay in msec.
     */
    private static final String SLOWWLY_URL_PATTERN = "http://slowwly.robertomurray.co.uk/delay/%d/url/http://google.com";

    /**
     * Verify normal GET response.
     * This request has response timeout shorter than max request time in
     * {@link com.commercetools.util.HttpRequestUtil#REQUEST_TIMEOUT} thus should success, opposite to
     * {@link #longGetRequest_shouldFailLong()}
     */
    @Test
    public void shortGetRequest_shouldSuccess() throws Exception {
        final int timeout = REQUEST_TIMEOUT / 2;
        final HttpResponse httpResponse = executeGetRequest(format(SLOWWLY_URL_PATTERN, timeout));

        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatusCode.OK_200);
    }

    /**
     * Verify that if request timeout is too long - the client will retry 3 times.
     * This request has response timeout larger than max request time in
     * {@link com.commercetools.util.HttpRequestUtil#REQUEST_TIMEOUT} thus should failure, opposite to
     * {@link #shortGetRequest_shouldSuccess()}
     */
    @Test
    public void longGetRequest_shouldFailLong() throws Exception {
        final int timeout = REQUEST_TIMEOUT * 2;
        final int minimalFullRequestDuration = REQUEST_TIMEOUT * (RETRY_TIMES + 1);

        final long start = System.currentTimeMillis();
        assertThatIOException().isThrownBy(() -> executeGetRequest(format(SLOWWLY_URL_PATTERN, timeout)));
        final long executionTime = System.currentTimeMillis() - start;

        assertThat(executionTime).isGreaterThanOrEqualTo(minimalFullRequestDuration);
    }

    /**
     * Verify that if POST request timeout is too long - the client will fails with {@link java.io.IOException}, but
     * won't re-try, because the client is not re-sending request arguments once they are sent.
     * See {@link HttpRequestUtil#REQUEST_SENT_RETRY_ENABLED} for more details.
     * <p>
     * This request has response timeout larger than max request time in
     * {@link com.commercetools.util.HttpRequestUtil#REQUEST_TIMEOUT} thus should failure, opposite to
     * {@link #shortGetRequest_shouldSuccess()}
     * <p>
     * This test should be change if we decide the request should be re-send on timeout exceptions.
     */
    @Test
    public void longPostRequest_shouldFailFast() throws Exception {
        final int timeout = REQUEST_TIMEOUT * 2;
        final int maximalFullRequestDuration = (int) (REQUEST_TIMEOUT * 1.5);

        final long start = System.currentTimeMillis();
        assertThatIOException().isThrownBy(() -> executePostRequest(format(SLOWWLY_URL_PATTERN, timeout), null));
        final long executionTime = System.currentTimeMillis() - start;

        assertThat(executionTime).isLessThan(maximalFullRequestDuration);
    }
}
