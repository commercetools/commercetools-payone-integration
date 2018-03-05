package com.commercetools.pspadapter.payone.transaction.paymentinadvance;

import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields;
import com.commercetools.pspadapter.payone.domain.payone.model.paymentinadvance.BankTransferInAdvanceRequest;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.commercetools.pspadapter.payone.transaction.BaseTransaction_attemptExecutionTest;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.SetCustomField;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseStatus.APPROVED;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseStatus.REDIRECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BankTransferInAdvanceChargeTransaction_attemptExecutionTest extends BaseTransaction_attemptExecutionTest {

    private BankTransferInAdvanceChargeTransactionExecutor executor;

    @Mock
    protected BankTransferInAdvanceRequest preAuthorizationRequest;


    @Override
    @Before
    public void setUp() {
        super.setUp();
        executor = new BankTransferInAdvanceChargeTransactionExecutor(typeCache, requestFactory, payonePostService, client);

        when(preAuthorizationRequest.toStringMap(anyBoolean())).thenReturn(ImmutableMap.of("testRequestKey1", "testRequestValue2",
                "testRequestKey2", "testRequestValue2"));
        when(requestFactory.createPreauthorizationRequest(paymentWithCartLike)).thenReturn(preAuthorizationRequest);
    }

    @Test
    public void attemptExecution_withApprovedResponse_createsUpdateActions() throws Exception {
        when(payonePostService.executePost(preAuthorizationRequest)).thenReturn(ImmutableMap.of(
                PayoneResponseFields.STATUS, APPROVED.getStateCode(),
                PayoneResponseFields.TXID, "responseTxid",

                // bank transfer related custom field values
                PayoneResponseFields.BIC, "test-mock-BIC",
                PayoneResponseFields.IBAN, "test-mock-IBAN",
                PayoneResponseFields.ACCOUNT_HOLDER, "test-mock-NAME"
        ));
        executor.attemptExecution(paymentWithCartLike, transaction);

        assertRequestInterfaceInteraction(2);

        // opposite to instant and redirect payment methods, Vorkasse remains/becomes Pending till buyer sends money
        assertChangeTransactionStateActions(TransactionState.PENDING);

        assertSetInterfaceIdActions();
        assertCommonAddInterfaceInteractionAction(PAYONE_INTERACTION_RESPONSE, "responseTxid", APPROVED.getStateCode());

        PaymentUpdateCommand paymentUpdates = getVerifiedSecondPaymentUpdateCommand();
        assertThat(paymentUpdates.getUpdateActions()).containsOnlyOnce(
                SetCustomField.ofObject(CustomFieldKeys.PAY_TO_BIC_FIELD, "test-mock-BIC"),
                SetCustomField.ofObject(CustomFieldKeys.PAY_TO_IBAN_FIELD, "test-mock-IBAN"),
                SetCustomField.ofObject(CustomFieldKeys.PAY_TO_NAME_FIELD, "test-mock-NAME")
        );
    }

    @Test
    public void attemptExecution_withErrorResponse_createsUpdateActions() throws Exception {
        super.attemptExecution_withErrorResponse_createsUpdateActions(preAuthorizationRequest, executor);
    }

    @Test
    public void attemptExecution_withPendingResponse_createsUpdateActions() throws Exception {
        super.attemptExecution_withPendingResponse_createsUpdateActions(preAuthorizationRequest, executor);
    }

    @Test
    public void attemptExecution_withPayoneException_createsUpdateActions() throws Exception {
        super.attemptExecution_withPayoneException_createsUpdateActions(preAuthorizationRequest, executor);
    }

    @Test
    public void attemptExecution_withUnexpectedResponseStatus_throwsException() throws Exception {
        super.attemptExecution_withUnexpectedResponseStatus_throwsException(preAuthorizationRequest, executor);
    }

    /**
     * For Bank Transfer Advanced redirect is not expected response - exception is expected.
     * <p>
     * TODO: https://github.com/commercetools/commercetools-payone-integration/issues/199
     */
    @Test
    public void attemptExecution_withRedirectResponse_throwsException() throws Exception {
        when(payonePostService.executePost(preAuthorizationRequest)).thenReturn(ImmutableMap.of(
                PayoneResponseFields.STATUS, REDIRECT.getStateCode(),
                PayoneResponseFields.REDIRECT_URL, "http://mock-redirect.url",
                PayoneResponseFields.TXID, "responseTxid"
        ));

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> executor.attemptExecution(paymentWithCartLike, transaction))
                .withMessageContaining("Unknown PayOne status");

        assertRequestInterfaceInteraction(1);
    }

}