package specs.response;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

@RunWith(ConcordionRunner.class)
public class NotificationResponseFixture extends BasePaymentFixture {

    public int handleEmptyNotificationResponse() throws Exception {
        final HttpResponse httpResponse = Request.Post(getNotificationUrl())
                .connectTimeout(SIMPLE_REQUEST_TIMEOUT)
                .execute()
                .returnResponse();

        return httpResponse.getStatusLine().getStatusCode();
    }

}
