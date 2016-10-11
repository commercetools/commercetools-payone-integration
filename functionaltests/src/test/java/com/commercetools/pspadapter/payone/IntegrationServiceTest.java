package com.commercetools.pspadapter.payone;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class IntegrationServiceTest extends BaseIntegrationServiceTest {

    private static final String HEALTH_REQUEST = "/health";
    private static final String PAYMENT_REQUEST = "/commercetools/handle/payments/";
    private static final String NOTIFICATION_REQUEST = "/payone/notification";

    /**
     * Tests the /health URL returns valid JSON string and status
     */
    @Test
    public void testHealthUrlResponse() throws Exception {
        HttpGet request = new HttpGet( BASE_URL_WITH_PORT() + HEALTH_REQUEST);
        HttpResponse httpResponse = HttpClientBuilder.create().build().execute( request );

        assertThat("Response Code must be 200", httpResponse.getStatusLine().getStatusCode(), is(HttpStatus.OK_200));
        assertThat("Content Type must be JSON", ContentType.getOrDefault(httpResponse.getEntity()).getMimeType(),
                is(ContentType.APPLICATION_JSON.getMimeType()));

        String responseString = new BasicResponseHandler().handleResponse(httpResponse);

        assertThat("JSON Answer must have status 200", (new JSONObject(responseString)).getInt("status"),
                is(HttpStatus.OK_200));
    }

    /**
     * Tests the /commercetools/handle/payments/ URL returns proper answers for bad request UUID.
     * {@link IntegrationService#handlePayment(String)} function itself is tested in the unit tests
     */
    @Test
    public void testPaymentUrlBadRequests() throws Exception {
        HttpGet request = new HttpGet( BASE_URL_WITH_PORT() + PAYMENT_REQUEST + "some-dummy-id");
        HttpResponse httpResponse = HttpClientBuilder.create().build().execute( request );
        assertThat("Response Code must be 400", httpResponse.getStatusLine().getStatusCode(),
                is(HttpStatus.BAD_REQUEST_400));

        // just non-existent, but well formed UUID
        request = new HttpGet( BASE_URL_WITH_PORT() + PAYMENT_REQUEST + "00000000-0000-0000-0000-000000000000");
        httpResponse = HttpClientBuilder.create().build().execute( request );
        assertThat("Response Code must be 404", httpResponse.getStatusLine().getStatusCode(),
                is(HttpStatus.NOT_FOUND_404));

        //empty id
        request = new HttpGet( BASE_URL_WITH_PORT() + PAYMENT_REQUEST);
        httpResponse = HttpClientBuilder.create().build().execute( request );
        assertThat("Response Code must be 404", httpResponse.getStatusLine().getStatusCode(),
                is(HttpStatus.NOT_FOUND_404));
    }

    /**
     * For now only tests /payone/notification returns 400 for empty request.
     */
    @Test
    public void testNotificationUrlResponse() throws Exception {
        HttpPost httpPost = new HttpPost(BASE_URL_WITH_PORT() + NOTIFICATION_REQUEST);
        HttpResponse httpResponse = HttpClientBuilder.create().build().execute( httpPost );

        assertThat("Response Code must be 400", httpResponse.getStatusLine().getStatusCode(),
                is(HttpStatus.BAD_REQUEST_400));
    }
}
