package com.commercetools.pspadapter.payone;

import com.commercetools.payments.TransactionStateResolver;
import com.commercetools.payments.TransactionStateResolverImpl;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod;
import com.commercetools.pspadapter.payone.transaction.PaymentMethodDispatcher;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import util.PaymentTestHelper;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;

import static com.commercetools.pspadapter.payone.util.PayoneConstants.PAYONE;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PaymentDispatcherTest {
    private final PaymentTestHelper payments = new PaymentTestHelper();

    @Spy
    private TransactionStateResolver transactionStateResolver = new TransactionStateResolverImpl();

    private class CountingPaymentMethodDispatcher extends PaymentMethodDispatcher {
        public int count = 0;

        public CountingPaymentMethodDispatcher() {
            super((payment, transaction) -> payment,
                    ImmutableMap.of(),
                    transactionStateResolver);
        }

        @Override
        public PaymentWithCartLike dispatchPayment(@Nonnull PaymentWithCartLike payment) {
            count += 1;
            return super.dispatchPayment(payment);
        }
    }

    @Test
    public void testRefusingWrongPaymentInterface() throws Exception {
        PaymentDispatcher dispatcher = new PaymentDispatcher(null, "TEST-IGNORED");

        final Throwable noInterface = catchThrowable(() -> dispatcher.dispatchPayment(new PaymentWithCartLike(payments.dummyPaymentNoInterface(), null)));
        assertThat(noInterface).isInstanceOf(IllegalArgumentException.class);

        final Throwable wrongInterface = catchThrowable(() -> dispatcher.dispatchPayment(new PaymentWithCartLike(payments.dummyPaymentWrongInterface(), null)));
        assertThat(wrongInterface).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testRefusingUnknownPaymentMethod() throws Exception {
        PaymentDispatcher dispatcher = new PaymentDispatcher(new HashMap<>(), "TEST-IGNORED");

        final Throwable noInterface = catchThrowable(() -> dispatcher.dispatchPayment(new PaymentWithCartLike(payments.dummyPaymentUnknownMethod(), null)));
        assertThat(noInterface).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testDispatchCorrectlyWithInitialTransactions() throws Exception {
        verifyDispatchCallsProperMethods(payments.dummyPaymentTwoTransactionsInitial(),
                payments.dummyPaymentTwoTransactionsSuccessInitial());
    }

    @Test
    public void testDispatchCorrectlyWithPendingTransactions() throws Exception {
        // Initial/PendingFix: this should not be true after migrating to Initial state:
        // Pending transactions should NOT be dispatched any more
        verifyDispatchCallsProperMethods(payments.dummyPaymentTwoTransactionsPending(),
                payments.dummyPaymentTwoTransactionsSuccessPending());
    }

    private void verifyDispatchCallsProperMethods(Payment paymentPendingOrInitial, Payment paymentSuccess) throws Exception {
        final PaymentWithCartLike paymentPendingOrInitialWithCartLike = new PaymentWithCartLike(paymentPendingOrInitial, null);
        final PaymentWithCartLike paymentSuccessWithCartLike = new PaymentWithCartLike(paymentSuccess, null);
        final Transaction firstInitTransaction = paymentPendingOrInitial.getTransactions().get(0);

        final CountingPaymentMethodDispatcher creditCardDispatcher = new CountingPaymentMethodDispatcher();
        final CountingPaymentMethodDispatcher postFinanceDispatcher = new CountingPaymentMethodDispatcher();

        final HashMap<PaymentMethod, PaymentMethodDispatcher> methodDispatcherMap = new HashMap<>();
        methodDispatcherMap.put(PaymentMethod.CREDIT_CARD, creditCardDispatcher);
        methodDispatcherMap.put(PaymentMethod.BANK_TRANSFER_POSTFINANCE_EFINANCE, postFinanceDispatcher);

        PaymentDispatcher dispatcher = new PaymentDispatcher(methodDispatcherMap, PAYONE);
        dispatcher.dispatchPayment(paymentPendingOrInitialWithCartLike);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        // isNotCompletedTransaction expected to be called twice: 1 - filter transaction, 2 - re-verify update transaction
        verify(transactionStateResolver, times(2)).isNotCompletedTransaction(transactionCaptor.capture());

        // credit card is dispatched ones, cos it has only one CC payment
        assertThat(creditCardDispatcher.count).isEqualTo(1);
        // sepa is not called, because there were no sepa payments
        assertThat(postFinanceDispatcher.count).isEqualTo(0);

        // PaymentMethodDispatcher#dispatchPayment() filters in first in-completed (Initial/Pending) transaction,
        // and then verifies updated transaction, which is the same in our case.
        // second transaction is skipped, because the first one is updated successfully.
        List<Transaction> verifiedTransactions = transactionCaptor.getAllValues();
        assertThat(verifiedTransactions.stream().map(Transaction::getId).collect(toList()))
                .containsExactly(firstInitTransaction.getId(), firstInitTransaction.getId());

        dispatcher.dispatchPayment(paymentSuccessWithCartLike);
        // 2 is number of times isNotCompletedTransaction() is called on previous step,
        // +3 times is called now: 1 for first success transaction (filtered out), 1 for second pending/initial, 1 for updated transaction
        verify(transactionStateResolver, times(2 + 3)).isNotCompletedTransaction(transactionCaptor.capture());
        // second (accumulated) with test above CC dispatch call
        assertThat(creditCardDispatcher.count).isEqualTo(1 + 1);
        // sepa still never called
        assertThat(postFinanceDispatcher.count).isEqualTo(0);
    }
}