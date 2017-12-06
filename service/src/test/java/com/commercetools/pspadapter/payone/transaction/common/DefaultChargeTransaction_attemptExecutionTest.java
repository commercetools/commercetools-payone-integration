package com.commercetools.pspadapter.payone.transaction.common;

import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields;
import com.commercetools.pspadapter.payone.transaction.BaseTransaction_attemptExecutionTest;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.TransactionState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder.PAYONE_INTERACTION_REDIRECT;
import static com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseStatus.APPROVED;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseStatus.REDIRECT;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultChargeTransaction_attemptExecutionTest extends BaseTransaction_attemptExecutionTest {

    private DefaultChargeTransactionExecutor executor;

    @Mock
    protected AuthorizationRequest authorizationRequest;

    @Override
    @Before
    public void setUp() {
        super.setUp();

        executor = new DefaultChargeTransactionExecutor(typeCache, requestFactory, payonePostService, client);

        when(authorizationRequest.toStringMap(anyBoolean())).thenReturn(ImmutableMap.of("testRequestKey1", "testRequestValue2",
                "testRequestKey2", "testRequestValue2"));
        when(requestFactory.createAuthorizationRequest(paymentWithCartLike)).thenReturn(authorizationRequest);
    }

    @Test
    public void attemptExecution_withRedirectResponse_createsUpdateActions() throws Exception {
        when(payonePostService.executePost(authorizationRequest)).thenReturn(ImmutableMap.of(
                PayoneResponseFields.STATUS, REDIRECT.getStateCode(),
                PayoneResponseFields.REDIRECT_URL, "http://mock-redirect.url",
                PayoneResponseFields.TXID, "responseTxid"
        ));
        executor.attemptExecution(paymentWithCartLike, transaction);

        assertRequestInterfaceInteraction(2);
        assertRedirectActions(TransactionState.PENDING);
        assertSetInterfaceIdActions();
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

        assertRequestInterfaceInteraction(2);
        assertChangeTransactionStateActions(TransactionState.SUCCESS);
        assertSetInterfaceIdActions();
        assertCommonAddInterfaceInteractionAction(PAYONE_INTERACTION_RESPONSE, "responseTxid", APPROVED.getStateCode());
    }

    @Test
    public void attemptExecution_withErrorResponse_createsUpdateActions() throws Exception {
        super.attemptExecution_withErrorResponse_createsUpdateActions(authorizationRequest, executor);
    }

    @Test
    public void attemptExecution_withPendingResponse_createsUpdateActions() throws Exception {
        super.attemptExecution_withPendingResponse_createsUpdateActions(authorizationRequest, executor);
    }

    @Test
    public void attemptExecution_withPayoneException_createsUpdateActions() throws Exception {
        super.attemptExecution_withPayoneException_createsUpdateActions(authorizationRequest, executor);
    }

    @Test
    public void attemptExecution_withUnexpectedResponseStatus_throwsException() throws Exception {
        super.attemptExecution_withUnexpectedResponseStatus_throwsException(authorizationRequest, executor);
    }
}