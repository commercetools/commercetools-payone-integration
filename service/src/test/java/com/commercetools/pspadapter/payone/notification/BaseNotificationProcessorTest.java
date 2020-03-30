package com.commercetools.pspadapter.payone.notification;

import com.commercetools.payments.TransactionStateResolver;
import com.commercetools.payments.TransactionStateResolverImpl;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.mapping.order.PaymentToOrderStateMapper;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.pspadapter.tenant.TenantFactory;
import com.commercetools.service.OrderService;
import com.commercetools.service.PaymentService;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import util.PaymentTestHelper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class BaseNotificationProcessorTest {
    @Mock(lenient = true)
    protected PaymentService paymentService;

    @Mock(lenient = true)
    protected OrderService orderService;

    @Mock
    protected PaymentToOrderStateMapper paymentToOrderStateMapper;

    @Mock(lenient = true)
    protected TenantFactory tenantFactory;

    @Mock(lenient = true)
    protected TenantConfig tenantConfig;

    // it is used for injection to "@InjectMocks *NotificationProcessor testee" instances in the child classes
    @Spy
    protected TransactionStateResolver transactionStateResolver = new TransactionStateResolverImpl();

    @Mock
    protected Order orderToUpdate;

    @Mock
    protected Order orderUpdated;

    @Captor
    protected ArgumentCaptor<List<UpdateAction<Payment>>> paymentRequestUpdatesCaptor;

    @Captor
    protected ArgumentCaptor<Payment> paymentRequestPayment;

    protected final PaymentTestHelper testHelper = new PaymentTestHelper();

    protected Notification notification;

    @Before
    public void setUp() throws Exception {
        when(tenantFactory.getPaymentService()).thenReturn(paymentService);
        when(paymentService.updatePayment(any(Payment.class), anyObject()))
                .then(answer -> CompletableFuture.completedFuture(answer.getArgument(0, Payment.class)));

        when(tenantFactory.getOrderService()).thenReturn(orderService);
        when(orderService.getOrderByPaymentId(anyString()))
                .then(answer -> CompletableFuture.completedFuture(Optional.of(orderToUpdate)));
        when(orderService.updateOrderPaymentState(any(Order.class), any(PaymentState.class)))
                .then(answer -> CompletableFuture.completedFuture(orderUpdated));

        when(tenantFactory.getPaymentToOrderStateMapper()).thenReturn(paymentToOrderStateMapper);

        when(tenantConfig.isUpdateOrderPaymentState()).thenReturn(true);
    }

    protected List<? extends UpdateAction<Payment>> updatePaymentAndGetUpdateActions(Payment payment) {
        verify(paymentService).updatePayment(paymentRequestPayment.capture(), paymentRequestUpdatesCaptor.capture());

        final Payment updatePayment = paymentRequestPayment.getValue();
        assertThat(updatePayment).isEqualTo(payment);
        return paymentRequestUpdatesCaptor.getValue();
    }

    /**
     * Verify that the processor called expected order service functions when a payment is updated
     *
     * @param payment              payment which is updated
     * @param expectedPaymentState expected new payment state of the updated order
     */
    protected void verifyUpdateOrderActions(Payment payment, PaymentState expectedPaymentState) {
        ArgumentCaptor<String> paymentIdForOrderCaptor = ArgumentCaptor.forClass(String.class);
        verify(orderService).getOrderByPaymentId(paymentIdForOrderCaptor.capture());

        // verify the orderService.getOrderByPaymentId() was called with with our payment id
        assertThat(paymentIdForOrderCaptor.getValue()).isEqualTo(payment.getId());

        ArgumentCaptor<Order> orderUpdateCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<PaymentState> paymentStateCaptor = ArgumentCaptor.forClass(PaymentState.class);
        verify(orderService).updateOrderPaymentState(orderUpdateCaptor.capture(), paymentStateCaptor.capture());

        // verify the orderService.updateOrderPaymentState() for the same value we returned in orderService.getOrderByPaymentId()
        assertThat(orderUpdateCaptor.getValue()).isSameAs(orderToUpdate);
        // verify the orderService.updateOrderPaymentState() is called with expected PaymentState
        assertThat(paymentStateCaptor.getValue()).isEqualTo(expectedPaymentState);
    }

    protected void verifyUpdateOrderActionsNotCalled() {
        verify(orderService, never()).getOrderByPaymentId(anyString());
        verify(orderService, never()).updateOrderPaymentState(anyObject(), anyObject());
    }

    protected final void assertTransactionOfTypeIsNotAdded(List<UpdateAction<Payment>> actions,
                                                           Class<? extends UpdateAction<? extends Payment>> actionType) {
        actions.stream()
                .filter(actionType::isInstance)
                .findFirst()
                .ifPresent(action -> {
                    throw new AssertionError(
                            format("Expected that actions list does not contain action \"%s\", but it contains",
                                    actionType.getSimpleName()));
                });
    }

    /**
     * Also verify that {@link com.commercetools.payments.TransactionStateResolver#isNotCompletedTransaction(Transaction)}
     * has been called during a test with expected transaction type/state,
     * so we are sure we are testing {@code isNotCompletedTransaction()} influence
     *
     * @param transactionState expected captured transaction state
     * @param transactionType  expected captured transaction type
     */
    protected final void verify_isNotCompletedTransaction_called(TransactionState transactionState,
                                                                 TransactionType transactionType) {
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionStateResolver).isNotCompletedTransaction(transactionCaptor.capture());
        Transaction capturedAuthSuccessTransaction = transactionCaptor.getValue();
        assertThat(capturedAuthSuccessTransaction.getType()).isEqualTo(transactionType);
        assertThat(capturedAuthSuccessTransaction.getState()).isEqualTo(transactionState);
    }

}
