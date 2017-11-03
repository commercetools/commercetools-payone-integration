package specs.util;

import com.commercetools.util.HttpRequestUtil;
import org.apache.http.HttpResponse;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

import java.io.IOException;

import static com.commercetools.util.HttpRequestUtil.*;
import static java.lang.String.format;

@RunWith(ConcordionRunner.class)
public class HttpRequestUtilFixture {

    /**
     * Url pattern to <a href="http://slowwly.robertomurray.co.uk/">slowwly</a> http mocking resource.
     * <p>
     * <b>%d</b> is a placeholder for response delay in msec.
     */
    private static final String SLOWWLY_URL_PATTERN = "http://slowwly.robertomurray.co.uk/delay/%d/url/http://google.com";

    /**
     * Not a test, used for display info.
     * @return {@link HttpRequestUtil#REQUEST_TIMEOUT}
     */
    public int getDefaultTimeout() {
        return REQUEST_TIMEOUT;
    }

    /**
     * Verify normal response
     */
    public MultiValueResult executeShortGetRequest() throws Exception {
        final int timeout = REQUEST_TIMEOUT / 2;
        final HttpResponse httpResponse = executeGetRequest(format(SLOWWLY_URL_PATTERN, timeout));

        return MultiValueResult.multiValueResult()
                .with("responseStatus", httpResponse.getStatusLine().getStatusCode())
                .with("timeout", timeout);
    }

    /**
     * Verify that if request timeout is too long - the client will retry 3 times.
     */
    public MultiValueResult executeLongGetRequest() throws Exception {
        final int timeout = REQUEST_TIMEOUT * 2;
        boolean ioExceptionCaught = false;
        final int minimalFullRequestDuration = REQUEST_TIMEOUT * (RETRY_TIMES + 1);

        final long start = System.currentTimeMillis();
        try {
            executeGetRequest(format(SLOWWLY_URL_PATTERN, timeout));
        } catch (IOException e) {
            ioExceptionCaught = true;
        }
        long executionTime = System.currentTimeMillis() - start;

        return MultiValueResult.multiValueResult()
                .with("timeout", timeout)
                .with("ioExceptionCaught", ioExceptionCaught)
                .with("executionTime", executionTime)
                .with("executionTimeIs4Times", executionTime >= minimalFullRequestDuration)
                .with("defaultRequestTimeout", REQUEST_TIMEOUT)
                .with("retryTimes", RETRY_TIMES);
    }
}