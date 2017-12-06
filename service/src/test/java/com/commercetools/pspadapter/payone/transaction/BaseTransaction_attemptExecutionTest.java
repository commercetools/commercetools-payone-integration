package com.commercetools.pspadapter.payone.transaction;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.commercetools.pspadapter.payone.domain.payone.model.common.BaseRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseErrorCode;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseStatus;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.*;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import org.assertj.core.api.Assert;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder.PAYONE_INTERACTION_REQUEST;
import static com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseStatus.*;
import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.*;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Base class/methods for testing/asserting transaction executors.
 */
public class BaseTransaction_attemptExecutionTest {
    @Mock
    protected LoadingCache<String, Type> typeCache;

    @Mock
    protected BlockingSphereClient client;

    @Mock
    protected Payment paymentToUpdate;

    @Mock
    protected Payment updatedPayment;

    @Mock
    protected CustomFields paymentCustomField;

    @Mock
    protected CartLike<?> cartLike;

    @Mock
    protected Transaction transaction;

    @Mock
    protected PayoneRequestFactory requestFactory;

    @Mock
    protected PayonePostService payonePostService;

    /**
     * Capture sphere client update actions.
     */
    @Captor
    protected ArgumentCaptor<PaymentUpdateCommand> paymentUpdateCaptor;

    // PaymentWithCartLike#new requires payment with injected values, thus mocked in #setUp()
    protected PaymentWithCartLike paymentWithCartLike;

    /**
     * Mock all values required fo transaction executor.
     */
    @Before
    public void setUp() {
        when(paymentToUpdate.getCustom()).thenReturn(paymentCustomField);
        when(updatedPayment.getCustom()).thenReturn(paymentCustomField);
        when(paymentCustomField.getFieldAsString(REFERENCE_FIELD)).thenReturn("test-payment-reference");
        paymentWithCartLike = new PaymentWithCartLike(paymentToUpdate, cartLike);

        when(client.executeBlocking(any())).thenReturn(updatedPayment);

        when(transaction.getId()).thenReturn("transaction-mock-id");
    }

    /**
     * Common test for {@link ResponseStatus#ERROR} from Payone.
     */
    protected void attemptExecution_withErrorResponse_createsUpdateActions(final BaseRequest request,
                                                                           final TransactionBaseExecutor executor) throws Exception {
        when(payonePostService.executePost(request)).thenReturn(ImmutableMap.of(
                PayoneResponseFields.STATUS, ERROR.getStateCode(),
                PayoneResponseFields.TXID, "responseTxid"
        ));
        executor.attemptExecution(paymentWithCartLike, transaction);

        assertRequestInterfaceInteraction(2);
        assertChangeTransactionStateActions(TransactionState.FAILURE);
        assertCommonAddInterfaceInteractionAction(PAYONE_INTERACTION_RESPONSE, "responseTxid", ERROR.getStateCode());
    }

    /**
     * Common test for {@link ResponseStatus#PENDING} from Payone.
     */
    protected void attemptExecution_withPendingResponse_createsUpdateActions(final BaseRequest request,
                                                                             final TransactionBaseExecutor executor) throws Exception {
        when(payonePostService.executePost(request)).thenReturn(ImmutableMap.of(
                PayoneResponseFields.STATUS, PENDING.getStateCode(),
                PayoneResponseFields.TXID, "responseTxid"
        ));
        executor.attemptExecution(paymentWithCartLike, transaction);

        assertRequestInterfaceInteraction(2);
        assertChangeTransactionStateActions(TransactionState.PENDING);
        assertSetInterfaceIdActions();
        assertCommonAddInterfaceInteractionAction(PAYONE_INTERACTION_RESPONSE, "responseTxid", PENDING.getStateCode());
    }

    /**
     * Common test for case when {@link #payonePostService} throws an exception on execution.
     */
    protected void attemptExecution_withPayoneException_createsUpdateActions(final BaseRequest request,
                                                                             final TransactionBaseExecutor executor) throws Exception {
        when(payonePostService.executePost(request)).thenThrow(new PayoneException("payone exception message"));

        executor.attemptExecution(paymentWithCartLike, transaction);

        assertRequestInterfaceInteraction(2);
        assertChangeTransactionStateActions(TransactionState.FAILURE);
        assertCommonAddInterfaceInteractionAction(PAYONE_INTERACTION_RESPONSE,
                // opposite to other branches we don't expect to have txid here
                "payone exception message", ERROR.getStateCode(), ResponseErrorCode.TRANSACTION_EXCEPTION.getErrorCode());
    }

    /**
     * TODO: should be re-factored when https://github.com/commercetools/commercetools-payone-integration/issues/199
     * is fixed.
     */
    protected void attemptExecution_withUnexpectedResponseStatus_throwsException(final BaseRequest request,
                                                                                 final TransactionBaseExecutor executor) throws Exception {
        when(payonePostService.executePost(request)).thenReturn(ImmutableMap.of(
                PayoneResponseFields.STATUS, "OH-NO-DAVID-BLAINE",
                PayoneResponseFields.TXID, "responseTxid"
        ));

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> executor.attemptExecution(paymentWithCartLike, transaction))
                .withMessageContaining("Unknown PayOne status");

        assertRequestInterfaceInteraction(1);
    }

    protected void assertChangeTransactionStateActions(final TransactionState expectedTransactionState) {
        PaymentUpdateCommand paymentUpdates = getVerifiedSecondPaymentUpdateCommand();
        assertThat(paymentUpdates.getUpdateActions()).containsOnlyOnce(
                ChangeTransactionState.of(expectedTransactionState, "transaction-mock-id")
        );
    }

    protected void assertRedirectActions(final TransactionState expectedTransactionState) {
        assertChangeTransactionStateActions(expectedTransactionState);

        PaymentUpdateCommand paymentUpdates = getVerifiedSecondPaymentUpdateCommand();
        assertThat(paymentUpdates.getUpdateActions()).containsOnlyOnce(
                SetStatusInterfaceCode.of(REDIRECT.getStateCode()),
                SetStatusInterfaceText.of(REDIRECT.getStateCode()),
                SetCustomField.ofObject(CustomFieldKeys.REDIRECT_URL_FIELD, "http://mock-redirect.url")
        );
    }

    protected void assertSetInterfaceIdActions() {
        PaymentUpdateCommand paymentUpdates = getVerifiedSecondPaymentUpdateCommand();
        assertThat(paymentUpdates.getUpdateActions()).containsOnlyOnce(
                SetInterfaceId.of("responseTxid")
        );
    }

    /**
     * Because {@link AddInterfaceInteraction} contains quite complex values - we don't verify it with
     * {@link Assert#isEqualTo(Object)}, but rather verify manually some "control points"
     *
     * @param expectedInteractionType    expected key of {@link AddInterfaceInteraction#getType()})
     * @param expectedResponseSubstrings list of expected (control) substrings which
     *                                   {@link CustomFieldKeys#RESPONSE_FIELD} should contain
     */
    protected void assertCommonAddInterfaceInteractionAction(final String expectedInteractionType,
                                                             final String... expectedResponseSubstrings) {
        PaymentUpdateCommand paymentUpdates = getVerifiedSecondPaymentUpdateCommand();

        AddInterfaceInteraction addInterfaceInteraction = getSecondAddInterfaceInteraction();

        assertThat(addInterfaceInteraction)
                .withFailMessage("AddInterfaceInteraction action is not found in payment updates list: " + paymentUpdates.getUpdateActions())
                .isNotNull();

        assertThat(addInterfaceInteraction.getType())
                .withFailMessage("AddInterfaceInteraction type is empty")
                .isNotNull();

        assertThat(addInterfaceInteraction.getType().getKey())
                .withFailMessage("AddInterfaceInteraction type is incorrect")
                .isEqualTo(expectedInteractionType);

        final Map<String, JsonNode> fields = addInterfaceInteraction.getFields();
        assertThat(fields)
                .withFailMessage("AddInterfaceInteraction fields map is empty")
                .isNotNull();

        assertThat(ZonedDateTime.parse(fields.get(TIMESTAMP_FIELD).textValue())).isBeforeOrEqualTo(ZonedDateTime.now());

        assertThat(fields.get(RESPONSE_FIELD)).isNotNull();
        assertThat(fields.get(RESPONSE_FIELD).isTextual()).isTrue(); // value to RESPONSE_FIELD is written as a string in json format
        assertThat(fields.get(RESPONSE_FIELD).textValue()).contains(expectedResponseSubstrings);

        assertThat(fields).contains(
                entry(CustomFieldKeys.TRANSACTION_ID_FIELD, TextNode.valueOf("transaction-mock-id"))
        );
    }

    /**
     * Additionally to common interface interaction actions, redirect handler should save redirect URL
     */
    protected void assertRedirectAddInterfaceInteractionAction(final String expectedInteractionType,
                                                               final String... expectedResponseSubstrings) {
        assertCommonAddInterfaceInteractionAction(expectedInteractionType, expectedResponseSubstrings);

        assertThat(getSecondAddInterfaceInteraction().getFields()).contains(
                entry(CustomFieldKeys.REDIRECT_URL_FIELD, TextNode.valueOf("http://mock-redirect.url"))
        );
    }

    /**
     * Assert that first update payment command saves request interface interaction with proper timestamp and
     * transaction id field.
     *
     * @param times number of expected interface interactions with {@link #client}: 1 for failed (exception) request,
     *              2 for success request/response.
     */
    protected void assertRequestInterfaceInteraction(int times) {
        verify(client, times(times)).executeBlocking(paymentUpdateCaptor.capture());
        PaymentUpdateCommand saveRequest = paymentUpdateCaptor.getAllValues().get(0);
        List<? extends UpdateAction<Payment>> updateActions = saveRequest.getUpdateActions();
        assertThat(updateActions).contains(ChangeTransactionInteractionId.of("0", "transaction-mock-id"));

        AddInterfaceInteraction addInterfaceInteraction = getAddInterfaceInteraction(saveRequest);
        assertThat(addInterfaceInteraction.getType().getKey()).isEqualTo(PAYONE_INTERACTION_REQUEST);
        Map<String, JsonNode> fields = addInterfaceInteraction.getFields();
        assertThat(fields).contains(entry(TRANSACTION_ID_FIELD, TextNode.valueOf("transaction-mock-id")));
        assertThat(ZonedDateTime.parse(fields.get(TIMESTAMP_FIELD).textValue())).isBeforeOrEqualTo(ZonedDateTime.now());

        assertThat(fields.get(REQUEST_FIELD).textValue()).contains("testRequestKey1", "testRequestValue2",
                "testRequestKey2", "testRequestValue2");
    }

    /**
     * Capture and return Sphere Client "executed" update actions.
     *
     * @return last capture value of {@link #client} execute request.
     */
    protected PaymentUpdateCommand getVerifiedSecondPaymentUpdateCommand() {
        // 2 times expected: 1) save request; 2) save response
        verify(client, times(2)).executeBlocking(paymentUpdateCaptor.capture());
        return paymentUpdateCaptor.getValue();
    }

    /**
     * From captured update actions (from {@link #getVerifiedSecondPaymentUpdateCommand()} filter and return
     * second (last) {@link AddInterfaceInteraction} action.
     *
     * @return second AddInterfaceInteraction from captured update actions list.
     */
    @Nonnull
    protected AddInterfaceInteraction getSecondAddInterfaceInteraction() {
        return getAddInterfaceInteraction(getVerifiedSecondPaymentUpdateCommand());
    }

    @Nonnull
    protected AddInterfaceInteraction getAddInterfaceInteraction(PaymentUpdateCommand paymentUpdates) {
        return paymentUpdates.getUpdateActions().stream()
                .filter(a -> a instanceof AddInterfaceInteraction)
                .map(a -> (AddInterfaceInteraction) a)
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(format("PaymentUpdateCommand [%s] expected to contain AddInterfaceInteraction, but it doesn't.", paymentUpdates)));
    }
}
