package specs.response;

import org.concordion.api.FullOGNL;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

import static util.HttpRequestUtil.executeFastGetRequest;

/**
 * {@code /${getTenantName()}/commercetools/handle/payments/} URL tests for bad IDs.
 * <p>Correct statuses are checked in {@code ResponsesFixture}</p>
 */
@RunWith(ConcordionRunner.class)
@FullOGNL // this is to allow getHandlePaymentPath(':id') call in the HTML template
public class ResponseForBadPaymentsFixture extends BasePaymentFixture {

    public int handleBadPaymentResponses(String paymentId) throws Exception {
        return executeFastGetRequest(getHandlePaymentUrl(paymentId))
                .getStatusLine()
                .getStatusCode();
    }

}
