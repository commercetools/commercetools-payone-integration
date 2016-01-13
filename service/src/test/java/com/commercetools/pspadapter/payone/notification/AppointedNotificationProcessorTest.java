package com.commercetools.pspadapter.payone.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.collect.ImmutableMap;
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

    private static final Integer millis = 1450365542;
    private static final LocalDateTime timestamp = LocalDateTime.ofEpochSecond(millis, 0, ZoneOffset.UTC);

    @Mock
    private CommercetoolsClient client;

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
        notification.setCurrency("EUR");
        notification.setTxtime(millis.toString());
        notification.setSequencenumber("0");
        notification.setTxaction(NotificationAction.APPOINTED);
        notification.setTransactionStatus(TransactionStatus.COMPLETED);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processesNotificationAboutNewTransaction() throws Exception {
        Payment payment = testHelper.dummyPaymentOneAuthPending20Euro();
        payment.getTransactions().clear();

        // act
        final boolean wasNotificationProcessed = testee.processTransactionStatusNotification(notification, payment);

        // assert
        assertThat(wasNotificationProcessed).as("notification processing result").isTrue();
        verify(client).complete(paymentRequestCaptor.capture());

        final List<? extends UpdateAction<Payment>> updateActions = paymentRequestCaptor.getValue().getUpdateActions();

        MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());
        AddTransaction transaction = AddTransaction.of(TransactionDraftBuilder
                .of(TransactionType.AUTHORIZATION, amount, ZonedDateTime.of(timestamp, ZoneId.of("UTC")))
                .state(TransactionState.SUCCESS)
                .interactionId(notification.getSequencenumber())
                .build());

        AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                ImmutableMap.of(
                        CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.of(timestamp, ZoneId.of("UTC")),
                        CustomFieldKeys.SEQUENCE_NUMBER_FIELD, notification.getSequencenumber(),
                        CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                        CustomFieldKeys.NOTIFICATION_FIELD, notification.toString()));

        assertThat(updateActions).isNotEmpty().hasSize(2);
        assertThat(updateActions).filteredOn(u -> u.getAction().equals("addTransaction"))
                .usingElementComparatorOnFields("transaction.type", "transaction.amount", "transaction.state", "transaction.timestamp")
                .containsOnlyOnce(transaction);
        assertThat(updateActions).filteredOn(u -> u.getAction().equals("addInterfaceInteraction"))
                .usingElementComparatorOnFields("fields")
                .containsOnly(interfaceInteraction);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processesNotificationAboutExistingTransaction() throws Exception {
        // arrange
        Payment payment = testHelper.dummyPaymentOneAuthPending20Euro();

        // act
        final boolean wasNotificationProcessed = testee.processTransactionStatusNotification(notification, payment);

        // assert
        assertThat(wasNotificationProcessed).as("notification processing result").isTrue();
        verify(client).complete(paymentRequestCaptor.capture());

        final List<? extends UpdateAction<Payment>> updateActions = paymentRequestCaptor.getValue().getUpdateActions();
        AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                ImmutableMap.of(
                        CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.of(timestamp, ZoneId.of("UTC")),
                        CustomFieldKeys.SEQUENCE_NUMBER_FIELD, notification.getSequencenumber(),
                        CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                        CustomFieldKeys.NOTIFICATION_FIELD, notification.toString()));

        assertThat(updateActions).isNotEmpty().hasSize(3);
        assertThat(updateActions).filteredOn(u -> u.getAction().equals("changeTransactionState"))
            .containsOnly(ChangeTransactionState.of(TransactionState.SUCCESS, payment.getTransactions().get(0).getId()));
        assertThat(updateActions).filteredOn(u -> u.getAction().equals("changeTransactionInteractionId"))
            .containsOnly(ChangeTransactionInteractionId.of(notification.getSequencenumber(), payment.getTransactions().get(0).getId()));
        assertThat(updateActions).filteredOn(u -> u.getAction().equals("addInterfaceInteraction"))
                .usingElementComparatorOnFields("fields")
                .containsOnly(interfaceInteraction);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void processesNotificationAboutExistingTransactionRequiringNoStateChange() throws Exception {
        // arrange
        //modify transaction status to omit ChangeTransactionState Action
        notification.setTransactionStatus(TransactionStatus.PENDING);
        Payment payment = testHelper.dummyPaymentAuthSuccess();

        // act
        final boolean wasNotificationProcessed = testee.processTransactionStatusNotification(notification, payment);

        // assert
        assertThat(wasNotificationProcessed).as("notification processing result").isTrue();
        verify(client).complete(paymentRequestCaptor.capture());

        final List<? extends UpdateAction<Payment>> updateActions = paymentRequestCaptor.getValue().getUpdateActions();
        AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                ImmutableMap.of(
                        CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.of(timestamp, ZoneId.of("UTC")),
                        CustomFieldKeys.SEQUENCE_NUMBER_FIELD, notification.getSequencenumber(),
                        CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                        CustomFieldKeys.NOTIFICATION_FIELD, notification.toString()));

        assertThat(updateActions).isNotEmpty().hasSize(2);
        assertThat(updateActions).filteredOn(u -> u.getAction().equals("changeTransactionInteractionId"))
                .containsOnly(ChangeTransactionInteractionId.of(notification.getSequencenumber(), payment.getTransactions().get(0).getId()));
        assertThat(updateActions).filteredOn(u -> u.getAction().equals("addInterfaceInteraction"))
                .usingElementComparatorOnFields("fields")
                .containsOnly(interfaceInteraction);
    }
}