package com.commercetools.pspadapter.payone.domain.ctp.paymentmethods;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;

/**
 * @author Jan Wolter
 */
public class UnsupportedTransactionExecutorTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private BlockingClient client;

    @InjectMocks
    private UnsupportedTransactionExecutor testee;

    @Ignore
    @Test
    public void addsInterfaceInteractionAndSetsStateToFailure() {

    }

    @Ignore
    @Test
    public void throwsInCaseOfConcurrentlyModifiedPayment() {

    }
}