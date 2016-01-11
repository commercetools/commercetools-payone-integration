package com.commercetools.pspadapter.payone.notification;

import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.TransactionDraft;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionInteractionId;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionState;
import io.sphere.sdk.utils.MoneyImpl;

import javax.money.MonetaryAmount;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * @author fhaertig
 * @date 08.01.16
 */
public class AppointedNotificationProcessor implements NotificationProcessor {

    private final BlockingClient client;

    public AppointedNotificationProcessor(
            final BlockingClient client) {
        this.client = client;
    }

    @Override
    public NotificationAction supportedNotificationAction() {
        return NotificationAction.APPOINTED;
    }

    @Override
    public boolean processTransactionStatusNotification(final Notification notification, final Payment payment) {
        if (!notification.getTxaction().equals(supportedNotificationAction())) {
            throw new IllegalArgumentException(String.format("txaction %s is not supported for %s",
                                                                notification.getTxaction(),
                                                                this.getClass().getSimpleName()));
        }

        List<UpdateAction<Payment>> updateActions = getTransactionUpdates(payment, notification);

        final AddInterfaceInteraction newInterfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                ImmutableMap.of(CustomTypeBuilder.TIMESTAMP_FIELD, notification.getTxtime(),
                    CustomTypeBuilder.TRANSACTION_ID_FIELD, notification.getTxid(),
                    CustomTypeBuilder.NOTIFICATION_FIELD, notification.toString()));
        updateActions.add(newInterfaceInteraction);

        client.complete(PaymentUpdateCommand.of(payment, updateActions));

        return true;
    }

    @Override
    public List<UpdateAction<Payment>> getTransactionUpdates(final Payment payment, final Notification notification) {
        return payment.getTransactions().stream()
                .filter(t -> t.getType().equals(TransactionType.AUTHORIZATION))
                .findFirst()
                .map(t -> {
                    //map sequenceNr to interactionId in existing transaction
                    UpdateAction<Payment> changeInteractionId = ChangeTransactionInteractionId.of(notification.getSequencenumber(), t.getId());
                    //set transactionState according to notification
                    UpdateAction<Payment> changeTransactionState = ChangeTransactionState.of(notification.getTransactionStatus().getCtTransactionState(), t.getId());

                    return Arrays.asList(
                            changeInteractionId,
                            changeTransactionState
                    );
                })
                .orElseGet(() -> {
                    //create new transaction
                    MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());
                    TransactionDraft transactionDraft = TransactionDraftBuilder.of(
                            TransactionType.AUTHORIZATION, amount)
                            .timestamp(ZonedDateTime.now())
                            .state(notification.getTransactionStatus().getCtTransactionState())
                            .build();
                    return Arrays.asList(AddTransaction.of(transactionDraft));
                });
    }

}
