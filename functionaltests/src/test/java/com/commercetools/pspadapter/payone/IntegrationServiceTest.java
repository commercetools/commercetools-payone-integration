package com.commercetools.pspadapter.payone;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class IntegrationServiceTest extends BaseIntegrationServiceTest {

    private static final String HEALTH_REQUEST = "/health";

    /**
     * Tests the /health URL returns valid JSON string and status
     */
    @Test
    public void returnsJsonStatus200InCaseOfHealthUrlRequested() throws Exception {
        HttpUriRequest request = new HttpGet( BASE_URL_WITH_PORT() + HEALTH_REQUEST);
        HttpResponse httpResponse = HttpClientBuilder.create().build().execute( request );

        assertThat("Response Code must be 200", httpResponse.getStatusLine().getStatusCode(), is(HttpStatus.OK_200));
        assertThat("Content Type must be JSON", ContentType.getOrDefault(httpResponse.getEntity()).getMimeType(),
                is(ContentType.APPLICATION_JSON.getMimeType()));

        String responseString = EntityUtils.toString(httpResponse.getEntity());

        assertThat("JSON Answer must have status 200", (new JSONObject(responseString)).getInt("status"),
                is(HttpStatus.OK_200));
    }
}
