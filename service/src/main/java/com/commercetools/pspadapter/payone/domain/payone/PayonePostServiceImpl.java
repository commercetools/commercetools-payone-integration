package com.commercetools.pspadapter.payone.domain.payone;

import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.commercetools.pspadapter.payone.domain.payone.model.common.BaseRequest;
import com.commercetools.util.PayoneHttpClientUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.commercetools.util.PayoneHttpClientUtil.nameValue;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * <p>
 * The http client options: <ul>
 * <li>reusable, e.g. one instance per application</li>
 * <li>multi-threading</li>
 * <li>socket/request/connect timeouts are 10 sec</li>
 * <li>retries on connections exceptions up to 5 times, if request has not been sent yet
 * (see {@link DefaultHttpRequestRetryHandler#isRequestSentRetryEnabled()}
 * and {@link PayoneHttpClientUtil#httpRequestRetryHandler})</li>
 * <li>connections pool is 200 connections, up to 20 per route (see {@link PayoneHttpClientUtil#CONNECTION_MAX_TOTAL}
 * and {@link PayoneHttpClientUtil#CONNECTION_MAX_PER_ROUTE}). These values are "inherited" from
 * <a href="https://github.com/Kong/unirest-java/blob/3b461599ad021d0a3f14213c0dbb85bab7244f66/src/main/java/com/mashape/unirest/http/options/Options.java#L23-L24">Unirest</a>
 * library. It could be changed in the future if we face problems (for example, decrease if we have OutOfMemory
 * or increase if out of connections from the pool.</li>
 * </ul>
 * <p>
 * This service is intended to replace <i>Unirest</i> and <i>fluent-hc</i> dependencies, which don't propose any flexible
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

public class PayonePostServiceImpl implements PayonePostService {

    private static final String ENCODING_UTF8 = "UTF-8";

    private String serverAPIURL;

    private static final CloseableHttpClient PAYONE_HTTP_CLIENT = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectionRequestTimeout(
                            PayoneHttpClientUtil.TIMEOUT_WHEN_CONNECTION_POOL_FULLY_OCCUPIED)
                    .setSocketTimeout(
                            PayoneHttpClientUtil.TIMEOUT_WHEN_CONTINUOUS_DATA_STREAM_DOES_NOT_REPLY)
                    .setConnectTimeout(PayoneHttpClientUtil.TIMEOUT_TO_ESTABLISH_CONNECTION)
                    .build())
            .setRetryHandler(PayoneHttpClientUtil.httpRequestRetryHandler)
            .setServiceUnavailableRetryStrategy(PayoneHttpClientUtil.serviceUnavailableRetryStrategy)
            .setKeepAliveStrategy(PayoneHttpClientUtil.keepAliveStrategy)
            .setConnectionManager(PayoneHttpClientUtil.buildDefaultConnectionManager())
            .build();

    private PayonePostServiceImpl(final String serverAPIURL) {
        if(StringUtils.isBlank(serverAPIURL)) {
            throw new IllegalArgumentException("The server api url must not be null or empty.");
        }
        this.serverAPIURL = serverAPIURL;
    }

    /**
     * Initialize new service with url.
     *
     * @param payoneServerApiUrl - the payone server api url, must not be null or empty
     * @return new instance of PayonePostServiceImpl.class
     * @throws IllegalArgumentException if the provided {@code payoneServerApiUrl} is invalid
     */
    public static PayonePostServiceImpl of(final String payoneServerApiUrl) throws IllegalArgumentException {
        return new PayonePostServiceImpl(payoneServerApiUrl);
    }

    @Override
    public Map<String, String> executePost(final BaseRequest baseRequest) throws PayoneException {

        try {
            final List<BasicNameValuePair> mappedListParameters =
                    getNameValuePairsWithExpandedLists(baseRequest.toStringMap(false));
            final String serverResponse = executePostRequestToString(this.serverAPIURL, mappedListParameters);

            return buildMapFromResultParams(serverResponse);
        } catch (Exception e) {
            final String requestBody =
                getNameValuePairsWithExpandedLists(baseRequest.toStringMap(true)).toString();
            final String exceptionMessage = format("Payone POST request with body (%s) failed.", requestBody);
            throw new PayoneException(exceptionMessage, e);
        }
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
    public static String executePostRequestToString(@Nonnull String url,
                                                    @Nullable Iterable<? extends NameValuePair> parameters)
            throws IOException {

        return PayoneHttpClientUtil.responseToString(executePostRequest(url, parameters));
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
    public static HttpResponse executePostRequest(@Nonnull String url,
                                                  @Nullable Iterable<? extends NameValuePair> parameters)
            throws IOException {

        final HttpPost request = new HttpPost(url);
        if (parameters != null) {
            request.setEntity(new UrlEncodedFormEntity(parameters, Consts.UTF_8));
        }

        return executeReadAndCloseRequest(request);
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
    private static CloseableHttpResponse executeReadAndCloseRequest(@Nonnull final HttpUriRequest request)
            throws IOException {

        final CloseableHttpResponse response = PAYONE_HTTP_CLIENT.execute(request);
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

    /**
     * Execute retryable HTTP GET request with default
     * {@link PayoneHttpClientUtil#TIMEOUT_TO_ESTABLISH_CONNECTION}
     *
     * @param url url to get
     * @return response from the {@code url}
     * @throws IOException in case of a problem or the connection was aborted
     */
    public static HttpResponse executeGetRequest(@Nonnull String url) throws IOException {
        return executeReadAndCloseRequest(new HttpGet(url));
    }

    /**
     * Expand entries with list values to key-value pairs where the keys are transformed to set of {@code key[i]} with
     * respective values from the list. Entries with map values are mapped to 'paydata', like add_paydata[key] =
     * mapped value.
     * All other arguments remain the same.
     * <p>
     * The indices origin is <b>1</b>.
     * <p>
     * <b>Note:</b> so far only {@link List} values are expanded, arrays behavior is still undefined.
     * <p>
     * Example:
     * <pre> {"a": [5, 7], "b": "c", "d": ["e", "f"]} =>
     * {"a[1]": 5, "a[2]": 7, "b": "c", "d[1]": "e", "d[2]": "f"}
     * </pre>
     *
     * @param parameters Set of key-value pairs to expand
     * @return new {@link List} with respective {@link BasicNameValuePair} values from {@code parameters}.
     * Note: the items order in the list is undefined.
     */
    @Nonnull
    List<BasicNameValuePair> getNameValuePairsWithExpandedLists(Map<String, Object> parameters) {
        return parameters.entrySet().stream()
                .flatMap(entry -> {
                    Object value = entry.getValue();
                    if (value instanceof List) {
                        final List list = (List) value;
                        if (list.size() > 0) {
                            return IntStream.range(0, list.size())
                                    .mapToObj(i -> nameValue(entry.getKey() + "[" + (i + 1) + "]", list.get(i)));
                        } else {
                            return Stream.of(nameValue(entry.getKey() + "[]", ""));
                        }
                    }
                    else if (value instanceof Map) {
                        final Map<String, String> payData = (Map<String, String>) value;
                        Stream.Builder<BasicNameValuePair> streamBuilder = Stream.builder();
                        payData.forEach((key, val) -> streamBuilder.add(nameValue("add_paydata["+key+"]", val)));
                        return streamBuilder.build();
                    }


                    return Stream.of(nameValue(entry.getKey(), value));

                })
                .collect(toList());
    }

    Map<String, String> buildMapFromResultParams(final String serverResponse) throws UnsupportedEncodingException {
        Map<String, String> resultMap = new HashMap<>();

        String[] properties = serverResponse.split("\\r?\\n");
        for (String property : properties) {
            String[] param = StringUtils.split(property, "=", 2);
            if (param != null && param.length > 1) {
                // Attention: the Redirect-param should not be decoded, otherwise the redirect will not work
                resultMap.put(
                        param[0],
                        StringUtils.equalsIgnoreCase("redirecturl", param[0]) ? param[1] : URLDecoder.decode(param[1],
                                ENCODING_UTF8));
            }
        }
        return resultMap;
    }

    public String getServerAPIURL() {
        return serverAPIURL;
    }

    public void setServerAPIURL(String serverAPIURL) {
        this.serverAPIURL = serverAPIURL;
    }

}
