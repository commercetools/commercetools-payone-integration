package com.commercetools.pspadapter.payone.notification.common;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceCode;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceText;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static util.UpdatePaymentTestHelper.*;

/**
 * @author Jan Wolter
 */
@RunWith(MockitoJUnitRunner.class)
public class CaptureNotificationProcessorTest extends BaseChargedNotificationProcessorTest {

    private static final Integer millis = 1450365542;
    private static final ZonedDateTime timestamp =
        ZonedDateTime.of(LocalDateTime.ofEpochSecond(millis, 0, ZoneOffset.UTC), ZoneId.of("UTC"));

    @InjectMocks
    private CaptureNotificationProcessor testee;


    // Payone "capture" is used to be mapped to CTP "paid" state
    private static final PaymentState ORDER_PAYMENT_STATE = PaymentState.PAID;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        notification = new Notification();
        notification.setPrice("20.00");
        notification.setCurrency("EUR");
        notification.setTxtime(millis.toString());
        notification.setSequencenumber("23");
        notification.setClearingtype("cc");
        notification.setTxaction(NotificationAction.CAPTURE);
        notification.setTransactionStatus(TransactionStatus.COMPLETED);

        when(paymentToOrderStateMapper.mapPaymentToOrderState(any(Payment.class)))
                .thenReturn(ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processingPendingNotificationAboutUnknownTransactionAddsChargeTransactionWithStatePending() throws Exception {
        // note, this case tests un-documented (actually, documented, but not clearly) feature:
        // it is not clear, could it ever be that txaction=capture has transaction_state=pending.
        // Doc says [PAYONE Platform Channel Server API, Edition: 2017-08-29, Version: 2.92, Chapter 4.2.2 List of status (transaction_status)]:
        //   > The parameter “transaction_status” is currently introduced with event-txaction “appointed” only.
        // but later:
        //   > Other event-txaction with parameter “transaction_status” may follow (e.g. “paid”, “debit”, ...).
        //   > The new “txaction” can then be “paid/pending”, “paid/completed”, ... or “failed/completed”.
        //
        // So, even if Payone changes API and such case happens - we expect to switch CT transaction state to PENDING, not SUCCESS.
        super.processingPendingNotificationAboutUnknownTransactionAddsChargeTransactionWithStatePending(testee, ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processingCompletedNotificationAboutUnknownTransactionAddsChargeTransactionWithStateSuccess() throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentOneAuthPending20EuroCC();
        payment.getTransactions().clear();

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

        assertThat(updateActions).as("# of payment update actions").hasSize(4);
        assertThat(updateActions).as("added transaction")
                .filteredOn(u -> u.getAction().equals("addTransaction"))
                .usingElementComparatorOnFields(
                        "transaction.type", "transaction.amount", "transaction.state", "transaction.timestamp")
                .containsOnlyOnce(transaction);

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
    public void processingCompletedNotificationForPendingChargeTransactionDoesNotChangeState() throws Exception {
        final Payment payment = processingCompletedNotificationForPendingChargeTransactionDoesNotChangeStateWireframe();
        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }

    @Test
    public void orderServiceIsNotCalledWhenIsUpdateOrderPaymentStateFalse() throws Exception {
        // this test is similar to #processingCompletedNotificationForPendingChargeTransactionDoesNotChangeState(),
        // but #isUpdateOrderPaymentState() is false, so update order actions should be skipped
        when(tenantConfig.isUpdateOrderPaymentState()).thenReturn(false);

        processingCompletedNotificationForPendingChargeTransactionDoesNotChangeStateWireframe();
        verifyUpdateOrderActionsNotCalled();
    }

    private Payment processingCompletedNotificationForPendingChargeTransactionDoesNotChangeStateWireframe() throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentOneChargePending20Euro();
        final Transaction chargeTransaction = payment.getTransactions().get(0);

        notification.setSequencenumber(chargeTransaction.getInteractionId());

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, timestamp);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions).as("# of payment update actions").hasSize(3);
        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);
        return payment;
    }
}
