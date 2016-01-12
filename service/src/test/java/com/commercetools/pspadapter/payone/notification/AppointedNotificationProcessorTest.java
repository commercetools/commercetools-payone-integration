package com.commercetools.pspadapter.payone.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionInteractionId;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionState;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import util.PaymentTestHelper;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author fhaertig
 * @date 11.01.16
 */
@RunWith(MockitoJUnitRunner.class)
public class AppointedNotificationProcessorTest {

    private static final Integer millis = 1450365542;
    private static final LocalDateTime timestamp = LocalDateTime.ofEpochSecond(millis, 0, ZoneOffset.UTC);

    @Mock
    private CommercetoolsClient client;

    private final PaymentTestHelper testHelper = new PaymentTestHelper();

    private Notification notification;

    private Payment payment;

    @Before
    public void setUp() throws Exception {
        payment = testHelper.getPaymentQueryResultFromFile("dummyPaymentQueryResult.json").head().get();

        notification = new Notification();
        notification.setPrice("20.00");
        notification.setCurrency("EUR");
        notification.setTxtime(millis.toString());
        notification.setSequencenumber("0");
        notification.setTxaction(NotificationAction.APPOINTED);
        notification.setTransactionStatus(TransactionStatus.PENDING);

    }

    @Test
    public void getUpdatesForNewTransaction() throws IOException {
        Payment payment = testHelper.getPaymentQueryResultFromFile("dummyPaymentQueryResult.json").head().get();
        payment.getTransactions().clear();

        AppointedNotificationProcessor processor = new AppointedNotificationProcessor(client);

        List<UpdateAction<Payment>> updateActions = processor.createPaymentUpdates(payment, notification);

        MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());

        AddTransaction transaction = AddTransaction.of(TransactionDraftBuilder
                .of(TransactionType.AUTHORIZATION, amount, ZonedDateTime.of(timestamp, ZoneId.of("UTC")))
                .state(TransactionState.PENDING)
                .interactionId(notification.getSequencenumber())
                .build());
        AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                ImmutableMap.of(
                        CustomTypeBuilder.TIMESTAMP_FIELD, ZonedDateTime.of(timestamp, ZoneId.of("UTC")),
                        CustomTypeBuilder.TRANSACTION_ID_FIELD, "",
                        CustomTypeBuilder.NOTIFICATION_FIELD, notification.toString()));

        assertThat(updateActions).isNotEmpty().hasSize(2);
        assertThat(updateActions).filteredOn(u -> u.getAction().equals("addTransaction"))
                .usingElementComparatorOnFields("transaction.type", "transaction.amount", "transaction.state", "transaction.timestamp")
                .containsOnly(transaction);
        assertThat(updateActions).filteredOn(u -> u.getAction().equals("addInterfaceInteraction"))
                .usingElementComparatorOnFields("fields")
                .containsOnly(interfaceInteraction);
    }

    @Test
    public void getUpdatesForExistingTransaction() throws IOException {
        AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                ImmutableMap.of(
                        CustomTypeBuilder.TIMESTAMP_FIELD, ZonedDateTime.of(timestamp, ZoneId.of("UTC")),
                        CustomTypeBuilder.TRANSACTION_ID_FIELD, payment.getTransactions().get(0).getId(),
                        CustomTypeBuilder.NOTIFICATION_FIELD, notification.toString()));

        AppointedNotificationProcessor processor = new AppointedNotificationProcessor(client);

        List<UpdateAction<Payment>> updateActions = processor.createPaymentUpdates(payment, notification);

        assertThat(updateActions).isNotEmpty().hasSize(3);
        assertThat(updateActions).filteredOn(u -> u.getAction().equals("changeTransactionState"))
            .containsOnly(ChangeTransactionState.of(TransactionState.PENDING, payment.getTransactions().get(0).getId()));
        assertThat(updateActions).filteredOn(u -> u.getAction().equals("changeTransactionInteractionId"))
            .containsOnly(ChangeTransactionInteractionId.of(notification.getSequencenumber(), payment.getTransactions().get(0).getId()));
        assertThat(updateActions).filteredOn(u -> u.getAction().equals("addInterfaceInteraction"))
                .usingElementComparatorOnFields("fields")
                .containsOnly(interfaceInteraction);
    }
}