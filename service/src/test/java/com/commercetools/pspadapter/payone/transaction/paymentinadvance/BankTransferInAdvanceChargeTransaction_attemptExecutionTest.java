package com.commercetools.pspadapter.payone.transaction.paymentinadvance;

import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields;
import com.commercetools.pspadapter.payone.domain.payone.model.paymentinadvance.BankTransferInAdvanceRequest;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.commercetools.pspadapter.payone.transaction.BaseTransaction_attemptExecutionTest;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.SetCustomField;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

import static com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseStatus.APPROVED;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseStatus.REDIRECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BankTransferInAdvanceChargeTransaction_attemptExecutionTest extends BaseTransaction_attemptExecutionTest {

    private BaseBankTransferInAdvanceTransactionExecutor executor;

    @Mock
    protected BankTransferInAdvanceRequest bankTransferInAdvanceRequest;


    @Override
    @Before
    public void setUp() {
        super.setUp();
        executor = new BankTransferInAdvanceChargeTransactionExecutor(typeCache, requestFactory, payonePostService, client);

        final HashMap<String, Object> requestMap = new HashMap<>();
        requestMap.put("testRequestKey1", "testRequestValue2");
        requestMap.put("testRequestKey2", "testRequestValue2");

        when(bankTransferInAdvanceRequest.toStringMap(anyBoolean())).thenReturn(requestMap);
        when(requestFactory.createPreauthorizationRequest(paymentWithCartLike)).thenReturn(bankTransferInAdvanceRequest);
        when(requestFactory.createAuthorizationRequest(paymentWithCartLike)).thenReturn(bankTransferInAdvanceRequest);
    }

    @Test
    public void attemptExecution_withApprovedResponse_createsUpdateActions() throws Exception {
        final HashMap<String, String> responseMap = new HashMap<>();
        responseMap.put(PayoneResponseFields.STATUS, APPROVED.getStateCode());
        responseMap.put(PayoneResponseFields.TXID, "responseTxid");
        // bank transfer related custom field values
        responseMap.put(PayoneResponseFields.BIC, "test-mock-BIC");
        responseMap.put(PayoneResponseFields.IBAN, "test-mock-IBAN");
        responseMap.put(PayoneResponseFields.ACCOUNT_HOLDER, "test-mock-NAME");
        when(payonePostService.executePost(bankTransferInAdvanceRequest)).thenReturn(responseMap);
        executor.execute(paymentWithCartLike, transaction);

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
        super.attemptExecution_withErrorResponse_createsUpdateActions(bankTransferInAdvanceRequest, executor);
    }

    @Test
    public void attemptExecution_withPendingResponse_createsUpdateActions() throws Exception {
        super.attemptExecution_withPendingResponse_createsUpdateActions(bankTransferInAdvanceRequest, executor);
    }

    @Test
    public void attemptExecution_withPayoneException_createsUpdateActions() throws Exception {
        super.attemptExecution_withPayoneException_createsUpdateActions(bankTransferInAdvanceRequest, executor);
    }

    @Test
    public void attemptExecution_withUnexpectedResponseStatus_throwsException() throws Exception {
        super.attemptExecution_withUnexpectedResponseStatus_throwsException(bankTransferInAdvanceRequest, executor);
    }

    /**
     * For Bank Transfer Advanced redirect is not expected response - exception is expected.
     * <p>
     * TODO: https://github.com/commercetools/commercetools-payone-integration/issues/199
     */
    @Test
    public void attemptExecution_withRedirectResponse_throwsException() throws Exception {
        final HashMap<String, String> responseMap = new HashMap<>();
        responseMap.put(PayoneResponseFields.STATUS, REDIRECT.getStateCode());
        responseMap.put(PayoneResponseFields.REDIRECT_URL, "http://mock-redirect.url");
        responseMap.put(PayoneResponseFields.TXID, "responseTxid");
        when(payonePostService.executePost(bankTransferInAdvanceRequest)).thenReturn(responseMap);

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> executor.execute(paymentWithCartLike, transaction))
                .withMessageContaining("Unknown Payone status");

        assertRequestInterfaceInteraction(1);
    }

}
