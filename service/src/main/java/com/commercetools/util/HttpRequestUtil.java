package com.commercetools.util;

import org.apache.http.Consts;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;
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

import static java.lang.String.format;

/**
 * Util to make simple retryable HTTP GET/POST requests.
 * <p>
 * The http client options: <ul>
 * <li>reusable, e.g. one instance per application</li>
 * <li>multi-threading</li>
 * <li>socket/request/connect timeouts are 10 sec</li>
 * <li>retries on connections exceptions up to 5 times, if request has not been sent yet
 * (see {@link DefaultHttpRequestRetryHandler#isRequestSentRetryEnabled()}
 * and {@link #httpRequestRetryHandler})</li>
 * <li>connections pool is 200 connections, up to 20 per route (see {@link #CONNECTION_MAX_TOTAL}
 * and {@link #CONNECTION_MAX_PER_ROUTE}). These values are "inherited" from
 * <a href="https://github.com/Kong/unirest-java/blob/3b461599ad021d0a3f14213c0dbb85bab7244f66/src/main/java/com/mashape/unirest/http/options/Options.java#L23-L24">Unirest</a>
 * library. It could be changed in the future if we face problems (for example, decrease if we have OutOfMemory
 * or increase if out of connections from the pool.</li>
 * </ul>
 * <p>
 * This util is intended to replace <i>Unirest</i> and <i>fluent-hc</i> dependencies, which don't propose any flexible
 * way to implement retry strategy.
 * <p>
 * Implementation notes (for developers):<ul>
 * <li>remember, the responses must be closed, otherwise the connections won't be return to the pool,
 * no new connections will be available and {@link org.apache.http.conn.ConnectionPoolTimeoutException} will be
 * thrown. See {@link #executeReadAndCloseRequest(HttpUriRequest)}</li>
 * <li>{@link UrlEncodedFormEntity} the charset should be explicitly set to UTF-8, otherwise
 * {@link HTTP#DEF_CONTENT_CHARSET ISO_8859_1} is used, which is not acceptable for German alphabet</li>
 * </ul>
 */
public final class HttpRequestUtil {

    static final int TIMEOUT_TO_ESTABLISH_CONNECTION = 10000;

    static final int TIMEOUT_WHEN_CONNECTION_POOL_FULLY_OCCUPIED = 10000;

    static final int TIMEOUT_WHEN_CONTINUOUS_DATA_STREAM_DOES_NOT_REPLY = 8000;

    static final int RETRY_TIMES = 5;

    static final int CONNECTION_MAX_TOTAL = 200;

    static final int CONNECTION_MAX_PER_ROUTE = 20;

    static final int SERVICE_UNAVAILABLE_RETRY_DELAY_MILLIS = 100;

    static final Logger logger = LoggerFactory.getLogger(HttpRequestUtil.class);

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
    private static final DefaultHttpRequestRetryHandler httpRequestRetryHandler = new DefaultHttpRequestRetryHandler(
            RETRY_TIMES, REQUEST_SENT_RETRY_ENABLED, Arrays.asList(
            UnknownHostException.class,
            SSLException.class)) {

                // We need additional logging on each failure to payone endpoint and also
                // everytime we make a retry attempt
                @Override
                public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                    String payoneRequestReference = "Unknown";
                    if (context instanceof HttpCoreContext){
                        HttpCoreContext httpCoreContext = (HttpCoreContext) context;
                        Object objReference = httpCoreContext.getAttribute("reference");
                        if (objReference instanceof String) {
                            payoneRequestReference = (String) objReference;
                        }
                    }

                    logger.error(
                            format("Handle payment request with reference [%s] to payone service endpoint failed. " +
                                            "We have already retried [%d] times. We are going to retry again...",
                                    payoneRequestReference, executionCount),
                            exception);
                    return super.retryRequest(exception, executionCount, context);
                }
            };

    /**
     * ServiceUnavailableRetryStrategy represents a strategy determining whether or not the request should be
     * retried after a while in case of the service being temporarily unavailable
     */
    static final ServiceUnavailableRetryStrategy serviceUnavailableRetryStrategy = new ServiceUnavailableRetryStrategy() {

        int waitPeriod = SERVICE_UNAVAILABLE_RETRY_DELAY_MILLIS;

        @Override
        public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
            waitPeriod *= 2;
            String payoneRequestReference = "Unknown";
            if (context instanceof HttpCoreContext){
                HttpCoreContext httpCoreContext = (HttpCoreContext) context;
                Object objReference = httpCoreContext.getAttribute("reference");
                if (objReference instanceof String) {
                    payoneRequestReference = (String) objReference;
                }
            }
            logger.error(
                    format("Payone service endpoint is unavailable! Payone reference [%s], Received HTTP Code: [%d]. " +
                                    "We have already retried [%d] times. We are going to retry again...",
                            payoneRequestReference, response.getStatusLine().getStatusCode(), executionCount));
            return executionCount <= RETRY_TIMES;
        }

        @Override
        public long getRetryInterval() {
            return waitPeriod;
        }
    };

    private static final BasicResponseHandler BASIC_RESPONSE_HANDLER = new BasicResponseHandler();

    private static final ConnectionKeepAliveStrategy keepAliveStrategy = (response, context) -> {
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

    private static final CloseableHttpClient CLIENT = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectionRequestTimeout(TIMEOUT_WHEN_CONNECTION_POOL_FULLY_OCCUPIED)
                    .setSocketTimeout(TIMEOUT_WHEN_CONTINUOUS_DATA_STREAM_DOES_NOT_REPLY)
                    .setConnectTimeout(TIMEOUT_TO_ESTABLISH_CONNECTION)
                    .build())
            .setRetryHandler(httpRequestRetryHandler)
            .setServiceUnavailableRetryStrategy(serviceUnavailableRetryStrategy)
            .setKeepAliveStrategy(keepAliveStrategy)
            .setConnectionManager(buildDefaultConnectionManager())
            .build();

    private HttpRequestUtil() {
    }

    private static PoolingHttpClientConnectionManager buildDefaultConnectionManager() {
        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(CONNECTION_MAX_TOTAL);
        connectionManager.setValidateAfterInactivity(100);
        connectionManager.setDefaultMaxPerRoute(CONNECTION_MAX_PER_ROUTE);

        return connectionManager;
    }

    /**
     * Execute retryable HTTP GET request with default {@link #TIMEOUT_TO_ESTABLISH_CONNECTION}
     *
     * @param url url to get
     * @return response from the {@code url}
     * @throws IOException in case of a problem or the connection was aborted
     */
    public static HttpResponse executeGetRequest(@Nonnull String url) throws IOException {
        return executeReadAndCloseRequest(new HttpGet(url));
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
     * Execute retryable HTTP POST request with specified {@code timeoutMsec}
     *
     * @param url        url to post
     * @param parameters list of values to send as URL encoded form data. If <b>null</b> - not data is sent, but
     *                   empty POST request is executed.
     * @return response from the {@code url}
     * @throws IOException in case of a problem or the connection was aborted
     */
    public static HttpResponse executePostRequest(@Nonnull String url, @Nullable Iterable<? extends NameValuePair>
            parameters)
            throws IOException {
        final HttpPost request = new HttpPost(url);
        if (parameters != null) {
            request.setEntity(new UrlEncodedFormEntity(parameters, Consts.UTF_8));
        }

        return executeReadAndCloseRequest(request);
    }

    /**
     * Make URL request and return a response string.
     *
     * @param url        URL to post/query
     * @param parameters list of values to send as URL encoded form data. If <b>null</b> - not data is sent, but
     *                   empty POST request is executed.
     * @return response string from the request
     * @throws IOException in case of a problem or the connection was aborted
     */
    public static String executePostRequestToString(@Nonnull String url, @Nullable Iterable<? extends NameValuePair>
            parameters)
            throws IOException {
        return responseToString(executePostRequest(url, parameters));
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

    /**
     * By default apache httpclient responses are not closed, thus we should explicitly read the stream and close the
     * connection.
     * <p>
     * The connection will be closed even if read exception occurs.
     *
     * @param request GET/POST/other request to execute, read and close
     * @return read and closed {@link CloseableHttpResponse} instance from
     * {@link CloseableHttpClient#execute(HttpUriRequest)}
     * where {@link HttpResponse#getEntity()} is set to read string value.
     * @throws IOException if reading failed. Note, even in case of exception the {@code response} will be closed.
     */
    private static CloseableHttpResponse executeReadAndCloseRequest(@Nonnull final HttpUriRequest request) throws
            IOException {
        final CloseableHttpResponse response = CLIENT.execute(request);
        try {
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                final ByteArrayEntity byteArrayEntity = new ByteArrayEntity(EntityUtils.toByteArray(entity));
                final ContentType contentType = ContentType.getOrDefault(entity);
                byteArrayEntity.setContentType(contentType.toString());
                response.setEntity(byteArrayEntity);
            }
        } finally {
            response.close();
        }

        return response;
    }
}
