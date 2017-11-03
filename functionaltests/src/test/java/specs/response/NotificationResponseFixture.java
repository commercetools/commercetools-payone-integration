package specs.response;

import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

import static com.commercetools.util.HttpRequestUtil.executePostRequest;

@RunWith(ConcordionRunner.class)
public class NotificationResponseFixture extends BasePaymentFixture {

    public int handleEmptyNotificationResponse() throws Exception {
        return executePostRequest(getNotificationUrl(), null)
                .getStatusLine()
                .getStatusCode();
    }

}
