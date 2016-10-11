package com.commercetools.pspadapter.payone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.exceptions.NoCartLikeFoundException;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.client.NotFoundException;
import io.sphere.sdk.http.HttpStatusCode;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentMethodInfo;
import io.sphere.sdk.types.CustomFields;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Random;
import java.util.concurrent.CompletionException;

/**
 * @author Jan Wolter
 */
public class IntegrationServiceTest
{
    private static final Cart UNUSED_CART = null;

    private static final Random random = new Random();

    private static final PaymentMethodInfo payonePaymentMethodInfo = paymentMethodInfo("PAYONE");

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private CustomTypeBuilder customTypeBuilder;

    @Mock
    private CommercetoolsQueryExecutor commercetoolsQueryExecutor;

    @Mock
    private PaymentDispatcher paymentDispatcher;

    @Mock
    private Payment payment;

    @InjectMocks
    private IntegrationService testee;

    @Before
    public void setUp() throws IOException {
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
        assertThat(paymentHandleResult.statusCode(), is(HttpStatusCode.OK_200));
        assertThat(paymentHandleResult.body(), isEmptyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void returnsStatusCodeOk200InCaseOfSuccessfulConcurrentPaymentHandling() throws IOException {
        // arrange
        final String paymentId = randomString();
        final PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, UNUSED_CART);
        final Payment modifiedPayment = mock(Payment.class, "modified payment");
        when(modifiedPayment.getCustom()).thenReturn(customFields("dummyReferenceValue"));

        final PaymentWithCartLike modifiedPaymentWithCartLike = new PaymentWithCartLike(modifiedPayment, UNUSED_CART);
        final PaymentMethodInfo paymentMethodInfo = paymentMethodInfo("PAYONE");

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
        assertThat(paymentHandleResult.statusCode(), is(HttpStatusCode.OK_200));
        assertThat(paymentHandleResult.body(), isEmptyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void returnsStatusCodeAccepted202InCaseOfOngoingConcurrentPaymentHandling() {
        // arrange
        final String paymentId = randomString();
        final PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, UNUSED_CART);

        when(paymentDispatcher.dispatchPayment(same(paymentWithCartLike)))
                .thenThrow(ConcurrentModificationException.class);

        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenReturn(paymentWithCartLike);

        // act
        final PaymentHandleResult paymentHandleResult = testee.handlePayment(paymentId);

        // assert
        assertThat(paymentHandleResult.statusCode(), is(HttpStatusCode.ACCEPTED_202));
        assertThat(paymentHandleResult.body(), isEmptyString());
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
        assertThat(paymentHandleResult.statusCode(), is(HttpStatusCode.NOT_FOUND_404));
        assertThat(paymentHandleResult.body(), containsString("Could not process payment with ID \"" + paymentId + "\""));
        assertThat(paymentHandleResult.body(), containsString(notFoundException.getMessage()));
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
        assertThat(paymentHandleResult.statusCode(), is(HttpStatusCode.NOT_FOUND_404));
        assertThat(paymentHandleResult.body(), containsString("Could not process payment with ID \"" + paymentId + "\""));
        assertThat(paymentHandleResult.body(), containsString(noCartLikeFoundException.getMessage()));
    }

    @Test
    public void returnsStatusCodeBadRequest400InCaseOfPaymentWithOtherPaymentInterface() {
        // arrange
        final String paymentId = randomString();
        final PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, UNUSED_CART);
        final PaymentMethodInfo paymentMethodInfo = paymentMethodInfo("other than PAYONE");

        when(payment.getPaymentMethodInfo()).thenReturn(paymentMethodInfo);
        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenReturn(paymentWithCartLike);

        // act
        final PaymentHandleResult paymentHandleResult = testee.handlePayment(paymentId);

        // assert
        assertThat(paymentHandleResult.statusCode(), is(HttpStatusCode.BAD_REQUEST_400));
        assertThat(paymentHandleResult.body(), isEmptyString());
    }

    @Test
    public void returnsStatusCodeInternalServerError500InCaseOfUnexpectedExceptionFromCommercetoolsQuery() {
        // arrange
        final String paymentId = randomString();

        final String exceptionMessage = randomString();
        final CompletionException exception = new CompletionException(exceptionMessage) {
        };

        when(commercetoolsQueryExecutor.getPaymentWithCartLike(eq(paymentId))).thenThrow(exception);

        // act
        final PaymentHandleResult paymentHandleResult = testee.handlePayment(paymentId);

        // assert
        assertThat(paymentHandleResult.statusCode(), is(HttpStatusCode.INTERNAL_SERVER_ERROR_500));
        assertThat(paymentHandleResult.body(), containsString("An error occurred during communication with the commercetools platform"));
        assertThat(paymentHandleResult.body(), containsString(exceptionMessage));
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
        assertThat(paymentHandleResult.statusCode(), is(HttpStatusCode.INTERNAL_SERVER_ERROR_500));
        assertThat(paymentHandleResult.body(), containsString("Sorry, but you hit us between the eyes"));
        assertThat(paymentHandleResult.body(), containsString(exceptionMessage));
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
        assertThat(paymentHandleResult.statusCode(), is(HttpStatusCode.INTERNAL_SERVER_ERROR_500));
        assertThat(paymentHandleResult.body(), containsString(exceptionMessage));
    }

    private static PaymentMethodInfo paymentMethodInfo(final String paymentInterface) {
        return SphereJsonUtils.readObject(
                "{\"paymentInterface\": \"" + paymentInterface +  "\",\"method\": \"CREDIT_CARD\"}",
                PaymentMethodInfo.class);
    }

    private static CustomFields customFields(final String referenceValue) throws IOException {
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
        return Integer.toString(random.nextInt());
    }
}
