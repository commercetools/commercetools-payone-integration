package com.commercetools.pspadapter.payone.util;

import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.commercetools.pspadapter.payone.util.PaymentUtil.getTransactionById;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PaymentUtilTest {

    @Mock
    private Payment payment;

    @Mock
    private Transaction transaction;

    @Test
    public void getTransactionById_emptyCases() {
        assertThat(getTransactionById(null, null)).isEmpty();
        assertThat(getTransactionById(null, "blah")).isEmpty();
        assertThat(getTransactionById(payment, null)).isEmpty();

        when(payment.getTransactions()).thenReturn(singletonList(transaction));
        when(transaction.getId()).thenReturn("transaction-id");
        assertThat(getTransactionById(payment, "blah")).isEmpty();
        assertThat(getTransactionById(payment, null)).isEmpty();
        assertThat(getTransactionById(payment, "")).isEmpty();
    }

    @Test
    public void getTransactionById_normalCases() {
        when(payment.getTransactions()).thenReturn(singletonList(transaction));
        when(transaction.getId()).thenReturn("transaction-id");
        assertThat(getTransactionById(payment, "transaction-id")).contains(transaction);

        Transaction transaction2 = mock(Transaction.class);
        when(transaction2.getId()).thenReturn("another-transaction-id");

        when(payment.getTransactions()).thenReturn(asList(transaction2, transaction));
        assertThat(getTransactionById(payment, "transaction-id")).contains(transaction);
        assertThat(getTransactionById(payment, "another-transaction-id")).contains(transaction2);

        when(payment.getTransactions()).thenReturn(asList(transaction, transaction2));
        assertThat(getTransactionById(payment, "transaction-id")).contains(transaction);
        assertThat(getTransactionById(payment, "another-transaction-id")).contains(transaction2);
        assertThat(getTransactionById(payment, "blah")).isEmpty();
    }
}