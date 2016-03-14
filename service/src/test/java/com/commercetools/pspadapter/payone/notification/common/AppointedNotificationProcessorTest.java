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
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionInteractionId;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionState;
import io.sphere.sdk.payments.commands.updateactions.SetAuthorization;
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
 * @author fhaertig
 * @since 11.01.16
 */
@RunWith(MockitoJUnitRunner.class)
public class AppointedNotificationProcessorTest {

    private static final Integer seconds = 1450365542;
    private static final ZonedDateTime TXTIME_ZONED_DATE_TIME =
            ZonedDateTime.of(LocalDateTime.ofEpochSecond(seconds, 0, ZoneOffset.UTC), ZoneId.of("UTC"));

    private static final ZonedDateTime TXTIME_PLUS_7_DAYS_ZONED_DATE_TIME = TXTIME_ZONED_DATE_TIME.plusDays(7);

    @Mock
    private BlockingSphereClient client;

    @InjectMocks
    private AppointedNotificationProcessor testee;

    @Captor
    private ArgumentCaptor<PaymentUpdateCommand> paymentRequestCaptor;

    private final PaymentTestHelper testHelper = new PaymentTestHelper();

    private Notification notification;

    @Before
    public void setUp() throws Exception {
        notification = new Notification();
        notification.setPrice("20.00");
        notification.setBalance("0.00");
        notification.setReceivable("0.00");
        notification.setCurrency("EUR");
        notification.setTxtime(seconds.toString());
        notification.setSequencenumber("0");
        notification.setTxaction(NotificationAction.APPOINTED);
        notification.setTransactionStatus(TransactionStatus.COMPLETED);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processingNotificationWithZeroBalanceAboutUnknownTransactionAddsAuthorizationTransactionWithStateSuccess() throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentNoTransaction20EuroPlanned();

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        verify(client).executeBlocking(paymentRequestCaptor.capture());

        final List<? extends UpdateAction<Payment>> updateActions = paymentRequestCaptor.getValue().getUpdateActions();

        final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());
        final AddTransaction transaction = AddTransaction.of(TransactionDraftBuilder
                .of(TransactionType.AUTHORIZATION, amount, TXTIME_ZONED_DATE_TIME)
                .state(TransactionState.SUCCESS)
                .interactionId(notification.getSequencenumber())
                .build());

        final AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(
                CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                ImmutableMap.of(
                        CustomFieldKeys.TIMESTAMP_FIELD, TXTIME_ZONED_DATE_TIME,
                        CustomFieldKeys.SEQUENCE_NUMBER_FIELD, notification.getSequencenumber(),
                        CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                        CustomFieldKeys.NOTIFICATION_FIELD, notification.toString()));

        assertThat(updateActions).filteredOn(u -> u.getAction().equals("setAuthorization"))
                .containsOnly(SetAuthorization.of(payment.getAmountPlanned(), TXTIME_PLUS_7_DAYS_ZONED_DATE_TIME));
        assertThat(updateActions).filteredOn(u -> u.getAction().equals("addTransaction"))
                .usingElementComparatorOnFields(
                        "transaction.type", "transaction.amount", "transaction.state", "transaction.timestamp")
                .containsOnlyOnce(transaction);
        assertThat(updateActions).filteredOn(u -> u.getAction().equals("addInterfaceInteraction"))
                .usingElementComparatorOnFields("fields")
                .containsOnly(interfaceInteraction);
        assertThat(updateActions).as("# of update actions").hasSize(3);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processingNotificationWithNonZeroBalanceAboutUnknownTransactionAddsChargeTransactionWithStatePending() throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentNoTransaction20EuroPlanned();

        notification.setBalance("20.00");

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        verify(client).executeBlocking(paymentRequestCaptor.capture());

        final List<? extends UpdateAction<Payment>> updateActions = paymentRequestCaptor.getValue().getUpdateActions();

        final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());
        final AddTransaction transaction = AddTransaction.of(TransactionDraftBuilder
                .of(TransactionType.CHARGE, amount, TXTIME_ZONED_DATE_TIME)
                .state(TransactionState.PENDING)
                .interactionId(notification.getSequencenumber())
                .build());

        final AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(
                CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                ImmutableMap.of(
                        CustomFieldKeys.TIMESTAMP_FIELD, TXTIME_ZONED_DATE_TIME,
                        CustomFieldKeys.SEQUENCE_NUMBER_FIELD, notification.getSequencenumber(),
                        CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                        CustomFieldKeys.NOTIFICATION_FIELD, notification.toString()));

        assertThat(updateActions).filteredOn(u -> u.getAction().equals("addTransaction"))
                .usingElementComparatorOnFields(
                        "transaction.type", "transaction.amount", "transaction.state", "transaction.timestamp")
                .containsOnlyOnce(transaction);
        assertThat(updateActions).filteredOn(u -> u.getAction().equals("addInterfaceInteraction"))
                .usingElementComparatorOnFields("fields")
                .containsOnly(interfaceInteraction);
        assertThat(updateActions).as("# of update actions").hasSize(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processingNotificationForPendingAuthorizationTransactionChangesStateToSuccess() throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentOneAuthPending20EuroCC();
        notification.setBalance("20.00");

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        verify(client).executeBlocking(paymentRequestCaptor.capture());

        final List<? extends UpdateAction<Payment>> updateActions = paymentRequestCaptor.getValue().getUpdateActions();
        AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(
                CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                ImmutableMap.of(
                        CustomFieldKeys.TIMESTAMP_FIELD, TXTIME_ZONED_DATE_TIME,
                        CustomFieldKeys.SEQUENCE_NUMBER_FIELD, notification.getSequencenumber(),
                        CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                        CustomFieldKeys.NOTIFICATION_FIELD, notification.toString()));

        assertThat(updateActions).as("setAuthorization action")
                .filteredOn(u -> u.getAction().equals("setAuthorization"))
                .containsOnly(SetAuthorization.of(payment.getAmountPlanned(), TXTIME_PLUS_7_DAYS_ZONED_DATE_TIME));

        assertThat(updateActions).as("changeTransactionState action")
                .filteredOn(u -> u.getAction().equals("changeTransactionState"))
                .containsOnly(
                        ChangeTransactionState.of(TransactionState.SUCCESS, payment.getTransactions().get(0).getId()));

        assertThat(updateActions).as("changeTransactionInteractionId action")
                .filteredOn(u -> u.getAction().equals("changeTransactionInteractionId"))
                .containsOnly(ChangeTransactionInteractionId.of(
                        notification.getSequencenumber(),
                        payment.getTransactions().get(0).getId()));

        assertThat(updateActions).as("addInterfaceInteraction action")
                .filteredOn(u -> u.getAction().equals("addInterfaceInteraction"))
                .usingElementComparatorOnFields("fields")
                .containsOnly(interfaceInteraction);

        assertThat(updateActions).as("# of update actions").hasSize(4);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processingNotificationForSuccessfulAuthorizationTransactionDoesNotChangeState() throws Exception {
        // arrange
        //modify transaction status to omit ChangeTransactionState Action
        notification.setTransactionStatus(TransactionStatus.PENDING);
        Payment payment = testHelper.dummyPaymentAuthSuccess();

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        verify(client).executeBlocking(paymentRequestCaptor.capture());

        final List<? extends UpdateAction<Payment>> updateActions = paymentRequestCaptor.getValue().getUpdateActions();
        AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                ImmutableMap.of(
                        CustomFieldKeys.TIMESTAMP_FIELD, TXTIME_ZONED_DATE_TIME,
                        CustomFieldKeys.SEQUENCE_NUMBER_FIELD, notification.getSequencenumber(),
                        CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                        CustomFieldKeys.NOTIFICATION_FIELD, notification.toString()));

        assertThat(updateActions).filteredOn(u -> u.getAction().equals("changeTransactionInteractionId"))
                .containsOnly(ChangeTransactionInteractionId.of(notification.getSequencenumber(), payment.getTransactions().get(0).getId()));
        assertThat(updateActions).filteredOn(u -> u.getAction().equals("addInterfaceInteraction"))
                .usingElementComparatorOnFields("fields")
                .containsOnly(interfaceInteraction);
        assertThat(updateActions).as("# of update actions").hasSize(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processingNotificationForPendingChargeTransactionDoesNotChangeState() throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentOneChargePending20Euro();

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        verify(client).executeBlocking(paymentRequestCaptor.capture());

        final List<? extends UpdateAction<Payment>> updateActions = paymentRequestCaptor.getValue().getUpdateActions();
        final AddInterfaceInteraction interfaceInteraction =
                AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                        ImmutableMap.of(
                                CustomFieldKeys.TIMESTAMP_FIELD, TXTIME_ZONED_DATE_TIME,
                                CustomFieldKeys.SEQUENCE_NUMBER_FIELD, notification.getSequencenumber(),
                                CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                                CustomFieldKeys.NOTIFICATION_FIELD, notification.toString()));

        assertThat(updateActions).as("content of payment update action")
                .filteredOn(u -> u.getAction().equals("addInterfaceInteraction"))
                .usingElementComparatorOnFields("fields")
                .containsOnly(interfaceInteraction);
        assertThat(updateActions).as("# of update actions").hasSize(1);
    }
}
