package com.commercetools.pspadapter.payone.notification.common;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import com.commercetools.pspadapter.payone.notification.BaseNotificationProcessorTest;
import com.google.common.collect.ImmutableList;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.payments.commands.updateactions.*;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus.PENDING;
import static io.sphere.sdk.payments.TransactionState.SUCCESS;
import static io.sphere.sdk.payments.TransactionType.AUTHORIZATION;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
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
        final AddTransaction transaction = AddTransaction.of(
                TransactionDraftBuilder.of(AUTHORIZATION, amount, TXTIME_ZONED_DATE_TIME)
                        .state(TransactionState.PENDING)
                        .interactionId(notification.getSequencenumber())
                        .build());

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions)
                .filteredOn(u -> u.getAction().equals("addTransaction"))
                .usingElementComparatorOnFields(
                        "transaction.type",
                        "transaction.amount",
                        "transaction.state",
                        "transaction.timestamp",
                        "transaction.interactionId")
                .containsOnlyOnce(transaction);
        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);
        assertThat(updateActions).as("# of update actions").hasSize(4);

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
        final AddTransaction transaction = AddTransaction.of(TransactionDraftBuilder
                .of(AUTHORIZATION, amount, TXTIME_ZONED_DATE_TIME)
                .state(SUCCESS)
                .interactionId(notification.getSequencenumber())
                .build());

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions).as("update actions list")
                .containsExactlyInAnyOrder(transaction, interfaceInteraction, statusInterfaceCode, statusInterfaceText);

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
        final AddTransaction transaction = AddTransaction.of(
                TransactionDraftBuilder.of(TransactionType.CHARGE, amount, TXTIME_ZONED_DATE_TIME)
                        .state(TransactionState.PENDING)
                        .interactionId(notification.getSequencenumber())
                        .build());

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions)
                .filteredOn(u -> u.getAction().equals("addTransaction"))
                .usingElementComparatorOnFields(
                        "transaction.type",
                        "transaction.amount",
                        "transaction.state",
                        "transaction.timestamp",
                        "transaction.interactionId")
                .containsOnlyOnce(transaction);
        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);
        assertThat(updateActions).as("# of update actions").hasSize(4);

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
        final AddTransaction transaction = AddTransaction.of(TransactionDraftBuilder
                .of(TransactionType.CHARGE, amount, TXTIME_ZONED_DATE_TIME)
                .state(TransactionState.PENDING)
                .interactionId(notification.getSequencenumber())
                .build());

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions)
                .filteredOn(u -> u.getAction().equals("addTransaction"))
                .usingElementComparatorOnFields(
                        "transaction.type",
                        "transaction.amount",
                        "transaction.state",
                        "transaction.timestamp",
                        "transaction.interactionId")
                .containsOnlyOnce(transaction);
        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);
        assertThat(updateActions).as("# of update actions").hasSize(4);

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
        final AddTransaction transaction = AddTransaction.of(TransactionDraftBuilder
                .of(TransactionType.CHARGE, amount, TXTIME_ZONED_DATE_TIME)
                .state(TransactionState.PENDING)
                .interactionId(notification.getSequencenumber())
                .build());

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions)
                .filteredOn(u -> u.getAction().equals("addTransaction"))
                .usingElementComparatorOnFields(
                        "transaction.type",
                        "transaction.amount",
                        "transaction.state",
                        "transaction.timestamp",
                        "transaction.interactionId")
                .containsOnlyOnce(transaction);
        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);
        assertThat(updateActions).as("# of update actions").hasSize(4);

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
        final AddTransaction transaction = AddTransaction.of(TransactionDraftBuilder
                .of(TransactionType.CHARGE, amount, TXTIME_ZONED_DATE_TIME)
                .state(TransactionState.PENDING)
                .interactionId(notification.getSequencenumber())
                .build());

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions)
                .filteredOn(u -> u.getAction().equals("addTransaction"))
                .usingElementComparatorOnFields(
                        "transaction.type",
                        "transaction.amount",
                        "transaction.state",
                        "transaction.timestamp",
                        "transaction.interactionId")
                .containsOnlyOnce(transaction);
        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);
        assertThat(updateActions).as("# of update actions").hasSize(4);

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

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions)
                .filteredOn(u -> u.getAction().equals("changeTransactionInteractionId"))
                .containsOnly(
                        ChangeTransactionInteractionId.of(notification.getSequencenumber(),
                                payment.getTransactions().get(0).getId()));
        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);
        assertThat(updateActions).as("# of update actions").hasSize(4);

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

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions)
                .filteredOn(u -> u.getAction().equals("changeTransactionInteractionId"))
                .containsOnly(
                        ChangeTransactionInteractionId.of(notification.getSequencenumber(),
                                payment.getTransactions().get(0).getId()));
        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);
        assertThat(updateActions).as("# of update actions").hasSize(4);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapsAppointedPendingToPendingAuthorizationTransaction() throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentOneAuthPending20EuroCC();
        notification.setTransactionStatus(PENDING);

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);


        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);
        assertThat(updateActions).as("# of update actions").hasSize(3);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapsAppointedCompletedToPendingAuthorizationTransactionAndChangesStateToSuccess() throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentOneAuthPending20EuroCC();

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions).as("update actions list")
                .containsExactlyInAnyOrder(
                        ChangeTransactionState.of(SUCCESS, payment.getTransactions().get(0).getId()),
                        interfaceInteraction, statusInterfaceCode, statusInterfaceText);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
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


        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);
        assertThat(updateActions).as("# of update actions").hasSize(3);

        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapsAppointedCompletedToSuccessfulAuthorizationTransaction() throws Exception {
        Payment payment = mapsAppointedCompleteToSuccessfulAuthorizationTransactionWireframe();
        verifyUpdateOrderActions(payment, ORDER_PAYMENT_STATE);
    }

    @Test
    public void orderServiceIsNotCalledWhenIsUpdateOrderPaymentStateFalse() throws Exception {
        // this test is similar to #mapsAppointedCompletedToSuccessfulAuthorizationTransaction(),
        // but #isUpdateOrderPaymentState() is false, so update order actions should be skipped
        when(tenantConfig.isUpdateOrderPaymentState()).thenReturn(false);

        mapsAppointedCompleteToSuccessfulAuthorizationTransactionWireframe();
        verifyUpdateOrderActionsNotCalled();
    }

    @Test
    public void whenAuthTransactionIsInitial_ChangeTransactionStateISAdded() throws Exception {
        Payment paymentAuthPending = testHelper.dummyPaymentOneAuthInitial20EuroCC();
        ImmutableList<UpdateAction<Payment>> authPendingUpdates = testee.createPaymentUpdates(paymentAuthPending, notification);
        assertThat(authPendingUpdates).containsOnlyOnce(ChangeTransactionState.of(
                notification.getTransactionStatus().getCtTransactionState(), paymentAuthPending.getTransactions().get(0).getId()));

        verify_isNotCompletedTransaction_called(TransactionState.INITIAL, AUTHORIZATION);
    }

    @Test
    public void whenAuthTransactionIsPending_ChangeTransactionStateISAdded() throws Exception {
        Payment paymentAuthPending = testHelper.dummyPaymentOneAuthPending20EuroCC();
        ImmutableList<UpdateAction<Payment>> authPendingUpdates = testee.createPaymentUpdates(paymentAuthPending, notification);
        assertThat(authPendingUpdates).containsOnlyOnce(ChangeTransactionState.of(
                notification.getTransactionStatus().getCtTransactionState(), paymentAuthPending.getTransactions().get(0).getId()));

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

        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);
        assertThat(updateActions).as("# of update actions").hasSize(3);
        return payment;
    }

}
