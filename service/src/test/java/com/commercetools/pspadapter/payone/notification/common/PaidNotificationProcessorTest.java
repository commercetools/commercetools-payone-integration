package com.commercetools.pspadapter.payone.notification.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionState;
import io.sphere.sdk.payments.commands.updateactions.SetAmountPaid;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import util.PaymentTestHelper;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author Jan Wolter
 */
@RunWith(MockitoJUnitRunner.class)
public class PaidNotificationProcessorTest {

    private static final Integer millis = 1450365542;
    private static final ZonedDateTime timestamp =
        ZonedDateTime.of(LocalDateTime.ofEpochSecond(millis, 0, ZoneOffset.UTC), ZoneId.of("UTC"));

    @Mock
    private BlockingSphereClient client;

    @InjectMocks
    private PaidNotificationProcessor testee;

    @Captor
    private ArgumentCaptor<PaymentUpdateCommand> paymentRequestCaptor;

    private final PaymentTestHelper testHelper = new PaymentTestHelper();

    private Notification notification;

    @Before
    public void setUp() throws Exception {
        notification = new Notification();
        notification.setPrice("20.00");
        notification.setCurrency("EUR");
        notification.setTxtime(millis.toString());
        notification.setSequencenumber("23");
        notification.setTxaction(NotificationAction.PAID);
        notification.setTransactionStatus(TransactionStatus.COMPLETED);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processingPendingNotificationAboutUnknownTransactionAddsChargeTransactionWithStatePending()
            throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentOneAuthPending20EuroCC();
        payment.getTransactions().clear();

        notification.setTransactionStatus(TransactionStatus.PENDING);

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        verify(client).executeBlocking(paymentRequestCaptor.capture());

        final List<? extends UpdateAction<Payment>> updateActions = paymentRequestCaptor.getValue().getUpdateActions();

        final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());
        final AddTransaction transaction = AddTransaction.of(TransactionDraftBuilder
                .of(TransactionType.CHARGE, amount, timestamp)
                .state(TransactionState.PENDING)
                .interactionId(notification.getSequencenumber())
                .build());

        final AddInterfaceInteraction interfaceInteraction =
                AddInterfaceInteraction.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                        ImmutableMap.of(
                                CustomFieldKeys.TIMESTAMP_FIELD, timestamp,
                                CustomFieldKeys.SEQUENCE_NUMBER_FIELD, notification.getSequencenumber(),
                                CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                                CustomFieldKeys.NOTIFICATION_FIELD, notification.toString()));

        assertThat(updateActions).as("# of payment update actions").hasSize(2);
        assertThat(updateActions).as("added transaction")
                .filteredOn(u -> u.getAction().equals("addTransaction"))
                .usingElementComparatorOnFields(
                        "transaction.type", "transaction.amount", "transaction.state", "transaction.timestamp")
                .containsOnlyOnce(transaction);

        assertThat(updateActions).as("added interaction")
                .filteredOn(u -> u.getAction().equals("addInterfaceInteraction"))
                .usingElementComparatorOnFields("fields")
                .containsOnly(interfaceInteraction);
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
        verify(client).executeBlocking(paymentRequestCaptor.capture());

        final List<? extends UpdateAction<Payment>> updateActions = paymentRequestCaptor.getValue().getUpdateActions();

        final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());
        final AddTransaction transaction = AddTransaction.of(TransactionDraftBuilder
                .of(TransactionType.CHARGE, amount, timestamp)
                .state(TransactionState.SUCCESS)
                .interactionId(notification.getSequencenumber())
                .build());

        final AddInterfaceInteraction interfaceInteraction =
                AddInterfaceInteraction.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                        ImmutableMap.of(
                                CustomFieldKeys.TIMESTAMP_FIELD, timestamp,
                                CustomFieldKeys.SEQUENCE_NUMBER_FIELD, notification.getSequencenumber(),
                                CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                                CustomFieldKeys.NOTIFICATION_FIELD, notification.toString()));

        assertThat(updateActions).as("# of payment update actions").hasSize(3);
        assertThat(updateActions).as("added transaction")
                .filteredOn(u -> u.getAction().equals("addTransaction"))
                .usingElementComparatorOnFields(
                        "transaction.type", "transaction.amount", "transaction.state", "transaction.timestamp")
                .containsOnlyOnce(transaction);

        assertThat(updateActions).as("amount paid")
                .filteredOn(u -> u.getAction().equals("setAmountPaid"))
                .containsOnly(SetAmountPaid.of(MoneyImpl.of(notification.getPrice(), notification.getCurrency())));

        assertThat(updateActions).as("added interaction")
                .filteredOn(u -> u.getAction().equals("addInterfaceInteraction"))
                .usingElementComparatorOnFields("fields")
                .containsOnly(interfaceInteraction);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processingPendingNotificationForPendingChargeTransactionDoesNotChangeState() throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentOneChargePending20Euro();

        notification.setSequencenumber(payment.getTransactions().get(0).getInteractionId());
        notification.setTransactionStatus(TransactionStatus.PENDING);

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        verify(client).executeBlocking(paymentRequestCaptor.capture());

        final List<? extends UpdateAction<Payment>> updateActions = paymentRequestCaptor.getValue().getUpdateActions();

        final AddInterfaceInteraction interfaceInteraction =
                AddInterfaceInteraction.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                        ImmutableMap.of(
                                CustomFieldKeys.TIMESTAMP_FIELD, timestamp,
                                CustomFieldKeys.SEQUENCE_NUMBER_FIELD, notification.getSequencenumber(),
                                CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                                CustomFieldKeys.NOTIFICATION_FIELD, notification.toString()));

        assertThat(updateActions).as("# of payment update actions").hasSize(1);
        assertThat(updateActions).as("added interaction")
                .filteredOn(u -> u.getAction().equals("addInterfaceInteraction"))
                .usingElementComparatorOnFields("fields")
                .containsOnly(interfaceInteraction);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processingCompletedNotificationForPendingChargeTransactionChangesStateToSuccess() throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentOneChargePending20Euro();
        final Transaction chargeTransaction = payment.getTransactions().get(0);

        notification.setSequencenumber(chargeTransaction.getInteractionId());
        notification.setReceivable("20.00");
        notification.setBalance("0.00");


        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        verify(client).executeBlocking(paymentRequestCaptor.capture());

        final List<? extends UpdateAction<Payment>> updateActions = paymentRequestCaptor.getValue().getUpdateActions();

        final AddInterfaceInteraction interfaceInteraction =
                AddInterfaceInteraction.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                        ImmutableMap.of(
                                CustomFieldKeys.TIMESTAMP_FIELD, timestamp,
                                CustomFieldKeys.SEQUENCE_NUMBER_FIELD, notification.getSequencenumber(),
                                CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                                CustomFieldKeys.NOTIFICATION_FIELD, notification.toString()));

        assertThat(updateActions).as("# of payment update actions").hasSize(3);
        assertThat(updateActions).as("transaction state change")
                .filteredOn(u -> u.getAction().equals("changeTransactionState"))
                .containsOnly(ChangeTransactionState.of(TransactionState.SUCCESS, chargeTransaction.getId()));

        assertThat(updateActions).as("amount paid")
                .filteredOn(u -> u.getAction().equals("setAmountPaid"))
                .containsOnly(SetAmountPaid.of(MoneyImpl.of(notification.getPrice(), notification.getCurrency())));

        assertThat(updateActions).as("added interaction")
                .filteredOn(u -> u.getAction().equals("addInterfaceInteraction"))
                .usingElementComparatorOnFields("fields")
                .containsOnly(interfaceInteraction);
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
        verify(client).executeBlocking(paymentRequestCaptor.capture());

        final List<? extends UpdateAction<Payment>> updateActions = paymentRequestCaptor.getValue().getUpdateActions();

        final AddInterfaceInteraction interfaceInteraction =
                AddInterfaceInteraction.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                        ImmutableMap.of(
                                CustomFieldKeys.TIMESTAMP_FIELD, timestamp,
                                CustomFieldKeys.SEQUENCE_NUMBER_FIELD, notification.getSequencenumber(),
                                CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                                CustomFieldKeys.NOTIFICATION_FIELD, notification.toString()));

        assertThat(updateActions).as("# of payment update actions").hasSize(1);
        assertThat(updateActions).as("added interaction")
                .filteredOn(u -> u.getAction().equals("addInterfaceInteraction"))
                .usingElementComparatorOnFields("fields")
                .containsOnly(interfaceInteraction);
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
        verify(client).executeBlocking(paymentRequestCaptor.capture());

        final List<? extends UpdateAction<Payment>> updateActions = paymentRequestCaptor.getValue().getUpdateActions();

        final AddInterfaceInteraction interfaceInteraction =
                AddInterfaceInteraction.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                        ImmutableMap.of(
                                CustomFieldKeys.TIMESTAMP_FIELD, timestamp,
                                CustomFieldKeys.SEQUENCE_NUMBER_FIELD, notification.getSequencenumber(),
                                CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                                CustomFieldKeys.NOTIFICATION_FIELD, notification.toString()));

        assertThat(updateActions).as("# of payment update actions").hasSize(1);
        assertThat(updateActions).as("added interaction")
                .filteredOn(u -> u.getAction().equals("addInterfaceInteraction"))
                .usingElementComparatorOnFields("fields")
                .containsOnly(interfaceInteraction);
    }
}
