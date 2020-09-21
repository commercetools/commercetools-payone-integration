package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.exceptions.NoCartLikeFoundException;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.NotFoundException;
import io.sphere.sdk.http.HttpStatusCode;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.models.errors.ErrorResponse;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentMethodInfo;
import io.sphere.sdk.types.CustomFields;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ConcurrentModificationException;
import java.util.Random;
import java.util.concurrent.CompletionException;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jan Wolter
 */
public class PaymentHandlerTest
{
    private static final Cart UNUSED_CART = null;

    private static final Random random = new Random();

    private static final PaymentMethodInfo payonePaymentMethodInfo = paymentMethodInfo("TestIntegrationServicePaymentMethodName");

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private CommercetoolsQueryExecutor commercetoolsQueryExecutor;

    @Mock
    private PaymentDispatcher paymentDispatcher;

    @Mock
    private Payment payment;

    private PaymentHandler testee;

    @Before
    public void setUp() {
        // the last argument in the constructor is a String, that's why we can't use @InjectMocks for this instantiation
        testee = new PaymentHandler("TestIntegrationServicePaymentMethodName", "testTenantName", commercetoolsQueryExecutor, paymentDispatcher);

        when(payment.getVersion()).thenReturn(1L);
        when(payment.getPaymentMethodInfo()).thenReturn(payonePaymentMethodInfo);
        when(payment.getCustom()).thenReturn(customFields("<dummyReference>"));
    }

    @Test
    public void returnsStatusCodeOk200InCaseOfSuccessfulPaymentHandling() {
        // arrange
        final String paymentId = randomString();
        final PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, UNUSED_CART);
        final PaymentWithCartLike processedPaymentWithCartLike = new PaymentWithCartLike(payment, UNUSED_CART);

        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenReturn(paymentWithCartLike);
        when(paymentDispatcher.dispatchPayment(same(paymentWithCartLike))).thenReturn(processedPaymentWithCartLike);

        // act
        final PaymentHandleResult paymentHandleResult = testee.handlePayment(paymentId);

        // assert
        verify(paymentDispatcher, times(1)).dispatchPayment(same(paymentWithCartLike));
        assertThat(paymentHandleResult.statusCode()).isEqualTo(HttpStatusCode.OK_200);
        assertThat(paymentHandleResult.body()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void returnsStatusCodeOk200InCaseOfSuccessfulConcurrentPaymentHandling() {
        // arrange
        final String paymentId = randomString();
        final PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, UNUSED_CART);
        final Payment modifiedPayment = mock(Payment.class, "modified payment");
        when(modifiedPayment.getCustom()).thenReturn(customFields("dummyReferenceValue"));

        final PaymentWithCartLike modifiedPaymentWithCartLike = new PaymentWithCartLike(modifiedPayment, UNUSED_CART);
        final PaymentMethodInfo paymentMethodInfo = paymentMethodInfo("TestIntegrationServicePaymentMethodName");

        when(payment.getPaymentMethodInfo()).thenReturn(paymentMethodInfo);
        when(modifiedPayment.getPaymentMethodInfo()).thenReturn(paymentMethodInfo);
        when(paymentDispatcher.dispatchPayment(same(paymentWithCartLike)))
                .thenThrow(ConcurrentModificationException.class);

        when(modifiedPayment.getVersion()).thenReturn(3L);
        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId)))
                .thenReturn(paymentWithCartLike, paymentWithCartLike, paymentWithCartLike, modifiedPaymentWithCartLike);

        // act
        final PaymentHandleResult paymentHandleResult = testee.handlePayment(paymentId);

        // assert
        assertThat(paymentHandleResult.statusCode()).isEqualTo(HttpStatusCode.OK_200);
        assertThat(paymentHandleResult.body()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void handlePayment_WithAlwaysConcurrentModificationException_ShouldReturn202() {
        // arrange
        final String paymentId = randomString();
        final PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, UNUSED_CART);

        when(paymentDispatcher.dispatchPayment(same(paymentWithCartLike)))
                .thenThrow(ConcurrentModificationException.class);

        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenReturn(paymentWithCartLike);

        // act
        final PaymentHandleResult paymentHandleResult = testee.handlePayment(paymentId);

        // assert
        assertThat(paymentHandleResult.statusCode()).isEqualTo(HttpStatusCode.ACCEPTED_202);
        assertThat(paymentHandleResult.body())
            .isEqualTo(format("The payment with id '%s' couldn't be processed after %s retries.", paymentId, 5));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void handlePayment_WithLessThanLimitConcurrentModificationException_ShouldReturn200() {
        // arrange
        final String paymentId = randomString();
        final PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, UNUSED_CART);

        when(paymentDispatcher.dispatchPayment(same(paymentWithCartLike)))
            .thenThrow(ConcurrentModificationException.class)
            .thenThrow(ConcurrentModificationException.class)
            .thenReturn(new PaymentWithCartLike(payment, UNUSED_CART));

        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenReturn(paymentWithCartLike);

        // act
        final PaymentHandleResult paymentHandleResult = testee.handlePayment(paymentId);

        // assert
        assertThat(paymentHandleResult.statusCode()).isEqualTo(HttpStatusCode.OK_200);
        assertThat(paymentHandleResult.body()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void handlePayment_WithLessThanLimitConcurrentModificationExceptionButNotFound_ShouldReturn404() {
        // arrange
        final String paymentId = randomString();
        final PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, UNUSED_CART);

        when(paymentDispatcher.dispatchPayment(same(paymentWithCartLike)))
            .thenThrow(ConcurrentModificationException.class)
            .thenThrow(ConcurrentModificationException.class)
            .thenThrow(new NotFoundException());

        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenReturn(paymentWithCartLike);

        // act
        final PaymentHandleResult paymentHandleResult = testee.handlePayment(paymentId);

        // assert
        assertThat(paymentHandleResult.statusCode()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        assertThat(paymentHandleResult.body()).contains(format("Failed to process the commercetools Payment with id "
            + "[%s], as the payment or the cart could not be found", paymentId));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void handlePayment_WithLessThanLimitConcurrentModificationExceptionErrorResponseException_ShouldReturn502() {
        // arrange
        final String paymentId = randomString();
        final PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, UNUSED_CART);

        when(paymentDispatcher.dispatchPayment(same(paymentWithCartLike)))
            .thenThrow(ConcurrentModificationException.class)
            .thenThrow(ConcurrentModificationException.class)
            .thenThrow(new ErrorResponseException(ErrorResponse.of(HttpStatusCode.BAD_GATEWAY_502,
                "commercetools is unavailable", emptyList())));

        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenReturn(paymentWithCartLike);

        // act
        final PaymentHandleResult paymentHandleResult = testee.handlePayment(paymentId);

        // assert
        assertThat(paymentHandleResult.statusCode()).isEqualTo(HttpStatusCode.BAD_GATEWAY_502);
        assertThat(paymentHandleResult.body()).contains(format("Failed to process the commercetools Payment with id "
            + "[%s], due to an error response from the commercetools platform. Try again later.", paymentId));
    }

    @Test
    public void returnsStatusCodeNotFound404InCaseOfUnknownPayment() {
        // arrange
        final String paymentId = randomString();

        final NotFoundException notFoundException = new NotFoundException();

        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenThrow(notFoundException);

        // act
        final PaymentHandleResult paymentHandleResult = testee.handlePayment(paymentId);

        // assert
        assertThat(paymentHandleResult.statusCode()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        assertThat(paymentHandleResult.body()).contains(format("Failed to process the commercetools Payment with id "
            + "[%s], as the payment or the cart could not be found", paymentId));
    }

    @Test
    public void returnsStatusCodeNotFound404InCaseOfCartLikeMissing() {
        // arrange
        final String paymentId = randomString();

        final NoCartLikeFoundException noCartLikeFoundException = new NoCartLikeFoundException();

        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenThrow(noCartLikeFoundException);

        // act
        final PaymentHandleResult paymentHandleResult = testee.handlePayment(paymentId);

        // assert
        assertThat(paymentHandleResult.statusCode()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        assertThat(paymentHandleResult.body()).contains(format("Failed to process the commercetools Payment with id "
            + "[%s], as the payment or the cart could not be found", paymentId));
    }

    @Test
    public void returnsStatusCodeBadRequest400InCaseOfPaymentWithOtherPaymentInterface() {
        // arrange
        final String paymentId = randomString();
        final PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, UNUSED_CART);
        final PaymentMethodInfo paymentMethodInfo = paymentMethodInfo("other than TestIntegrationServicePaymentMethodName");

        when(payment.getPaymentMethodInfo()).thenReturn(paymentMethodInfo);
        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenReturn(paymentWithCartLike);

        // act
        final PaymentHandleResult paymentHandleResult = testee.handlePayment(paymentId);

        // assert
        assertThat(paymentHandleResult.statusCode()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        assertThat(paymentHandleResult.body()).contains(format("Wrong payment interface name: expected '%s', found '%s'"
                + " for the commercetools Payment with id '%s'.",
            payonePaymentMethodInfo.getPaymentInterface(),
            paymentMethodInfo.getPaymentInterface(),
            paymentId));
    }

    @Test
    public void returnsStatusCodeInternalServerError500InCaseOfUnexpectedExceptionFromCommercetoolsQuery() {
        // arrange
        final String paymentId = randomString();

        final String exceptionMessage = randomString();
        final CompletionException exception = new CompletionException(exceptionMessage) {};

        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenThrow(exception);

        // act
        final PaymentHandleResult paymentHandleResult = testee.handlePayment(paymentId);

        // assert
        assertThat(paymentHandleResult.statusCode()).isEqualTo(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
        assertThat(paymentHandleResult.body()).contains(format("Unexpected error occurred when processing commercetools Payment with id [%s]. See the service logs", paymentId));
    }

    @Test
    public void returnsStatusCodeInternalServerError500InCaseOfUnexpectedExceptionFromPaymentWithCartLike() {
        // arrange
        final String paymentId = randomString();

        final String exceptionMessage = randomString();
        final RuntimeException runtimeException = new RuntimeException(exceptionMessage);

        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId)))
                .thenThrow(runtimeException);

        // act
        final PaymentHandleResult paymentHandleResult = testee.handlePayment(paymentId);

        // assert
        assertThat(paymentHandleResult.statusCode()).isEqualTo(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
        assertThat(paymentHandleResult.body()).contains(format("Unexpected error occurred when processing commercetools"
            + " Payment with id [%s]. See the service logs", paymentId));
    }

    @Test
    public void returnsStatusCodeInternalServerError500InCaseOfUnexpectedExceptionFromPaymentDispatcher() {
        // arrange
        final String paymentId = randomString();
        final PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, UNUSED_CART);

        final String exceptionMessage = randomString();
        final RuntimeException exception = new RuntimeException(exceptionMessage) {
        };

        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenReturn(paymentWithCartLike);
        when(paymentDispatcher.dispatchPayment(same(paymentWithCartLike))).thenThrow(exception);

        // act
        final PaymentHandleResult paymentHandleResult = testee.handlePayment(paymentId);

        // assert
        assertThat(paymentHandleResult.statusCode()).isEqualTo(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
        assertThat(paymentHandleResult.body()).contains(format("Unexpected error occurred when processing commercetools"
            + " Payment with id [%s]. See the service logs", paymentId));
    }

    private static PaymentMethodInfo paymentMethodInfo(final String paymentInterface) {
        return SphereJsonUtils.readObject(
                "{\"paymentInterface\": \"" + paymentInterface +  "\",\"method\": \"CREDIT_CARD\"}",
                PaymentMethodInfo.class);
    }

    private static CustomFields customFields(final String referenceValue) {
        return SphereJsonUtils.readObject(
                "{\n" +
                        "        \"type\": {\n" +
                        "          \"typeId\": \"type\",\n" +
                        "          \"id\": \"b16889f7-a900-4558-a7db-26ba86a7703e\"\n" +
                        "        },\n" +
                        "        \"fields\": {\n" +
                        "          \"reference\": \"" + referenceValue + "\"\n" +
                        "        }\n" +
                        "      }\n",
                CustomFields.class);
    }

    private static String randomString() {
        return Integer.toString(random.nextInt(0x7FFFFFFF));
    }
}
