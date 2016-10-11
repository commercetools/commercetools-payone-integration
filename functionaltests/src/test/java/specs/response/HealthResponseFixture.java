package specs.response;

import com.google.common.collect.ImmutableMap;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicResponseHandler;
import org.concordion.integration.junit4.ConcordionRunner;
import org.json.JSONObject;
import org.junit.runner.RunWith;

import java.util.Map;

/**
 * Simple /health URL response checker
 */
@RunWith(ConcordionRunner.class)
public class HealthResponseFixture extends BasePaymentFixture {

    public Map<String, Object> handleHealthResponse() throws Exception {

        final HttpResponse httpResponse = Request.Get(getHealthUrl())
                .connectTimeout(200)
                .execute()
                .returnResponse();

        String responseString = new BasicResponseHandler().handleResponse(httpResponse);

        return ImmutableMap.of("statusCode", httpResponse.getStatusLine().getStatusCode(),
                "mimeType", ContentType.getOrDefault(httpResponse.getEntity()).getMimeType(),
                "bodyStatus",(new JSONObject(responseString)).getInt("status"));
    }

}
