package com.commercetools.util;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;

import static com.commercetools.pspadapter.payone.domain.payone.PayonePostServiceImpl.executeGetRequest;
import static java.lang.String.format;

/**
 * Util to make simple retryable HTTP GET/POST requests for Payone Http Client.
 */
public final class PayoneHttpClientUtil {

    public static final int TIMEOUT_TO_ESTABLISH_CONNECTION = 10000;

    public static final int TIMEOUT_WHEN_CONNECTION_POOL_FULLY_OCCUPIED = 10000;

    public static final int TIMEOUT_WHEN_CONTINUOUS_DATA_STREAM_DOES_NOT_REPLY = 5000;

    static final int RETRY_TIMES = 5;

    static final int CONNECTION_MAX_TOTAL = 200;

    static final int CONNECTION_MAX_PER_ROUTE = 20;

    static final int SERVICE_UNAVAILABLE_RETRY_DELAY_MILLIS = 100;

    static final Logger logger = LoggerFactory.getLogger(PayoneHttpClientUtil.class);

    /**
     * Don't resend request on connection exception once it has been successfully sent.
     */
    static final boolean REQUEST_SENT_RETRY_ENABLED = false;
    /**
     * This retry handler implementation overrides default list of <i>nonRetriableClasses</i> excluding
     * {@link java.io.InterruptedIOException} and {@link ConnectException} so the client will retry on interruption and
     * socket timeouts.
     * <p>
     * The implementation will retry 5 times.
     */
    public static final DefaultHttpRequestRetryHandler httpRequestRetryHandler = new DefaultHttpRequestRetryHandler(
            RETRY_TIMES, REQUEST_SENT_RETRY_ENABLED, Arrays.asList(
            UnknownHostException.class,
            SSLException.class)) {

                // We need additional logging on each failure to payone endpoint and also
                // everytime we make a retry attempt
                @Override
                public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                   boolean isRetryable = super.retryRequest(exception, executionCount, context);
                   if(isRetryable) {
                       logger.error(
                               format("Handle payment request to payone service endpoint failed. " +
                                               "We have already retried [%d] times. We are going to retry again...",
                                       executionCount - 1),
                               exception);
                   }
                    return isRetryable;
                }
            };

    /**
     * ServiceUnavailableRetryStrategy represents a strategy determining whether or not the request should be
     * retried after a while in case of the service being temporarily unavailable
     */
    public static final ServiceUnavailableRetryStrategy serviceUnavailableRetryStrategy = new ServiceUnavailableRetryStrategy() {

        int waitPeriod = SERVICE_UNAVAILABLE_RETRY_DELAY_MILLIS;

        @Override
        public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
            waitPeriod *= 2;
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 500) {
                logger.error(
                        format("Payone service endpoint is unavailable! Received HTTP Code: [%d]. " +
                                        "We have already retried [%d] times. We are going to retry again...",
                                statusCode, executionCount));
            }
            return executionCount <= RETRY_TIMES && statusCode >= 500;
        }

        @Override
        public long getRetryInterval() {
            return waitPeriod;
        }
    };

    private static final BasicResponseHandler BASIC_RESPONSE_HANDLER = new BasicResponseHandler();

    public static final ConnectionKeepAliveStrategy keepAliveStrategy = (response, context) -> {
        // Honor 'keep-alive' header
        HeaderElementIterator it = new BasicHeaderElementIterator(
                response.headerIterator(HTTP.CONN_KEEP_ALIVE));
        while (it.hasNext()) {
            HeaderElement he = it.nextElement();
            String param = he.getName();
            String value = he.getValue();
            if (value != null && param.equalsIgnoreCase("timeout")) {
                try {
                    return Long.parseLong(value) * 500;
                } catch (NumberFormatException ignore) {
                }
            }
        }
        // Keep alive for 2.5 seconds only
        return 2500;
    };

    private PayoneHttpClientUtil() {
    }

    public static PoolingHttpClientConnectionManager buildDefaultConnectionManager() {
        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(CONNECTION_MAX_TOTAL);
        connectionManager.setValidateAfterInactivity(100);
        connectionManager.setDefaultMaxPerRoute(CONNECTION_MAX_PER_ROUTE);

        return connectionManager;
    }


    /**
     * Make URL request and return a response string.
     *
     * @param url URL to get/query
     * @return response string from the request
     * @throws IOException in case of a problem or the connection was aborted
     */
    public static String executeGetRequestToString(@Nonnull String url) throws IOException {
        return responseToString(executeGetRequest(url));
    }

    /**
     * Read string content from {@link HttpResponse}.
     *
     * @param response response to read
     * @return string content of the response
     * @throws IOException in case of a problem or the connection was aborted
     */
    public static String responseToString(@Nonnull final HttpResponse response) throws IOException {
        return BASIC_RESPONSE_HANDLER.handleResponse(response);
    }

    /**
     * Short {@link BasicNameValuePair} factory alias.
     *
     * @param name  request argument name
     * @param value request argument value. If value is <b>null</b> - {@link BasicNameValuePair#value} set to
     *              <b>null</b>,
     *              otherwise {@link Object#toString()} is applied.
     * @return new instance of {@link BasicNameValuePair} with {@code name} and {@code value}
     */
    public static BasicNameValuePair nameValue(@Nonnull final String name, @Nullable final Object value) {
        return new BasicNameValuePair(name, Objects.toString(value, null));
    }

}
