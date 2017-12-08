package com.commercetools.util;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.commercetools.util.HttpRequestUtil.*;
import static com.commercetools.util.HttpRequestUtilTest.*;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.http.HttpStatus.SC_REQUEST_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stress test {@link HttpRequestUtil}: make a lot of parallel requests with timeouts + test <i>toString</i> methods.
 * <p>
 * <b>Note:</b> These tests are based on request/response from <a href="http://httpbin.org/">http://httpbin.org/</a>,
 * thus they may rarely fail if the service is out of order.
 */
public class HttpRequestUtilParallelTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestUtilParallelTest.class);

    // try to make 200 simultaneous requests in 200 threads
    private final int nThreads = CONNECTION_MAX_TOTAL;
    private final int nRequests = CONNECTION_MAX_TOTAL;

    @Test
    public void executeGetRequest_shouldBeParalleled() throws Exception {
        ExceptionalSupplier<HttpResponse> getRequestSupplier = () -> executeGetRequest(HTTP_HTTPBIN_ORG_GET);

        // in get requests asserted only response status
        ExceptionalConsumer<HttpResponse> getResponseAssertor =
                httpResponse -> assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);

        makeAndAssertParallelRequests(nThreads, nRequests, createRequestsValidator(getRequestSupplier, getResponseAssertor));
    }

    @Test
    public void executePostRequest_shouldBeParalleled() throws Exception {
        ExceptionalSupplier<HttpResponse> postRequestSupplier = () -> executePostRequest(HTTP_HTTPBIN_ORG_POST, asList(
                nameValue("xxx", "yyy"),
                nameValue("zzz", 11223344)));

        // in post requests asserted status and body
        ExceptionalConsumer<HttpResponse> postResponseAssertor = httpResponse -> {
            assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            assertThat(responseToString(httpResponse)).contains("xxx", "yyy", "zzz", "11223344");
        };

        makeAndAssertParallelRequests(nThreads, nRequests, createRequestsValidator(postRequestSupplier, postResponseAssertor));
    }

    /**
     * Function to create request/response supplier for GET/POST tests and assert the received response. The result of
     * this function is used as a supplier for {@link #makeAndAssertParallelRequests(int, int, Supplier)}.
     * <p>
     * The function implementation takes care about {@code 408 Timeout} response from
     * <a href="http://httpbin.org">http://httpbin.org</a>: if such response is received
     * (when the service is overloaded) - the implementation makes a retry up to 3 times. This is important to avoid
     * false failures of the tests when <a href="http://httpbin.org">http://httpbin.org</a> is overloaded.
     *
     * @param responseSupplier       supplier which when executed makes single blocking request and returns a response.
     * @param responseAssertConsumer procedure to assert the response obtained from {@code responseSupplier}
     * @return new supplier which when finished successfully - returns <b>1</b>, otherwise - <b>0</b>.
     */
    private Supplier<Integer> createRequestsValidator(final ExceptionalSupplier<HttpResponse> responseSupplier,
                                                      final ExceptionalConsumer<HttpResponse> responseAssertConsumer) {
        return () -> {
            try {
                int statusCode;
                int repeatedTimes = 0;
                HttpResponse httpResponse;
                do {
                    if (repeatedTimes > 0) {
                        LOGGER.warn("{} \"Request Timeout\" received from [{}]. Retry {} of {} ...",
                                SC_REQUEST_TIMEOUT, HTTP_HTTPBIN_ORG, repeatedTimes, RETRY_TIMES);
                    }
                    httpResponse = responseSupplier.get();
                    statusCode = httpResponse.getStatusLine().getStatusCode();
                    repeatedTimes++;
                    // sometimes httpbin.org returns 408 (timeout) response. Note, this is not a InterruptedIOException
                    // or ConnectException, thus DefaultHttpRequestRetryHandler re-try won't handle this case.
                    // That's why in the tests we use this approach to re-try if the server responded successfully,
                    // but with "408 timeout" response
                } while (statusCode == SC_REQUEST_TIMEOUT && repeatedTimes <= RETRY_TIMES);

                assertThat(repeatedTimes)
                        .withFailMessage(format("Retries over-limit: the post request is executed %d times with timeout response %d",
                                repeatedTimes, SC_REQUEST_TIMEOUT))
                        .isLessThanOrEqualTo(RETRY_TIMES);

                responseAssertConsumer.accept(httpResponse);
                return 1;
            } catch (Exception e) {
                LOGGER.error("Request exception: ", e);
            }

            return 0;
        };
    }

    /**
     * Execute and count {@code nRequests} simultaneous (parallel) requests in {@code nThreads} generating them
     * from {@code requestSupplier}. At the end assert that all {@code nRequests} finished successfully, i.e.
     * exception or timeout not happened.
     *
     * @param nThreads         number of parallel requests/threads
     * @param nRequests        total number of requests to execute
     * @param requestValidator requests validator: generates request, validate response  and return <b>1</b> if
     *                         the request is finished successfully or <b>0</b> otherwise
     * @throws Exception in case of thread pool exceptions
     */
    private void makeAndAssertParallelRequests(final int nThreads, final int nRequests,
                                               final Supplier<Integer> requestValidator) throws Exception {
        final ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(nThreads);

        final AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < nRequests; i++) {
            newFixedThreadPool.execute(() -> counter.addAndGet(requestValidator.get()));
        }

        newFixedThreadPool.shutdown();

        // length of the longest pipe in the multi thread pipeline,
        // literally it is an maximum integer number of sequential requests in one pipe (thread)
        final int criticalPathLength = (int) Math.ceil((double) nRequests / nThreads);

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

        assertThat(counter.get())
                .withFailMessage(format("Incorrect number of finished requests: expected %d, executed %d. " +
                        "Please see the standard error logs (stderr) for exceptions", counter.get(), nRequests))
                .isEqualTo(nRequests);
    }

    @FunctionalInterface
    private interface ExceptionalSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    public interface ExceptionalConsumer<T> {
        void accept(T t) throws Exception;

    }
}