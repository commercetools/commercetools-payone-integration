package specs.util;

import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

import java.io.IOException;

import static com.commercetools.util.HttpRequestUtil.*;
import static java.lang.String.format;

@RunWith(ConcordionRunner.class)
public class HttpRequestUtilTimeoutFixture extends HttpRequestUtilFixture {

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