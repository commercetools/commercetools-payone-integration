package com.commercetools.pspadapter.payone.transaction.common;

import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.transaction.BaseTransaction_attemptExecutionTest;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ChargeTransaction_attemptExecutionTest extends BaseTransaction_attemptExecutionTest {

    private ChargeTransactionExecutor executor;

    @Mock
    protected AuthorizationRequest authorizationRequest;

    @Override
    @Before
    public void setUp() {
        super.setUp();

        executor = new ChargeTransactionExecutor(typeCache, requestFactory, payonePostService, client);

        when(authorizationRequest.toStringMap(anyBoolean())).thenReturn(ImmutableMap.of("testRequestKey1", "testRequestValue2",
                "testRequestKey2", "testRequestValue2"));
        when(requestFactory.createAuthorizationRequest(paymentWithCartLike)).thenReturn(authorizationRequest);
    }

    @Test
    public void attemptExecution_withRedirectResponse_createsUpdateActions() throws Exception {
        super.attemptExecution_withRedirectResponse_createsUpdateActions(authorizationRequest, executor);
    }

    @Test
    public void attemptExecution_withApprovedResponse_createsUpdateActions() throws Exception {
        super.attemptExecution_withApprovedResponse_createsUpdateActions(authorizationRequest, executor);
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