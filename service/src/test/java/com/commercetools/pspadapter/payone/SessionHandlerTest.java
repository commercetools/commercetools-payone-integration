package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.BaseTenantPropertyTest;
import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.StartSessionRequestWithCart;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaStartSessionRequest;
import com.commercetools.pspadapter.payone.mapping.klarna.KlarnaRequestFactory;
import com.commercetools.service.PaymentService;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.http.HttpStatusCode;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.payments.Payment;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import util.PaymentTestHelper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.pspadapter.payone.SessionHandler.ADD_PAYDATA_CLIENT_TOKEN;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType.GENERICPAYMEMT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jan Wolter
 */
public class SessionHandlerTest extends BaseTenantPropertyTest {


    private static final Random random = new Random();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Captor
    protected ArgumentCaptor<List<UpdateAction<Payment>>> paymentRequestUpdatesCaptor;
    @Mock
    KlarnaRequestFactory requestFactory;
    private Cart cart = null;
    @Mock
    private CommercetoolsQueryExecutor commercetoolsQueryExecutor;
    @Mock
    private PayonePostService postService;

    @Mock
    private PaymentService paymentService;

    private SessionHandler testee;

    private static String randomString() {
        return Integer.toString(random.nextInt(0x7FFFFFFF));
    }

    public static String stringFromResource(final String resourcePath) throws Exception {
        InputStream resourceStream =
                Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(resourcePath);
        return IOUtils.toString(resourceStream, StandardCharsets.UTF_8);
    }

    @Before
    public void setUp() throws Exception {
        // the last argument in the constructor is a String, that's why we can't use @InjectMocks for this instantiation
        testee = new SessionHandler("PAYONE", "testTenantName", commercetoolsQueryExecutor, postService, paymentService, requestFactory);
        PaymentTestHelper testHelper = new PaymentTestHelper();
        cart = testHelper.dummyKlarnaCart();

    }

    @Test
    public void start_startSessioCallWasSuccessFull_shouldReturn200() throws Exception {
        String customerToken = "anyCustomerToken";
        String sessionId = randomString();
        String paymentId = randomString();
        Payment payment = mockPayment(paymentId, "PAYONE", "INVOICE-KLARNA", "emptyKlarnaPayment.json");

        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, cart);
        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenReturn(paymentWithCartLike);
        when(tenantPropertyProvider.getCommonPropertyProvider()).thenReturn(propertyProvider);
        when(tenantPropertyProvider.getTenantProperty(any())).thenReturn(Optional.of("dummyValue"));
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(any())).thenReturn("dummyValue");
        PayoneConfig payoneConfig = new PayoneConfig(tenantPropertyProvider);
        StartSessionRequestWithCart startSessionRequest = new KlarnaStartSessionRequest(payoneConfig,
                GENERICPAYMEMT.getType(), paymentWithCartLike);
        when(requestFactory.createStartSessionRequest(eq(paymentWithCartLike))).thenReturn(startSessionRequest);
        when(paymentService.updatePayment(any(), any())).thenReturn(CompletableFuture.completedFuture(payment));
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put(ADD_PAYDATA_CLIENT_TOKEN, customerToken);
        resultMap.put("add_paydata[session_id]", sessionId);
        when(postService.executePost(eq(startSessionRequest))).thenReturn(resultMap);
        PayoneResult payoneResult = testee.start(paymentId);
        verify(paymentService).updatePayment(eq(payment), paymentRequestUpdatesCaptor.capture());
        List<UpdateAction<Payment>> updateActions = paymentRequestUpdatesCaptor.getValue();
        assertThat(updateActions.get(0).getAction()).isEqualTo("setCustomField");
        assertThat(updateActions.get(1).getAction()).isEqualTo("setCustomField");
        assertThat(updateActions.get(2).getAction()).isEqualTo("setCustomField");
        assertThat(payoneResult.statusCode()).isEqualTo(HttpStatusCode.OK_200);
        assertThat(payoneResult.body()).isEqualTo("{\"add_paydata[session_id]\":\"" + sessionId + "\",\"add_paydata[client_token" +
                "]\":\"anyCustomerToken\"}");
    }

    @Test
    public void start_clientTokenAlreadyProvided_shouldReturn200() throws Exception {

        String paymentId = randomString();
        Payment payment = mockPayment(paymentId, "PAYONE", "INVOICE-KLARNA", "notEmptyKlarnaPayment.json");
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, cart);
        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenReturn(paymentWithCartLike);
        StartSessionRequestWithCart startSessionRequest = mock(StartSessionRequestWithCart.class);
        when(requestFactory.createStartSessionRequest(eq(paymentWithCartLike))).thenReturn(startSessionRequest);
        PayoneResult payoneResult = testee.start(paymentId);
        verify(paymentService, times(0)).updatePayment(eq(payment), paymentRequestUpdatesCaptor.capture());
        assertThat(payoneResult.statusCode()).isEqualTo(HttpStatusCode.OK_200);
        assertThat(payoneResult.body()).isEqualTo("existingResponse");
    }

    @Test
    public void start_paymentCannotBeFound_shouldReturn400() throws Exception {
        String paymentId = "633060207";
        PaymentWithCartLike paymentWithCartLike = null;
        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenReturn(paymentWithCartLike);
        PayoneResult payoneResult = testee.start(paymentId);
        assertThat(payoneResult.statusCode()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        assertThat(payoneResult.body()).isEqualTo("The payment with id '633060207' cannot be found.");
    }

    @Test
    public void start_wrongPaymentInterfaceWasProvided_shouldReturn400() throws Exception {
        String paymentId = randomString();
        Payment payment = mockPayment(paymentId, "WRONG_INTERFACE", "INVOICE-KLARNA", "emptyKlarnaPayment.json");
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, cart);
        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenReturn(paymentWithCartLike);

        PayoneResult payoneResult = testee.start(paymentId);
        verify(paymentService, times(0)).updatePayment(eq(payment), paymentRequestUpdatesCaptor.capture());
        assertThat(payoneResult.statusCode()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);

    }

    @Test
    public void start_wrongPaymentMethodeWasProvided_shouldReturn400() throws Exception {
        String paymentId = randomString();
        Payment payment = mockPayment(paymentId, "PAYONE", "WRONG-PAYMENT", "emptyKlarnaPayment.json");
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, cart);
        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenReturn(paymentWithCartLike);

        PayoneResult payoneResult = testee.start(paymentId);
        verify(paymentService, times(0)).updatePayment(eq(payment), paymentRequestUpdatesCaptor.capture());
        assertThat(payoneResult.statusCode()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);

    }

    private Payment mockPayment(String paymentID, String paymentInterface, String method, String sourcePath) {
        String jsonString = null;
        try {
            jsonString = stringFromResource(sourcePath);
            jsonString = jsonString.replaceAll("###paymentID###", paymentID);
            jsonString = jsonString.replaceAll("###paymentInterface###", paymentInterface);
            jsonString = jsonString.replaceAll("###method###", method);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SphereJsonUtils.readObject(jsonString, Payment.typeReference());

    }
}