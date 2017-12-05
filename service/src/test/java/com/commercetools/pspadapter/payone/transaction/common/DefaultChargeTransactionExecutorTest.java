package com.commercetools.pspadapter.payone.transaction.common;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseErrorCode;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.*;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.Map;

import static com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder.PAYONE_INTERACTION_REDIRECT;
import static com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseStatus.*;
import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.*;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class DefaultChargeTransactionExecutorTest {

    private DefaultChargeTransactionExecutor executor;

    @Mock
    private LoadingCache<String, Type> typeCache;

    @Mock
    private TenantConfig tenantConfig;

    @Mock
    private BlockingSphereClient client;

    @Mock
    private Payment paymentToUpdate;

    @Mock
    private Payment updatedPayment;

    @Mock
    private CustomFields paymentCustomField;

    @Mock
    private CartLike<?> cartLike;

    @Mock
    private Transaction transaction;

    @Mock
    private AuthorizationRequest authorizationRequest;

    @Mock
    private PayoneRequestFactory requestFactory;

    @Mock
    private PayonePostService payonePostService;

    @Captor
    private ArgumentCaptor<PaymentUpdateCommand> paymentUpdateCaptor;

    private PaymentWithCartLike paymentWithCartLike;

    @Before
    public void setUp() throws Exception {
        //requestFactory = new CreditCardRequestFactory(tenantConfig);
        executor = new DefaultChargeTransactionExecutor(typeCache, requestFactory, payonePostService, client);

        when(paymentToUpdate.getCustom()).thenReturn(paymentCustomField);
        when(updatedPayment.getCustom()).thenReturn(paymentCustomField);
        when(paymentCustomField.getFieldAsString(REFERENCE_FIELD)).thenReturn("test-payment-reference");
        paymentWithCartLike = new PaymentWithCartLike(paymentToUpdate, cartLike);

        when(requestFactory.createAuthorizationRequest(paymentWithCartLike)).thenReturn(authorizationRequest);
        when(client.executeBlocking(any())).thenReturn(updatedPayment);

        when(authorizationRequest.toStringMap(anyBoolean())).thenReturn(emptyMap());

        when(transaction.getId()).thenReturn("transaction-mock-id");
    }

    @Test
    public void attemptExecution_withRedirectResponse_createsUpdateActions() throws Exception {
        when(payonePostService.executePost(authorizationRequest)).thenReturn(ImmutableMap.of(
                PayoneResponseFields.STATUS, REDIRECT.getStateCode(),
                PayoneResponseFields.REDIRECT, "http://mock-redirect.url",
                PayoneResponseFields.TXID, "responseTxid"
        ));
        executor.attemptExecution(paymentWithCartLike, transaction);

        assertRedirectActions(TransactionState.PENDING);
        assertRedirectAddInterfaceInteractionAction(PAYONE_INTERACTION_REDIRECT,
                "http://mock-redirect.url", REDIRECT.getStateCode(), "responseTxid");
    }

    @Test
    public void attemptExecution_withApprovedResponse_createsUpdateActions() throws Exception {
        when(payonePostService.executePost(authorizationRequest)).thenReturn(ImmutableMap.of(
                PayoneResponseFields.STATUS, APPROVED.getStateCode(),
                PayoneResponseFields.TXID, "responseTxid"
        ));
        executor.attemptExecution(paymentWithCartLike, transaction);

        assertApprovedActions(TransactionState.SUCCESS);
        assertCommonAddInterfaceInteractionAction(PAYONE_INTERACTION_RESPONSE, "responseTxid", APPROVED.getStateCode());
    }

    @Test
    public void attemptExecution_withErrorResponse_createsUpdateActions() throws Exception {
        when(payonePostService.executePost(authorizationRequest)).thenReturn(ImmutableMap.of(
                PayoneResponseFields.STATUS, ERROR.getStateCode(),
                PayoneResponseFields.TXID, "responseTxid"
        ));
        executor.attemptExecution(paymentWithCartLike, transaction);

        assertChangeTransactionStateActions(TransactionState.FAILURE);
        assertCommonAddInterfaceInteractionAction(PAYONE_INTERACTION_RESPONSE, "responseTxid", ERROR.getStateCode());
    }

    @Test
    public void attemptExecution_withPendingResponse_createsUpdateActions() throws Exception {
        when(payonePostService.executePost(authorizationRequest)).thenReturn(ImmutableMap.of(
                PayoneResponseFields.STATUS, PENDING.getStateCode(),
                PayoneResponseFields.TXID, "responseTxid"
        ));
        executor.attemptExecution(paymentWithCartLike, transaction);

        assertChangeTransactionStateActions(TransactionState.PENDING);
        assertCommonAddInterfaceInteractionAction(PAYONE_INTERACTION_RESPONSE, "responseTxid", PENDING.getStateCode());
    }

    @Test
    public void attemptExecution_withPayoneException_createsUpdateActions() throws Exception {
        when(payonePostService.executePost(authorizationRequest)).thenThrow(new PayoneException("payone exception message"));

        executor.attemptExecution(paymentWithCartLike, transaction);

        assertChangeTransactionStateActions(TransactionState.FAILURE);
        assertCommonAddInterfaceInteractionAction(PAYONE_INTERACTION_RESPONSE,
                "payone exception message", ERROR.getStateCode(), ResponseErrorCode.TRANSACTION_EXCEPTION.getErrorCode());
    }

    // TODO: https://github.com/commercetools/commercetools-payone-integration/issues/199
    @Test
    public void attemptExecution_withUnexpectedResponseStatus_throwsException() throws Exception {
        when(payonePostService.executePost(authorizationRequest)).thenReturn(ImmutableMap.of(
                PayoneResponseFields.STATUS, "OH-NO-DAVID-BLAINE",
                PayoneResponseFields.TXID, "responseTxid"
        ));

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> executor.attemptExecution(paymentWithCartLike, transaction))
                .withMessageContaining("Unknown PayOne status");
    }

    private void assertChangeTransactionStateActions(final TransactionState expectedTransactionState) {
        PaymentUpdateCommand paymentUpdates = getVerifiedPaymentUpdateCommand();
        assertThat(paymentUpdates.getUpdateActions()).containsOnlyOnce(
                ChangeTransactionState.of(expectedTransactionState, "transaction-mock-id")
        );
    }

    private void assertRedirectActions(final TransactionState expectedTransactionState) {
        assertChangeTransactionStateActions(expectedTransactionState);

        PaymentUpdateCommand paymentUpdates = getVerifiedPaymentUpdateCommand();
        assertThat(paymentUpdates.getUpdateActions()).containsOnlyOnce(
                SetStatusInterfaceCode.of(REDIRECT.getStateCode()),
                SetStatusInterfaceText.of(REDIRECT.getStateCode()),
                SetCustomField.ofObject(CustomFieldKeys.REDIRECT_URL_FIELD, "http://mock-redirect.url")
        );
    }

    private void assertApprovedActions(final TransactionState expectedTransactionState) {
        assertChangeTransactionStateActions(expectedTransactionState);

        PaymentUpdateCommand paymentUpdates = getVerifiedPaymentUpdateCommand();
        assertThat(paymentUpdates.getUpdateActions()).containsOnlyOnce(
                SetInterfaceId.of("responseTxid")
        );
    }

    private void assertCommonAddInterfaceInteractionAction(final String expectedInteractionType,
                                                           final String ... expectedResponseSubstrings) {
        PaymentUpdateCommand paymentUpdates = getVerifiedPaymentUpdateCommand();

        AddInterfaceInteraction addInterfaceInteraction = getAddInterfaceInteraction();

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

    private void assertRedirectAddInterfaceInteractionAction(final String expectedInteractionType,
                                                             final String ... expectedResponseSubstrings) {
        assertCommonAddInterfaceInteractionAction(expectedInteractionType, expectedResponseSubstrings);

        assertThat(getAddInterfaceInteraction().getFields()).contains(
                entry(CustomFieldKeys.REDIRECT_URL_FIELD, TextNode.valueOf("http://mock-redirect.url"))
        );
    }

    private PaymentUpdateCommand getVerifiedPaymentUpdateCommand() {
        verify(client, times(2)).executeBlocking(paymentUpdateCaptor.capture());
        return paymentUpdateCaptor.getValue();
    }

    @Nonnull
    private AddInterfaceInteraction getAddInterfaceInteraction() {
        PaymentUpdateCommand paymentUpdates = getVerifiedPaymentUpdateCommand();
        return paymentUpdates.getUpdateActions().stream()
                .filter(a -> a instanceof AddInterfaceInteraction)
                .map(a -> (AddInterfaceInteraction) a)
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(format("PaymentUpdateCommand [%s] expected to contain AddInterfaceInteraction, but it doesn't.", paymentUpdates)));
    }


}