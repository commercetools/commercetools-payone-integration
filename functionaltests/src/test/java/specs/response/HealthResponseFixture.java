package specs.response;

import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicResponseHandler;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.json.JSONObject;
import org.junit.runner.RunWith;

import static util.HttpRequestUtil.executeFastGetRequest;

/**
 * Simple /health URL response checker
 */
@RunWith(ConcordionRunner.class)
public class HealthResponseFixture extends BasePaymentFixture {

    public String getUrl() throws Exception {
        return getHealthUrl();
    }

    public MultiValueResult handleHealthResponse() throws Exception {
        final HttpResponse httpResponse = executeFastGetRequest(getHealthUrl());
        String responseString = new BasicResponseHandler().handleResponse(httpResponse);

        return MultiValueResult.multiValueResult()
                .with("statusCode", httpResponse.getStatusLine().getStatusCode())
                .with("mimeType", ContentType.getOrDefault(httpResponse.getEntity()).getMimeType())
                .with("bodyStatus", (new JSONObject(responseString)).getInt("status"));
    }

}
