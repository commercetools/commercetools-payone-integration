package specs.response;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.util.HttpRequestUtil.executeGetRequest;
import static com.commercetools.util.HttpRequestUtil.responseToString;
import static java.util.Optional.ofNullable;

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

        JsonObject rootNode = parser.parse(responseString).getAsJsonObject();
        String bodyStatus = rootNode.get("status").getAsString();
        Optional<JsonObject> applicationInfo =
                ofNullable(rootNode.getAsJsonObject("applicationInfo")).filter(JsonObject::isJsonObject);
        String title = applicationInfo.map(node -> node.get("title")).map(JsonElement::getAsString).orElse("");
        String version = applicationInfo.map(node -> node.get("version")).map(JsonElement::getAsString).orElse("");

        return MultiValueResult.multiValueResult()
                .with("statusCode", httpResponse.getStatusLine().getStatusCode())
                .with("mimeType", ContentType.getOrDefault(httpResponse.getEntity()).getMimeType())
                .with("bodyStatus", bodyStatus)
                .with("bodyApplicationName", title)
                .with("bodyApplicationVersion", version)
                .with("versionIsEmpty", version.isEmpty());
    }
}
