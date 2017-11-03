package com.commercetools.util;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Util to make simple retryable HTTP GET/POST requests.
 * <p>
 * The http client options: <ul>
 * <li>reusable, e.g. one instance per application</li>
 * <li>multi-threading</li>
 * <li>socket/request/connect timeouts are 10 sec</li>
 * <li>retries on connections exceptions up to 3 times, if request has not been sent yet
 * (see {@link DefaultHttpRequestRetryHandler#isRequestSentRetryEnabled()}
 * and {@link #HTTP_REQUEST_RETRY_ON_SOCKET_TIMEOUT})</li>
 * </ul>
 * <p>
 * This util is intended to replace <i>Unirest</i> and <i>fluent-hc</i> dependencies, which don't propose any flexible
 * way to implement retry strategy.
 */
public final class HttpRequestUtil {

    public static final int REQUEST_TIMEOUT = 10000;

    public static final int RETRY_TIMES = 3;

    /**
     * This retry handler implementation override default list of <i>nonRetriableClasses</i> excluding
     * {@link java.io.InterruptedIOException} and {@link ConnectException} so the client will retry on interruption and
     * socket timeouts.
     * <p>
     * The implementation will retry 3 times.
     */
    private static final DefaultHttpRequestRetryHandler HTTP_REQUEST_RETRY_ON_SOCKET_TIMEOUT =
            new DefaultHttpRequestRetryHandler(RETRY_TIMES, false, Arrays.asList(
                    UnknownHostException.class,
                    SSLException.class)) {
                // empty implementation, we just need to use protected constructor
            };

    private static final CloseableHttpClient CLIENT = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectionRequestTimeout(REQUEST_TIMEOUT)
                    .setSocketTimeout(REQUEST_TIMEOUT)
                    .setConnectTimeout(REQUEST_TIMEOUT)
                    .build())
            .setRetryHandler(HTTP_REQUEST_RETRY_ON_SOCKET_TIMEOUT)
            .build();

    /**
     * Execute retryable HTTP GET request with default {@link #REQUEST_TIMEOUT}
     *
     * @param url url to get
     * @return response from the {@code url}
     * @throws IOException if request failed
     */
    public static HttpResponse executeGetRequest(@Nonnull String url) throws IOException {
        return CLIENT.execute(new HttpGet(url));
    }

    /**
     * Execute retryable HTTP POST request with specified {@code timeoutMsec}
     *
     * @param url        url to post
     * @param parameters list of values to send as URL encoded form data. If <b>null</b> - not data is sent, but
     *                   empty POST request is executed.
     * @return response from the {@code url}
     * @throws IOException if request failed
     */
    public static HttpResponse executePostRequest(@Nonnull String url, @Nullable Iterable<? extends NameValuePair> parameters)
            throws IOException {
        final HttpPost request = new HttpPost(url);
        if (parameters != null) {
            request.setEntity(new UrlEncodedFormEntity(parameters));
        }

        return CLIENT.execute(request);
    }

    /**
     * Short {@link BasicNameValuePair} factory alias.
     *
     * @param name  request argument name
     * @param value request argument value. If value is <b>null</b> - {@link BasicNameValuePair#value} set to <b>null</b>,
     *              otherwise {@link Object#toString()} is applied.
     * @return new instance of {@link BasicNameValuePair} with {@code name} and {@code value}
     */
    public static BasicNameValuePair nvPair(@Nonnull final String name, @Nullable final Object value) {
        return new BasicNameValuePair(name, Objects.toString(value, null));
    }

    private HttpRequestUtil() {
    }
}
