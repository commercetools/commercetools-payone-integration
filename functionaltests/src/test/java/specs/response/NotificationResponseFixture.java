package specs.response;

import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

import static util.HttpRequestUtil.executeFastPostRequest;

@RunWith(ConcordionRunner.class)
public class NotificationResponseFixture extends BasePaymentFixture {

    public int handleEmptyNotificationResponse() throws Exception {
        return executeFastPostRequest(getNotificationUrl())
                .getStatusLine()
                .getStatusCode();
    }

}
