package specs.response;

import org.apache.http.client.fluent.Request;
import org.concordion.api.FullOGNL;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

/**
 * {@code /${getTenantName()}/commercetools/handle/payments/} URL tests for bad IDs.
 * <p>Correct statuses are checked in {@code ResponsesFixture}</p>
 */
@RunWith(ConcordionRunner.class)
@FullOGNL // this is to allow getHandlePaymentPath(':id') call in the HTML template
public class ResponseForBadPaymentsFixture extends BasePaymentFixture {

    public int handleBadPaymentResponses(String paymentId) throws Exception {
        return Request.Get(getHandlePaymentUrl(paymentId))
                .connectTimeout(SIMPLE_REQUEST_TIMEOUT)
                .execute()
                .returnResponse()
                .getStatusLine()
                .getStatusCode();
    }

}
