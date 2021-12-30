package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.model.common.StartSessionRequestWithCart;
import com.commercetools.pspadapter.payone.mapping.klarna.KlarnaRequestFactory;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.service.PaymentService;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.http.HttpStatusCode;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.payments.Payment;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.commercetools.pspadapter.payone.SessionHandler.ADD_PAYDATA_CLIENT_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jan Wolter
 */
public class SessionHandlerTest {
    private static final Cart UNUSED_CART = null;

    private static final Random random = new Random();


    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private CommercetoolsQueryExecutor commercetoolsQueryExecutor;

    @Mock
    KlarnaRequestFactory requestFactory;

    @Mock
    private PayonePostService postService;

    @Mock
    private PaymentService paymentService;

    private SessionHandler testee;

    @Before
    public void setUp() {
        // the last argument in the constructor is a String, that's why we can't use @InjectMocks for this instantiation
        testee = new SessionHandler("PAYONE", "testTenantName",  commercetoolsQueryExecutor, postService, paymentService, requestFactory);


    }

    @Test
    public void start_startSessioCallWasSuccessFull_shouldReturn200() throws Exception {
        String customerToken = "anyCustomerToken";
        String sessionId = randomString();
        String paymentId = randomString();
        Payment payment = mockPayment(paymentId);
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, UNUSED_CART);
        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenReturn(paymentWithCartLike);
        StartSessionRequestWithCart startSessionRequest = mock(StartSessionRequestWithCart.class);
        when(requestFactory.createStartSessionRequest(eq(paymentWithCartLike))).thenReturn(startSessionRequest);
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put(ADD_PAYDATA_CLIENT_TOKEN, customerToken);
        resultMap.put("add_paydata[session_id]", sessionId);
        when(postService.executePost(eq(startSessionRequest))).thenReturn(resultMap);
        PayoneResult payoneResult = testee.start(paymentId);
        assertThat(payoneResult.statusCode()).isEqualTo(HttpStatusCode.OK_200);
        assertThat(payoneResult.body()).isEqualTo("{\"add_paydata[session_id]\":\""+sessionId+"\",\"add_paydata[client_token" +
                "]\":\"anyCustomerToken\"}");
    }


    private Payment mockPayment(String paymentID) {
        String jsonString = null;
        try {
            jsonString = stringFromResource("emptyKlarnaPayment.json");
            jsonString = jsonString.replaceAll("###paymentID###", paymentID);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SphereJsonUtils.readObject(jsonString, Payment.typeReference());

    }

    private static String randomString() {
        return Integer.toString(random.nextInt(0x7FFFFFFF));
    }


    public static String stringFromResource(final String resourcePath) throws Exception {
        InputStream resourceStream =
                Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(resourcePath);
        return IOUtils.toString(resourceStream, "UTF-8");
    }
}