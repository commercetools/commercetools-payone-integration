package specs.util;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

import static com.commercetools.util.HttpRequestUtil.*;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

@RunWith(ConcordionRunner.class)
public class HttpRequestUtilSuccessFixture extends HttpRequestUtilFixture {

    /**
     * Verify normal response.
     * This request has response timeout shorter than max request time in
     * {@link com.commercetools.util.HttpRequestUtil#REQUEST_TIMEOUT} thus should success, opposite to
     * {@link HttpRequestUtilTimeoutFixture#executeLongGetRequest()}
     */
    public MultiValueResult executeShortGetRequest() throws Exception {
        final int timeout = REQUEST_TIMEOUT / 2;
        final HttpResponse httpResponse = executeGetRequest(format(SLOWWLY_URL_PATTERN, timeout));

        return MultiValueResult.multiValueResult()
                .with("responseStatus", httpResponse.getStatusLine().getStatusCode())
                .with("timeout", timeout);
    }

    public MultiValueResult executePostWithArguments() throws Exception {
        HttpResponse httpResponse = executePostRequest(HTTP_BIN_POST, ImmutableList.of(
                nameValue("a", "b"),
                nameValue("hello", 22),
                nameValue("Fußgängerübergänge", "Пішохідні переходи"),
                nameValue("Їжачок", "قنفذ"),
                nameValue("инь-янь", "陰陽")));

        String responseString = responseToString(httpResponse);
        JsonObject response = new JsonParser().parse(responseString).getAsJsonObject();
        JsonObject form = response.getAsJsonObject("form");

        String request = form.entrySet().stream()
                .map(entry -> format("%s=%s", entry.getKey(), entry.getValue().getAsString()))
                .sorted()
                .collect(joining("&"));


        return MultiValueResult.multiValueResult()
                .with("responseStatus", httpResponse.getStatusLine().getStatusCode())
                .with("fullRequest", request);
    }
}