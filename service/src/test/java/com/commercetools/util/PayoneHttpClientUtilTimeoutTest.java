package com.commercetools.util;

import com.sun.net.httpserver.HttpServer;
import io.sphere.sdk.http.HttpStatusCode;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

import static com.commercetools.pspadapter.payone.domain.payone.PayonePostServiceImpl.executeGetRequest;
import static com.commercetools.pspadapter.payone.domain.payone.PayonePostServiceImpl.executePostRequest;
import static com.commercetools.util.PayoneHttpClientUtil.RETRY_TIMES;
import static com.commercetools.util.PayoneHttpClientUtil.TIMEOUT_TO_ESTABLISH_CONNECTION;
import static com.commercetools.util.PayoneHttpClientUtil.TIMEOUT_WHEN_CONTINUOUS_DATA_STREAM_DOES_NOT_REPLY;
import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

public class PayoneHttpClientUtilTimeoutTest {
    /**
     * Url pattern to local mocking http service.
     * <ol>
     * <li><b>%d</b> is a placeholder for socket port number.</li>
     * <li><b>%d</b> is a placeholder for response delay in msec.</li>
     * </ol>
     */
    private static final String URL_PATTERN = "http://localhost:%d?delay=%d";

    private static final Logger LOGGER = LoggerFactory.getLogger(PayoneHttpClientUtilTimeoutTest.class);

    private int testRandomPort;

    /**
     * Mock http service instance, initiated separately for every test case.
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpServer.html">
     * HttpServer</a>
     */
    private HttpServer server = null;


    /**
     * Create mock http service which responses after some <i>delay</i>, specified in the the GET query.
     */
    @Before
    public void setUp() {
        int retries = 3;

        // run the server on random port
        do {
            try {
                // see https://en.wikipedia.org/wiki/Ephemeral_port#Range for the magic port number values range
                testRandomPort = 49152 + (new Random(System.currentTimeMillis())).nextInt(65536 - 49152);
                server = HttpServer.create(new InetSocketAddress(testRandomPort), 0);
                break;
            } catch (IOException e) {
                retries--;
                LOGGER.warn("Can't create mock http service on port {}, retry {} times...", testRandomPort, retries);
            }
        } while (retries > 0);

        if (server == null) {
            throw new IllegalStateException("Can't create mock http service after retries, last tried port number was " + testRandomPort);
        }

        // configure the server to "sleep" number of milliseconds specified in "?delay=" query argument
        server.createContext("/", httpExchange -> {
            List<NameValuePair> query = URLEncodedUtils.parse(httpExchange.getRequestURI(), StandardCharsets.UTF_8);

            // read value from "?delay=" query argument
            final long timeout = query.stream().filter(pair -> "delay".equals(pair.getName()))
                    .map(NameValuePair::getValue)
                    .mapToLong(Long::decode)
                    .findFirst()
                    .orElse(0);

            LOGGER.info("Mock http server makes {} msec delay", timeout);

            try {
                sleep(timeout);
            } catch (InterruptedException e) {
                LOGGER.error("Mock http server sleeping error", e);
            }

            LOGGER.info("Mock http server continue response after {} msec ", timeout);

            httpExchange.sendResponseHeaders(200, 0);
            httpExchange.getResponseBody().close();
        });

        server.start();
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    /**
     * Verify normal GET response.
     * This request has response timeout shorter than max request time in
     * {@link PayoneHttpClientUtil#TIMEOUT_TO_ESTABLISH_CONNECTION} thus should success, opposite to
     * {@link #longGetRequest_shouldFailLong()}
     */
    @Test
    public void shortGetRequest_shouldSuccess() throws Exception {
        final int timeout = TIMEOUT_TO_ESTABLISH_CONNECTION / 2;
        final HttpResponse httpResponse = executeGetRequest(format(URL_PATTERN, testRandomPort, timeout));

        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatusCode.OK_200);
    }

    /**
     * Verify that if request timeout is too long - the client will retry 5 times
     * ({@link PayoneHttpClientUtil#RETRY_TIMES})
     * This request has response timeout larger than max request time in
     * {@link PayoneHttpClientUtil#TIMEOUT_TO_ESTABLISH_CONNECTION} thus should failure, opposite to
     * {@link #shortGetRequest_shouldSuccess()}
     */
    @Test
    public void longGetRequest_shouldFailLong() {
        final int timeout = TIMEOUT_WHEN_CONTINUOUS_DATA_STREAM_DOES_NOT_REPLY * 2;
        final int minimalFullRequestDuration = TIMEOUT_WHEN_CONTINUOUS_DATA_STREAM_DOES_NOT_REPLY * (RETRY_TIMES + 1);

        final long start = System.currentTimeMillis();
        assertThatIOException().isThrownBy(() -> executeGetRequest(format(URL_PATTERN, testRandomPort, timeout)));
        final long executionTime = System.currentTimeMillis() - start;

        assertThat(executionTime).isGreaterThanOrEqualTo(minimalFullRequestDuration);
    }

    /**
     * Verify that if POST request timeout is too long - the client will fails with {@link java.io.IOException}, but
     * won't re-try, because the client is not re-sending request arguments once they are sent.
     * See {@link PayoneHttpClientUtil#REQUEST_SENT_RETRY_ENABLED} for more details.
     * <p>
     * This request has response timeout larger than max request time in
     * {@link PayoneHttpClientUtil#TIMEOUT_TO_ESTABLISH_CONNECTION} thus should failure, opposite to
     * {@link #shortGetRequest_shouldSuccess()}
     * <p>
     * This test should be change if we decide the request should be re-send on timeout exceptions.
     */
    @Test
    public void longPostRequest_shouldFailFast() throws Exception {
        final int timeout = TIMEOUT_TO_ESTABLISH_CONNECTION * 2;
        final int maximalFullRequestDuration = (int) (TIMEOUT_TO_ESTABLISH_CONNECTION * 1.5);

        final long start = System.currentTimeMillis();
        assertThatIOException().isThrownBy(() -> executePostRequest(format(URL_PATTERN, testRandomPort, timeout), null));
        final long executionTime = System.currentTimeMillis() - start;

        assertThat(executionTime).isLessThan(maximalFullRequestDuration);
    }
}
