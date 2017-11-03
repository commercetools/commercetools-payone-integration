package specs.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicResponseHandler;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

/**
 * Simple /health URL response checker
 */
@RunWith(ConcordionRunner.class)
public class HealthResponseFixture extends BasePaymentFixture {

    public String getUrl() throws Exception {
        return getHealthUrl();
    }

    public MultiValueResult handleHealthResponse() throws Exception {
        HttpResponse httpResponse = Request.Get(getHealthUrl())
                .connectTimeout(SIMPLE_REQUEST_TIMEOUT)
                .execute()
                .returnResponse();

        String responseString = new BasicResponseHandler().handleResponse(httpResponse);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(responseString);
        Optional<JsonNode> tenantNames = ofNullable(jsonNode.get("tenants")).filter(JsonNode::isArray);
        Optional<JsonNode> applicationInfo = ofNullable(jsonNode.get("applicationInfo")).filter(JsonNode::isObject);
        String title = applicationInfo.map(node -> node.get("title")).map(JsonNode::textValue).orElse("");
        String version = applicationInfo.map(node -> node.get("version")).map(JsonNode::textValue).orElse("");

        return MultiValueResult.multiValueResult()
                .with("statusCode", httpResponse.getStatusLine().getStatusCode())
                .with("mimeType", ContentType.getOrDefault(httpResponse.getEntity()).getMimeType())
                .with("bodyStatus", jsonNode.get("status"))
                .with("bodyTenants", tenantNames.map(JsonNode::toString).orElse("undefined"))
                .with("bodyTenantsSize", tenantNames.map(JsonNode::size).orElse(0))
                .with("bodyApplicationName", title)
                .with("bodyApplicationVersion", version)
                .with("versionIsEmpty", version.isEmpty());
    }

    /**
     * Join with "+" tenant names in alphabetical order
     * @param jsonObject
     * @return
     */
    private static String concatTenantNames(final JsonNode jsonObject) {
        return ofNullable(jsonObject)
                .filter(JsonNode::isArray)
                .map(arr -> IntStream.range(0, arr.size())
                        .mapToObj(arr::get)
                        .filter(Objects::nonNull)
                        .map(JsonNode::textValue)
                        .sorted()
                        .collect(joining("+")))
                .orElse("");

    }

}
