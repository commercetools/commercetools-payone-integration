package com.commercetools.pspadapter.payone.transaction.common;

import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneRequest;
import com.commercetools.pspadapter.payone.transaction.BaseTransaction_attemptExecutionTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ChargeTransaction_attemptExecutionTest extends BaseTransaction_attemptExecutionTest {

    private ChargeTransactionExecutor executor;

    @Mock
    protected PayoneRequest payoneRequest;

    @Override
    @Before
    public void setUp() {
        super.setUp();

        executor = new ChargeTransactionExecutor(typeCache, requestFactory, payonePostService, client);

        final HashMap<String, Object> requestMap = new HashMap<>();
        requestMap.put("testRequestKey1", "testRequestValue2");
        requestMap.put("testRequestKey2", "testRequestValue2");

        when(payoneRequest.toStringMap(anyBoolean())).thenReturn(requestMap);
        when(requestFactory.createAuthorizationRequest(paymentWithCartLike)).thenReturn(payoneRequest);
    }

    @Test
    public void attemptExecution_withRedirectResponse_createsUpdateActions() throws Exception {
        super.attemptExecution_withRedirectResponse_createsUpdateActions(payoneRequest, executor);
    }

    @Test
    public void attemptExecution_withApprovedResponse_createsUpdateActions() throws Exception {
        super.attemptExecution_withApprovedResponse_createsUpdateActions(payoneRequest, executor);
    }

    @Test
    public void attemptExecution_withErrorResponse_createsUpdateActions() throws Exception {
        super.attemptExecution_withErrorResponse_createsUpdateActions(payoneRequest, executor);
    }

    @Test
    public void attemptExecution_withPendingResponse_createsUpdateActions() throws Exception {
        super.attemptExecution_withPendingResponse_createsUpdateActions(payoneRequest, executor);
    }

    @Test
    public void attemptExecution_withPayoneException_createsUpdateActions() throws Exception {
        super.attemptExecution_withPayoneException_createsUpdateActions(payoneRequest, executor);
    }

    @Test
    public void attemptExecution_withUnexpectedResponseStatus_throwsException() throws Exception {
        super.attemptExecution_withUnexpectedResponseStatus_throwsException(payoneRequest, executor);
    }
}