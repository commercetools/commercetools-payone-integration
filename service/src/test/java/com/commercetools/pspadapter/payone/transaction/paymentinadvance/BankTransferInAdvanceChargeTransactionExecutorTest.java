package com.commercetools.pspadapter.payone.transaction.paymentinadvance;

import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseErrorCode;
import com.commercetools.pspadapter.payone.domain.payone.model.paymentinadvance.BankTransferInAdvancePreautorizationRequest;
import com.commercetools.pspadapter.payone.transaction.BaseTransactionBaseExecutorTest;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.TransactionState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseStatus.*;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BankTransferInAdvanceChargeTransactionExecutorTest extends BaseTransactionBaseExecutorTest {

    private BankTransferInAdvanceChargeTransactionExecutor executor;

    @Mock
    protected BankTransferInAdvancePreautorizationRequest preAuthorizationRequest;


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
                PayoneResponseFields.TXID, "responseTxid"
        ));
        executor.attemptExecution(paymentWithCartLike, transaction);

        assertRequestInterfaceInteraction(2);
        assertChangeTransactionStateActions(TransactionState.SUCCESS);
        assertSetInterfaceIdActions();
        assertCommonAddInterfaceInteractionAction(PAYONE_INTERACTION_RESPONSE, "responseTxid", APPROVED.getStateCode());
    }

    @Test
    public void attemptExecution_withErrorResponse_createsUpdateActions() throws Exception {
        when(payonePostService.executePost(preAuthorizationRequest)).thenReturn(ImmutableMap.of(
                PayoneResponseFields.STATUS, ERROR.getStateCode(),
                PayoneResponseFields.TXID, "responseTxid"
        ));
        executor.attemptExecution(paymentWithCartLike, transaction);

        assertRequestInterfaceInteraction(2);
        assertChangeTransactionStateActions(TransactionState.FAILURE);
        assertCommonAddInterfaceInteractionAction(PAYONE_INTERACTION_RESPONSE, "responseTxid", ERROR.getStateCode());
    }

    @Test
    public void attemptExecution_withPendingResponse_createsUpdateActions() throws Exception {
        when(payonePostService.executePost(preAuthorizationRequest)).thenReturn(ImmutableMap.of(
                PayoneResponseFields.STATUS, PENDING.getStateCode(),
                PayoneResponseFields.TXID, "responseTxid"
        ));
        executor.attemptExecution(paymentWithCartLike, transaction);

        assertRequestInterfaceInteraction(2);
        assertChangeTransactionStateActions(TransactionState.PENDING);
        assertSetInterfaceIdActions();
        assertCommonAddInterfaceInteractionAction(PAYONE_INTERACTION_RESPONSE, "responseTxid", PENDING.getStateCode());
    }

    @Test
    public void attemptExecution_withPayoneException_createsUpdateActions() throws Exception {
        when(payonePostService.executePost(preAuthorizationRequest)).thenThrow(new PayoneException("payone exception message"));

        executor.attemptExecution(paymentWithCartLike, transaction);

        assertRequestInterfaceInteraction(2);
        assertChangeTransactionStateActions(TransactionState.FAILURE);
        assertCommonAddInterfaceInteractionAction(PAYONE_INTERACTION_RESPONSE,
                // opposite to other branches we don't expect to have txid here
                "payone exception message", ERROR.getStateCode(), ResponseErrorCode.TRANSACTION_EXCEPTION.getErrorCode());
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
                PayoneResponseFields.REDIRECT, "http://mock-redirect.url",
                PayoneResponseFields.TXID, "responseTxid"
        ));

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> executor.attemptExecution(paymentWithCartLike, transaction))
                .withMessageContaining("Unknown PayOne status");

        assertRequestInterfaceInteraction(1);
    }

    // TODO: https://github.com/commercetools/commercetools-payone-integration/issues/199
    @Test
    public void attemptExecution_withUnexpectedResponseStatus_throwsException() throws Exception {
        when(payonePostService.executePost(preAuthorizationRequest)).thenReturn(ImmutableMap.of(
                PayoneResponseFields.STATUS, "OH-NO-DAVID-BLAINE",
                PayoneResponseFields.TXID, "responseTxid"
        ));

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> executor.attemptExecution(paymentWithCartLike, transaction))
                .withMessageContaining("Unknown PayOne status");

        assertRequestInterfaceInteraction(1);
    }
}