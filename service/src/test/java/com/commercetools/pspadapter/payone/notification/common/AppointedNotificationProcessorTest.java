package com.commercetools.pspadapter.payone.notification.common;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import com.commercetools.pspadapter.payone.notification.BaseNotificationProcessorTest;
import com.google.common.collect.ImmutableList;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.updateactions.*;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus.PENDING;
import static io.sphere.sdk.payments.TransactionState.SUCCESS;
import static io.sphere.sdk.payments.TransactionType.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static util.UpdatePaymentTestHelper.*;

/**
 * @author fhaertig
 * @since 11.01.16
 */
@RunWith(MockitoJUnitRunner.class)
public class AppointedNotificationProcessorTest extends BaseNotificationProcessorTest {

    private static final Integer seconds = 1450365542;
    private static final ZonedDateTime TXTIME_ZONED_DATE_TIME =
            ZonedDateTime.of(LocalDateTime.ofEpochSecond(seconds, 0, ZoneOffset.UTC), ZoneId.of("UTC"));

    @InjectMocks
    protected AppointedNotificationProcessor testee;

    // Payone "appointed" is used to be mapped to CTP "pending" state
    private static final PaymentState ORDER_PAYMENT_STATE = PaymentState.PENDING;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        notification = new Notification();
        notification.setPrice("20.00");
        notification.setBalance("0.00");
        notification.setReceivable("0.00");
        notification.setCurrency("EUR");
        notification.setTxtime(seconds.toString());
        notification.setSequencenumber("0");
        notification.setTxaction(NotificationAction.APPOINTED);
        notification.setTransactionStatus(TransactionStatus.COMPLETED);

        when(paymentToOrderStateMapper.mapPaymentToOrderState(any(Payment.class)))
                .thenReturn(ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapsAppointedPendingForUnknownFirstTransactionWithSequenceNumber0AndZeroBalanceToNewAuthorizationTransactionWithStatePending()
            throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentNoTransaction20EuroPlanned();
        notification.setTransactionStatus(PENDING);

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());
        final AddTransaction addTransaction = getAddTransaction(AUTHORIZATION, TransactionState.PENDING, amount,
                TXTIME_ZONED_DATE_TIME, notification.getSequencenumber());
        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions)
                .containsExactlyInAnyOrder(addTransaction, interfaceInteraction, statusInterfaceCode, statusInterfaceText);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapsAppointedCompletedForUnknownFirstTransactionWithSequenceNumber0AndZeroBalanceToNewAuthorizationTransactionWithStateSuccess()
            throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentNoTransaction20EuroPlanned();

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());
        final AddTransaction addTransaction = getAddTransaction(AUTHORIZATION, TransactionState.SUCCESS, amount,
                TXTIME_ZONED_DATE_TIME, notification.getSequencenumber());
        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions)
                .containsExactlyInAnyOrder(addTransaction, interfaceInteraction, statusInterfaceCode, statusInterfaceText);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapsAppointedPendingForUnknownFirstTransactionWithSequenceNumber0AndNonZeroBalanceToNewChargeTransactionWithStatePending()
            throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentNoTransaction20EuroPlanned();
        notification.setTransactionStatus(PENDING);
        notification.setBalance("20.00");

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());
        final AddTransaction addTransaction = getAddTransaction(TransactionType.CHARGE, TransactionState.PENDING, amount,
                TXTIME_ZONED_DATE_TIME, notification.getSequencenumber());
        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions)
                .containsExactlyInAnyOrder(addTransaction, interfaceInteraction, statusInterfaceCode, statusInterfaceText);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapsAppointedCompletedForUnknownFirstTransactionWithSequenceNumber0AndNonZeroBalanceToNewChargeTransactionWithStatePending()
            throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentNoTransaction20EuroPlanned();
        notification.setBalance("20.00");

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());
        final AddTransaction addTransaction = getAddTransaction(TransactionType.CHARGE, TransactionState.PENDING, amount,
                TXTIME_ZONED_DATE_TIME, notification.getSequencenumber());
        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions)
                .containsExactlyInAnyOrder(addTransaction, interfaceInteraction, statusInterfaceCode, statusInterfaceText);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapsAppointedPendingForUnknownFirstTransactionWithSequenceNumber1ToNewChargeTransactionWithStatePending()
            throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentNoTransaction20EuroPlanned();
        notification.setSequencenumber("1");
        notification.setBalance("20.00");
        notification.setTransactionStatus(PENDING);

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());
        final AddTransaction addTransaction = getAddTransaction(TransactionType.CHARGE, TransactionState.PENDING, amount,
                TXTIME_ZONED_DATE_TIME, notification.getSequencenumber());
        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions)
                .containsExactlyInAnyOrder(addTransaction, interfaceInteraction, statusInterfaceCode, statusInterfaceText);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapsAppointedCompletedForUnknownFirstTransactionWithSequenceNumber1ToNewChargeTransactionWithStatePending()
            throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentNoTransaction20EuroPlanned();
        notification.setSequencenumber("1");
        notification.setBalance("20.00");

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());
        final AddTransaction addTransaction = getAddTransaction(TransactionType.CHARGE, TransactionState.PENDING, amount,
                TXTIME_ZONED_DATE_TIME, notification.getSequencenumber());
        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions)
                .containsExactlyInAnyOrder(addTransaction, interfaceInteraction, statusInterfaceCode, statusInterfaceText);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapsAppointedPendingWithSequenceNumber1ToPendingChargeTransactionWithInteractionId0AndUpdatesInteractionId()
            throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentOneChargePending20Euro();

        notification.setSequencenumber("1");
        notification.setTransactionStatus(PENDING);

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final ChangeTransactionInteractionId changeTransaction =
                getChangeTransactionInteractionId(notification.getSequencenumber(), payment.getTransactions().get(0).getId());
        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions)
                .containsExactlyInAnyOrder(changeTransaction, interfaceInteraction, statusInterfaceCode, statusInterfaceText);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapsAppointedCompletedWithSequenceNumber1ToPendingChargeTransactionWithInteractionId0AndUpdatesInteractionId()
            throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentOneChargePending20Euro();

        notification.setSequencenumber("1");

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final ChangeTransactionInteractionId chargeTransaction =
                getChangeTransactionInteractionId(notification.getSequencenumber(), payment.getTransactions().get(0).getId());
        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions)
                .containsExactlyInAnyOrder(chargeTransaction, interfaceInteraction, statusInterfaceCode, statusInterfaceText);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }

    @Test
    public void whenCurrentInitial_mapsAppointedPendingToPendingAuthorizationWithoutStateChange() throws Exception {
        mapsAppointedPendingToPendingAuthorizationWithoutStateChange(testHelper.dummyPaymentOneAuthInitial20EuroCC(), TransactionState.INITIAL);
    }

    @Test
    public void whenCurrentPending_mapsAppointedPendingToPendingAuthorizationWithoutStateChange() throws Exception {
        mapsAppointedPendingToPendingAuthorizationWithoutStateChange(testHelper.dummyPaymentOneAuthPending20EuroCC(), TransactionState.PENDING);
    }

    private void mapsAppointedPendingToPendingAuthorizationWithoutStateChange(final Payment payment, final TransactionState currentState) throws Exception {
        notification.setTransactionStatus(PENDING);

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions)
                .containsExactlyInAnyOrder(interfaceInteraction, statusInterfaceCode, statusInterfaceText);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
        verify_isNotCompletedTransaction_called(currentState, TransactionType.AUTHORIZATION);
    }

    @Test
    public void whenCurrentInitial_mapsAppointedCompletedToPendingSuccess() throws Exception {
        mapsAppointedCompletedToPendingSuccess(testHelper.dummyPaymentOneAuthInitial20EuroCC(), TransactionState.INITIAL);
    }

    @Test
    public void whenCurrentPending_mapsAppointedCompletedToPendingSuccess() throws Exception {
        mapsAppointedCompletedToPendingSuccess(testHelper.dummyPaymentOneAuthPending20EuroCC(), TransactionState.PENDING);
    }

    private void mapsAppointedCompletedToPendingSuccess(final Payment payment, final TransactionState currentState) throws Exception {
        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final ChangeTransactionState changeTransactionState = getChangeTransactionState(SUCCESS, payment.getTransactions().get(0).getId());
        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions)
                .containsExactlyInAnyOrder(changeTransactionState, interfaceInteraction, statusInterfaceCode, statusInterfaceText);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
        verify_isNotCompletedTransaction_called(currentState, TransactionType.AUTHORIZATION);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapsAppointedPendingToSuccessfulAuthorizationTransaction() throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentAuthSuccess();
        notification.setTransactionStatus(PENDING);

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions).containsExactlyInAnyOrder(interfaceInteraction, statusInterfaceCode, statusInterfaceText);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapsAppointedCompletedToSuccessfulAuthorizationTransaction() throws Exception {
        Payment payment = mapsAppointedCompleteToSuccessfulAuthorizationTransactionWireframe();
        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
        verify_isNotCompletedTransaction_called(TransactionState.SUCCESS, AUTHORIZATION);
    }

    @Test
    public void orderServiceIsNotCalledWhenIsUpdateOrderPaymentStateFalse() throws Exception {
        // this test is similar to #mapsAppointedCompletedToSuccessfulAuthorizationTransaction(),
        // but #isUpdateOrderPaymentState() is false, so update order actions should be skipped
        when(tenantConfig.isUpdateOrderPaymentState()).thenReturn(false);

        mapsAppointedCompleteToSuccessfulAuthorizationTransactionWireframe();
        verifyUpdateOrderActionsNotCalled();
        verify_isNotCompletedTransaction_called(TransactionState.SUCCESS, AUTHORIZATION);
    }

    @Test
    public void whenAuthTransactionIsInitial_ChangeTransactionStateToSuccessAdded() throws Exception {
        Payment paymentAuthPending = testHelper.dummyPaymentOneAuthInitial20EuroCC();
        ImmutableList<UpdateAction<Payment>> authPendingUpdates = testee.createPaymentUpdates(paymentAuthPending, notification);
        assertThat(authPendingUpdates).containsOnlyOnce(ChangeTransactionState.of(
                TransactionState.SUCCESS, paymentAuthPending.getTransactions().get(0).getId()));

        verify_isNotCompletedTransaction_called(TransactionState.INITIAL, AUTHORIZATION);
    }

    @Test
    public void whenAuthTransactionIsPending_ChangeTransactionStateToSuccessAdded() throws Exception {
        Payment paymentAuthPending = testHelper.dummyPaymentOneAuthPending20EuroCC();
        ImmutableList<UpdateAction<Payment>> authPendingUpdates = testee.createPaymentUpdates(paymentAuthPending, notification);
        assertThat(authPendingUpdates).containsOnlyOnce(ChangeTransactionState.of(
                TransactionState.SUCCESS, paymentAuthPending.getTransactions().get(0).getId()));

        verify_isNotCompletedTransaction_called(TransactionState.PENDING, AUTHORIZATION);
    }

    @Test
    public void whenAuthTransactionIsSuccess_ChangeTransactionStateNOTAdded() throws Exception {
        Payment paymentAuthSuccess = testHelper.dummyPaymentAuthSuccess();
        ImmutableList<UpdateAction<Payment>> authSuccessUpdates = testee.createPaymentUpdates(paymentAuthSuccess, notification);
        assertTransactionOfTypeIsNotAdded(authSuccessUpdates, ChangeTransactionState.class);

        verify_isNotCompletedTransaction_called(TransactionState.SUCCESS, AUTHORIZATION);
    }

    @Test
    public void whenAuthTransactionIsFailure_ChangeTransactionStateNOTAdded() throws Exception {
        Payment paymentAuthFailure = testHelper.dummyPaymentOneAuthFailure20EuroCC();
        ImmutableList<UpdateAction<Payment>> authFailureUpdates = testee.createPaymentUpdates(paymentAuthFailure, notification);
        assertTransactionOfTypeIsNotAdded(authFailureUpdates, ChangeTransactionState.class);

        verify_isNotCompletedTransaction_called(TransactionState.FAILURE, AUTHORIZATION);
    }

    private Payment mapsAppointedCompleteToSuccessfulAuthorizationTransactionWireframe() throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentAuthSuccess();

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions).containsExactlyInAnyOrder(interfaceInteraction, statusInterfaceCode, statusInterfaceText);
        return payment;
    }

}
