package specs.response;

import org.apache.http.client.fluent.Request;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

/**
 * {@code /commercetools/handle/payments/} URL tests for bad IDs.
 * <p>Correct statuses are checked in {@code ResponsesFixture}</p>
 */
@RunWith(ConcordionRunner.class)
public class ResponseForBadPaymentsFixture extends BasePaymentFixture {

    public int handleBadPaymentResponses(String paymentId) throws Exception {
        return Request.Get(getHandlePaymentUrl(paymentId))
                .connectTimeout(200)
                .execute()
                .returnResponse()
                .getStatusLine()
                .getStatusCode();
    }

}
