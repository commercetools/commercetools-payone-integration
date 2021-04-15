package com.commercetools.pspadapter.payone.notification.common;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionState;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceCode;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceText;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import javax.money.MonetaryAmount;
import java.util.List;

import static io.sphere.sdk.payments.TransactionType.CHARGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static util.UpdatePaymentTestHelper.assertStandardUpdateActions;
import static util.UpdatePaymentTestHelper.getAddInterfaceInteraction;
import static util.UpdatePaymentTestHelper.getSetStatusInterfaceCode;
import static util.UpdatePaymentTestHelper.getSetStatusInterfaceText;

/**
 * @author Jan Wolter
 */
@RunWith(MockitoJUnitRunner.class)
public class PaidNotificationProcessorTest extends BaseChargedNotificationProcessorTest {

    @InjectMocks
    private PaidNotificationProcessor testee;

    // Payone "paid" is mapped to CTP "paid" state
    private static final PaymentState ORDER_PAYMENT_STATE = PaymentState.PAID;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        notification = new Notification();
        notification.setPrice("20.00");
        notification.setCurrency("EUR");
        notification.setTxtime(millis.toString());
        notification.setSequencenumber("23");
        notification.setTxaction(NotificationAction.PAID);
        notification.setTransactionStatus(TransactionStatus.COMPLETED);

        when(paymentToOrderStateMapper.mapPaymentToOrderState(any(Payment.class)))
                .thenReturn(ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processingPendingNotificationAboutUnknownTransactionAddsChargeTransactionWithStatePending() throws Exception {
        super.processingPendingNotificationAboutUnknownTransactionAddsChargeTransactionWithStatePending(testee, ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processingCompletedNotificationAboutUnknownTransactionAddsChargeTransactionWithStateSuccess()
            throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentOneAuthPending20EuroCC();
        payment.getTransactions().clear();

        notification.setReceivable("20.00");
        notification.setBalance("0.00");

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());
        final AddTransaction transaction = AddTransaction.of(TransactionDraftBuilder
                .of(TransactionType.CHARGE, amount, timestamp)
                .state(TransactionState.SUCCESS)
                .interactionId(notification.getSequencenumber())
                .build());

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, timestamp);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions).as("added transactions")
                .containsExactlyInAnyOrder(transaction, interfaceInteraction, statusInterfaceCode, statusInterfaceText);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processingPendingNotificationForPendingChargeTransactionDoesNotChangeState() throws Exception {
        super.processingPendingNotificationForPendingChargeTransactionDoesNotChangeState(testee, ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processingCompletedNotificationForPendingChargeTransactionChangesStateToSuccess() throws Exception {
        final Payment payment = processingCompletedNotificationForPendingChargeTransactionChangesStateToSuccessWireframe();

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }

    @Test
    public void orderServiceIsNotCalledWhenIsUpdateOrderPaymentStateFalse() throws Exception {
        // this test is similar to #processingCompletedNotificationForPendingChargeTransactionChangesStateToSuccess(),
        // but #isUpdateOrderPaymentState() is false, so update order actions should be skipped
        when(tenantConfig.isUpdateOrderPaymentState()).thenReturn(false);

        processingCompletedNotificationForPendingChargeTransactionChangesStateToSuccessWireframe();
        verifyUpdateOrderActionsNotCalled();
    }

    @Test
    public void whenChargeTransactionIsInitial_ChangeTransactionStateISAdded() throws Exception {
        Payment paymentChargeInitial = testHelper.dummyPaymentOneChargeInitial20Euro();
        Transaction firstTransaction = paymentChargeInitial.getTransactions().get(0);
        notification.setSequencenumber(firstTransaction.getInteractionId());
        List<UpdateAction<Payment>> authPendingUpdates = testee.createPaymentUpdates(paymentChargeInitial, notification);
        assertThat(authPendingUpdates).containsOnlyOnce(ChangeTransactionState.of(
                notification.getTransactionStatus().getCtTransactionState(), firstTransaction.getId()));

        verify_isNotCompletedTransaction_called(TransactionState.INITIAL, CHARGE);
    }

    @Test
    public void whenChargeTransactionIsPending_ChangeTransactionStateISAdded() throws Exception {
        Payment paymentChargePending = testHelper.dummyPaymentOneChargePending20Euro();
        Transaction firstTransaction = paymentChargePending.getTransactions().get(0);
        notification.setSequencenumber(firstTransaction.getInteractionId());
        List<UpdateAction<Payment>> authPendingUpdates = testee.createPaymentUpdates(paymentChargePending, notification);
        assertThat(authPendingUpdates).containsOnlyOnce(ChangeTransactionState.of(
                notification.getTransactionStatus().getCtTransactionState(), firstTransaction.getId()));

        verify_isNotCompletedTransaction_called(TransactionState.PENDING, CHARGE);
    }

    @Test
    public void whenChargeTransactionIsSuccess_ChangeTransactionStateNOTAdded() throws Exception {
        Payment paymentAuthSuccess = testHelper.dummyPaymentOneChargeSuccess20Euro();
        Transaction firstTransaction = paymentAuthSuccess.getTransactions().get(0);
        notification.setSequencenumber(firstTransaction.getInteractionId());
        List<UpdateAction<Payment>> authSuccessUpdates = testee.createPaymentUpdates(paymentAuthSuccess, notification);
        assertTransactionOfTypeIsNotAdded(authSuccessUpdates, ChangeTransactionState.class);

        verify_isNotCompletedTransaction_called(TransactionState.SUCCESS, CHARGE);
    }

    @Test
    public void whenChargeTransactionIsFailure_ChangeTransactionStateNOTAdded() throws Exception {
        Payment paymentChargeFailure = testHelper.dummyPaymentOneChargeFailure20Euro();
        Transaction firstTransaction = paymentChargeFailure.getTransactions().get(0);
        notification.setSequencenumber(firstTransaction.getInteractionId());
        List<UpdateAction<Payment>> authFailureUpdates = testee.createPaymentUpdates(paymentChargeFailure, notification);
        assertTransactionOfTypeIsNotAdded(authFailureUpdates, ChangeTransactionState.class);

        verify_isNotCompletedTransaction_called(TransactionState.FAILURE, CHARGE);
    }

    @SuppressWarnings("unchecked")
    private Payment processingCompletedNotificationForPendingChargeTransactionChangesStateToSuccessWireframe() throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentOneChargePending20Euro();
        final Transaction chargeTransaction = payment.getTransactions().get(0);

        notification.setSequencenumber(chargeTransaction.getInteractionId());
        notification.setReceivable("20.00");
        notification.setBalance("0.00");


        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, timestamp);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions).as("payment update actions list")
                .containsExactlyInAnyOrder(
                        ChangeTransactionState.of(TransactionState.SUCCESS, chargeTransaction.getId()),
                        interfaceInteraction, statusInterfaceCode, statusInterfaceText);

        return payment;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processingCompletedNotificationForSuccessfulChargeTransactionDoesNotChangeState() throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentTwoTransactionsSuccessPending();

        notification.setSequencenumber(payment.getTransactions().get(0).getInteractionId());
        notification.setPrice("200.00");
        notification.setReceivable(notification.getPrice());
        notification.setBalance("0.00");

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, timestamp);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions).as("# of payment update actions").hasSize(3);
        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processingCompletedNotificationForFailedChargeTransactionDoesNotChangeState() throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentOneChargeFailure20Euro();

        notification.setSequencenumber(payment.getTransactions().get(0).getInteractionId());
        notification.setTransactionStatus(TransactionStatus.PENDING);
        notification.setReceivable("20.00");
        notification.setBalance("0.00");

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, timestamp);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions).as("# of payment update actions").hasSize(3);
        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }
}
