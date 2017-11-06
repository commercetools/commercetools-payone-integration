package specs.util;

import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

import static com.commercetools.util.HttpRequestUtil.REQUEST_TIMEOUT;

@RunWith(ConcordionRunner.class)
public class HttpRequestUtilFixture {
    /**
     * Url pattern to <a href="http://slowwly.robertomurray.co.uk/">slowwly</a> http mocking resource.
     * <p>
     * <b>%d</b> is a placeholder for response delay in msec.
     */
    static final String SLOWWLY_URL_PATTERN = "http://slowwly.robertomurray.co.uk/delay/%d/url/http://google.com";

    /**
     * Reflective POST request test service
     */
    static final String HTTP_BIN_POST = "http://httpbin.org/post";

    /**
     * Not a test, used for display info.
     *
     * @return {@link com.commercetools.util.HttpRequestUtil#REQUEST_TIMEOUT}
     */
    public int getDefaultTimeout() {
        return REQUEST_TIMEOUT;
    }

}