package util;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Test util to make simple HTTP GET/POST requests.
 * <p>
 * By default the requests are done with long response timeout {@link #REQUEST_TIMEOUT}, but fast versions could be used
 * with {@link #FAST_REQUEST_TIMEOUT}
 */
public final class HttpRequestUtil {

    // looks like heroku may have some lags, so we use 10 seconds to avoid false test fails because of timeouts
    private static final int REQUEST_TIMEOUT = 10000;

    // timeout for simple requests, like health or malformed id, where heavy processing is not expected
    private static final int FAST_REQUEST_TIMEOUT = 2000;

    public static HttpResponse executeGetRequest(@Nonnull String url) throws IOException {
        return executeGetRequest(url, REQUEST_TIMEOUT);
    }

    public static HttpResponse executeGetRequest(@Nonnull String url, int timeoutMsec) throws IOException {
        return createHttpClient(timeoutMsec).execute(new HttpGet(url));
    }

    public static HttpResponse executeFastGetRequest(@Nonnull String url) throws IOException {
        return createHttpClient(FAST_REQUEST_TIMEOUT).execute(new HttpGet(url));
    }

    public static HttpResponse executePostRequest(@Nonnull String url) throws IOException {
        return executePostRequest(url, REQUEST_TIMEOUT);
    }

    public static HttpResponse executePostRequest(@Nonnull String url, int timeoutMsec) throws IOException {
        return createHttpClient(timeoutMsec).execute(new HttpPost(url));
    }

    public static HttpResponse executeFastPostRequest(@Nonnull String url) throws IOException {
        return executePostRequest(url, FAST_REQUEST_TIMEOUT);
    }

    private static CloseableHttpClient createHttpClient(int timeoutMsec) {
        return HttpClientBuilder.create()
                .setConnectionTimeToLive(timeoutMsec, TimeUnit.MILLISECONDS)
                .build();
    }

    private HttpRequestUtil() {
    }
}
