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

import static com.commercetools.pspadapter.payone.domain.payone.PayonePostServiceImpl.executeGetRequest;
import static com.commercetools.util.PayoneHttpClientUtil.responseToString;
import static java.util.Optional.ofNullable;

/**
 * Simple /health URL response checker
 */
@RunWith(ConcordionRunner.class)
public class HealthResponseFixture extends BasePaymentFixture {

    public String getUrl() {
        return getHealthUrl();
    }

    public MultiValueResult handleHealthResponse() throws Exception {
        final HttpResponse httpResponse = executeGetRequest(getHealthUrl());
        final String responseString = responseToString(httpResponse);

        final JsonObject rootNode = JsonParser.parseString(responseString).getAsJsonObject();
        final String bodyStatus = rootNode.get("status").getAsString();

        final Set<Map.Entry<String, JsonElement>> tenantNames = ofNullable(rootNode.get("tenants"))
                .map(e -> e.getAsJsonObject().entrySet())
                .orElse(Collections.emptySet());

        final Optional<JsonObject> applicationInfo =
                ofNullable(rootNode.getAsJsonObject("applicationInfo")).filter(JsonObject::isJsonObject);

        final String title = applicationInfo
            .map(node -> node.get("title"))
            .map(JsonElement::getAsString).orElse("");

        final String version = applicationInfo
            .map(node -> node.get("version"))
            .map(JsonElement::getAsString).orElse("");

        return MultiValueResult.multiValueResult()
                .with("statusCode", httpResponse.getStatusLine().getStatusCode())
                .with("mimeType", ContentType.getOrDefault(httpResponse.getEntity()).getMimeType())
                .with("bodyStatus", bodyStatus)
                .with("bodyTenants", tenantNames.toString()) // info only
                .with("bodyTenantsSize", tenantNames.size())
                .with("bodyApplicationName", title)
                .with("bodyApplicationVersion", version)
                .with("versionIsEmpty", version.isEmpty());
    }
}
