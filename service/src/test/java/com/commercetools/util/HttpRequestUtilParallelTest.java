package com.commercetools.util;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.commercetools.util.HttpRequestUtil.*;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stress test {@link HttpRequestUtil}: make a lot of parallel requests with timeouts + test <i>toString</i> methods.
 * <p>
 * <b>Note:</b> These tests are based on request/response from <a href="http://httpbin.org/">http://httpbin.org/</a>,
 * thus they may rarely fail if the service is out of order.
 */
public class HttpRequestUtilParallelTest {

    // try to make 200 simultaneous requests in 200 threads
    private final int nThreads = CONNECTION_MAX_TOTAL;
    private final int requests = CONNECTION_MAX_TOTAL;

    @Test
    public void executeGetRequest_shouldBeParalleled() throws Exception {
        makeParallelRequests(nThreads, requests, () -> {
            try {
                final HttpResponse httpResponse = executeGetRequest("http://httpbin.org/get");
                assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
                return 1;
            } catch (IOException e) {
                throw new RuntimeException("Request exception", e);
            }
        });
    }

    @Test
    public void executePostRequest_shouldBeParalleled() throws Exception {
        makeParallelRequests(nThreads, requests, () -> {
            try {
                final HttpResponse httpResponse = executePostRequest("http://httpbin.org/post", asList(
                        nameValue("xxx", "yyy"),
                        nameValue("zzz", 11223344)));
                assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
                assertThat(responseToString(httpResponse)).contains("xxx", "yyy", "zzz", "11223344");
                return 1;
            } catch (IOException e) {
                throw new RuntimeException("Request exception", e);
            }
        });
    }

    @Test
    public void executeGetRequestToString_returnsString() throws Exception {
        assertThat(executeGetRequestToString("http://httpbin.org/get")).contains("http://httpbin.org/get");
    }

    @Test
    public void executePostRequestToString_returnsStringContainingRequestArguments() throws Exception {
        assertThat(executePostRequestToString("http://httpbin.org/post", asList(
                nameValue("aaa", "bbb"),
                nameValue("ccc", 89456677823452345L))))
                .contains("http://httpbin.org/post", "aaa", "bbb", "ccc", "89456677823452345");
    }

    private void makeParallelRequests(final int nThreads, final int requests, final Supplier<Integer> request) throws Exception {
        final ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(nThreads);

        final AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < requests; i++) {
            newFixedThreadPool.execute(() -> counter.addAndGet(request.get()));
        }

        newFixedThreadPool.shutdown();

        // length of the longest pipe in the multi thread pipeline,
        // literally it is an maximum integer number of sequential requests in one pipe (thread)
        final int criticalPathLength = (int) Math.ceil((double) requests / nThreads);

        // longest expected time of one successful request (even if retried),
        // which may have +RETRY_TIMES attempts additionally to the first (failed) attempt
        final int longestRequestTimeMsec = REQUEST_TIMEOUT * (1 + RETRY_TIMES);

        // await not more than (longestRequestTimeMsec * criticalPathLength) msec with
        // coefficient 1.5 is added to avoid test fails on some lags and threads switching timeouts.
        final long maxCriticalPathDurationMsec = (int) (1.5 * longestRequestTimeMsec * criticalPathLength);
        boolean interrupted = newFixedThreadPool.awaitTermination(maxCriticalPathDurationMsec, TimeUnit.MILLISECONDS);
        assertThat(interrupted)
                .withFailMessage(format("The execution thread pool was not able to shutdown in time %s msec", maxCriticalPathDurationMsec))
                .isTrue();

        assertThat(counter.get()).isEqualTo(requests);
    }

}