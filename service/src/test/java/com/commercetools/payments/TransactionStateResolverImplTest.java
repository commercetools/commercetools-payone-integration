package com.commercetools.payments;

import io.sphere.sdk.payments.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static io.sphere.sdk.payments.TransactionState.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TransactionStateResolverImplTest {

    private TransactionStateResolver resolver;

    @Mock
    private Transaction transaction;

    @Before
    public void setUp() throws Exception {
        resolver = new TransactionStateResolverImpl();
    }

    @Test
    public void isNotCompletedTransaction() throws Exception {
        when(transaction.getState()).thenReturn(INITIAL);
        assertThat(resolver.isNotCompletedTransaction(transaction)).isTrue();

        when(transaction.getState()).thenReturn(PENDING);
        assertThat(resolver.isNotCompletedTransaction(transaction)).isTrue();

        when(transaction.getState()).thenReturn(SUCCESS);
        assertThat(resolver.isNotCompletedTransaction(transaction)).isFalse();

        when(transaction.getState()).thenReturn(FAILURE);
        assertThat(resolver.isNotCompletedTransaction(transaction)).isFalse();
    }
}