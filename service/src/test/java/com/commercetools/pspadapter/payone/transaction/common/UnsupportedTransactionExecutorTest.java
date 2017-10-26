package com.commercetools.pspadapter.payone.transaction.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import util.PaymentTestHelper;

import java.util.concurrent.CompletionException;

/**
 * @author Jan Wolter
 */
public class UnsupportedTransactionExecutorTest {
    private final PaymentTestHelper payments = new PaymentTestHelper();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private BlockingSphereClient client;

    @InjectMocks
    private UnsupportedTransactionExecutor testee;

    @Test
    public void addsInterfaceInteractionAndSetsStateToFailure() throws Exception {
        final Payment inputPayment = payments.dummyPaymentTwoTransactionsPending();
        final Payment outputPayment = payments.dummyPaymentTwoTransactionsSuccessPending();
        final Transaction transaction = inputPayment.getTransactions().get(0);

        // TODO jw: use more specific matchers
        when(client.executeBlocking(isA(PaymentUpdateCommand.class))).thenReturn(outputPayment);

        assertThat(testee.executeTransaction(new PaymentWithCartLike(inputPayment, (Cart)null), transaction).getPayment()).isSameAs(outputPayment);
    }

    @Test
    public void throwsCompletionException() throws Exception {
        final Payment inputPayment = payments.dummyPaymentTwoTransactionsSuccessPending();
        final Transaction transaction = inputPayment.getTransactions().get(0);

        // TODO jw: use more specific matchers
        final CompletionException completionException = new CompletionException(new Exception());
        when(client.executeBlocking(isA(PaymentUpdateCommand.class)))
                .thenThrow(completionException);

        final Throwable throwable = catchThrowable(() -> testee.executeTransaction(new PaymentWithCartLike(inputPayment, (Cart)null), transaction));

        assertThat(throwable).isSameAs(completionException);
    }

}