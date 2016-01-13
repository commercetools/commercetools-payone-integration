package com.commercetools.pspadapter.payone.notification;

import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.TransactionDraft;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionInteractionId;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionState;
import io.sphere.sdk.utils.MoneyImpl;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * An implementation of a NotificationProcessor specifically for notifications with txaction 'appointed'.
 * Determines the necessary UpdateActions for the payment and applies them.
 *
 * @author fhaertig
 * @since 08.01.16
 */
public class AppointedNotificationProcessor implements NotificationProcessor {

    private final BlockingClient client;
    private static final TransactionType authorizationTransactionType = TransactionType.AUTHORIZATION;

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

        client.complete(PaymentUpdateCommand.of(payment, createPaymentUpdates(payment, notification)));

        return true;
    }

    /**
     * Generates a list of update actions which can be applied to the payment in one step.
     *
     * @param payment the payment to which the updates may apply
     * @param notification the notification to process
     * @return an immutable list of update actions which will e.g. add an interfaceInteraction to the payment
     * or apply changes to a corresponding transaction in the payment
     */
    private ImmutableList<UpdateAction<Payment>> createPaymentUpdates(final Payment payment, final Notification notification) {
        LocalDateTime timestamp = LocalDateTime.ofEpochSecond(Long.valueOf(notification.getTxtime()), 0, ZoneOffset.UTC);

        return payment.getTransactions().stream()
                .filter(t -> t.getType().equals(authorizationTransactionType))
                .findFirst()
                .map(t -> {
                    ImmutableList.Builder<UpdateAction<Payment>> listBuilder = new ImmutableList.Builder<>();

                    //map sequenceNr to interactionId in existing transaction
                    listBuilder.add(ChangeTransactionInteractionId.of(notification.getSequencenumber(), t.getId()));

                    //set transactionState if still pending and notification has status "complete"
                    if (t.getState().equals(TransactionState.PENDING) &&
                            notification.getTransactionStatus().equals(TransactionStatus.COMPLETED)){
                        listBuilder.add(ChangeTransactionState
                                .of(notification.getTransactionStatus().getCtTransactionState(), t.getId()));
                    }


                    //add new interface interaction
                    listBuilder.add(AddInterfaceInteraction
                            .ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                                    ImmutableMap.of(
                                            CustomTypeBuilder.TIMESTAMP_FIELD, ZonedDateTime.of(timestamp, ZoneId.of("UTC")),
                                            CustomTypeBuilder.TRANSACTION_ID_FIELD, t.getId(),
                                            CustomTypeBuilder.NOTIFICATION_FIELD, notification.toString())));

                    return listBuilder.build();
                })
                .orElseGet(() -> {
                    //create new transaction
                    MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());

                    TransactionDraft transactionDraft = TransactionDraftBuilder.of(authorizationTransactionType, amount)
                            .timestamp(ZonedDateTime.of(timestamp, ZoneId.of("UTC")))
                            .state(notification.getTransactionStatus().getCtTransactionState())
                            .interactionId(notification.getSequencenumber())
                            .build();

                    //add new interface interaction
                    final AddInterfaceInteraction newInterfaceInteraction = AddInterfaceInteraction
                            .ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                                    ImmutableMap.of(
                                            CustomTypeBuilder.TIMESTAMP_FIELD, ZonedDateTime.of(timestamp, ZoneId.of("UTC")),
                                            // TODO: its impossible to get id of the new transaction, we need to change the customType to reflect this
                                            CustomTypeBuilder.TRANSACTION_ID_FIELD, "",
                                            CustomTypeBuilder.NOTIFICATION_FIELD, notification.toString()));

                    return ImmutableList.of(
                            AddTransaction.of(transactionDraft),
                            newInterfaceInteraction
                    );
                });
    }

}
