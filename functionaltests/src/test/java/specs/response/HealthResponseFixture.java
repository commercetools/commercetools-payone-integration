package specs.response;

import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

import static com.commercetools.util.HttpRequestUtil.executeGetRequest;
import static com.commercetools.util.HttpRequestUtil.responseToString;

/**
 * Simple /health URL response checker
 */
@RunWith(ConcordionRunner.class)
public class HealthResponseFixture extends BasePaymentFixture {

    public String getUrl() throws Exception {
        return getHealthUrl();
    }

    public MultiValueResult handleHealthResponse() throws Exception {
        HttpResponse httpResponse = executeGetRequest(getHealthUrl());
        String responseString = responseToString(httpResponse);

        JsonParser parser = new JsonParser();

        return MultiValueResult.multiValueResult()
                .with("statusCode", httpResponse.getStatusLine().getStatusCode())
                .with("mimeType", ContentType.getOrDefault(httpResponse.getEntity()).getMimeType())
                .with("bodyStatus", parser.parse(responseString).getAsJsonObject().getAsJsonPrimitive("status").getAsString());
    }

}
