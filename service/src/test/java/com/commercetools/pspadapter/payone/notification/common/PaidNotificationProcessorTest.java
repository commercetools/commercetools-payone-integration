package com.commercetools.pspadapter.payone.notification.common;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.payments.commands.updateactions.*;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import javax.money.MonetaryAmount;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static util.UpdatePaymentTestHelper.*;

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

        assertThat(updateActions).as("# of payment update actions").hasSize(5);
        assertThat(updateActions).as("added transaction")
                .filteredOn(u -> u.getAction().equals("addTransaction"))
                .usingElementComparatorOnFields(
                        "transaction.type", "transaction.amount", "transaction.state", "transaction.timestamp")
                .containsOnlyOnce(transaction);

        assertThat(updateActions).as("amount paid")
                .filteredOn(u -> u.getAction().equals("setAmountPaid"))
                .containsOnly(SetAmountPaid.of(MoneyImpl.of(notification.getPrice(), notification.getCurrency())));
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);

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

        assertThat(updateActions).as("# of payment update actions").hasSize(5);
        assertThat(updateActions).as("transaction state change")
                .filteredOn(u -> u.getAction().equals("changeTransactionState"))
                .containsOnly(ChangeTransactionState.of(TransactionState.SUCCESS, chargeTransaction.getId()));

        assertThat(updateActions).as("amount paid")
                .filteredOn(u -> u.getAction().equals("setAmountPaid"))
                .containsOnly(SetAmountPaid.of(MoneyImpl.of(notification.getPrice(), notification.getCurrency())));

        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);
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
