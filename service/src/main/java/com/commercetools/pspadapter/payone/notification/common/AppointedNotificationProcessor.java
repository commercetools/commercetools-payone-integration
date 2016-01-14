package com.commercetools.pspadapter.payone.notification.common;

import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.commercetools.pspadapter.payone.notification.NotificationProcessorBase;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * An implementation of a NotificationProcessor specifically for notifications with txaction 'appointed'.
 * Determines the necessary UpdateActions for the payment and applies them.
 *
 * @author fhaertig
 * @since 08.01.16
 */
public class AppointedNotificationProcessor extends NotificationProcessorBase {

    /**
     * Initializes a new instance.
     *
     * @param client the commercetools platform client to update payments
     */
    public AppointedNotificationProcessor(final BlockingClient client) {
        super(client);
    }

    @Override
    public NotificationAction supportedNotificationAction() {
        return NotificationAction.APPOINTED;
    }

    @Override
    protected ImmutableList<UpdateAction<Payment>> createPaymentUpdates(final Payment payment,
                                                                        final Notification notification) {
        final LocalDateTime timestamp =
                LocalDateTime.ofEpochSecond(Long.valueOf(notification.getTxtime()), 0, ZoneOffset.UTC);

        final List<Transaction> transactions = payment.getTransactions();
        final String sequenceNumber = Optional.ofNullable(notification.getSequencenumber()).orElse("0");

        final ImmutableList.Builder<UpdateAction<Payment>> listBuilder = ImmutableList.builder();
        listBuilder.add(AddInterfaceInteraction.ofTypeKeyAndObjects(
                CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                ImmutableMap.of(
                        CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.of(timestamp, ZoneId.of("UTC")),
                        CustomFieldKeys.SEQUENCE_NUMBER_FIELD, sequenceNumber,
                        CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                        CustomFieldKeys.NOTIFICATION_FIELD, notification.toString())));

        if (containsMatchingChargeTransaction(transactions, sequenceNumber)) {
            return listBuilder.build();
        }

        return transactions.stream()
                .filter(t -> t.getType().equals(TransactionType.AUTHORIZATION))
                .findFirst()
                .map(t -> {

                    //map sequenceNr to interactionId in existing transaction
                    listBuilder.add(ChangeTransactionInteractionId.of(sequenceNumber, t.getId()));

                    //set transactionState if still pending and notification has status "complete"
                    if (t.getState().equals(TransactionState.PENDING) &&
                            notification.getTransactionStatus().equals(TransactionStatus.COMPLETED)){
                        listBuilder.add(ChangeTransactionState
                                .of(notification.getTransactionStatus().getCtTransactionState(), t.getId()));
                    }

                    return listBuilder.build();
                })
                .orElseGet(() -> {
                    //create new transaction
                    final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());

                    listBuilder.add(AddTransaction.of(TransactionDraftBuilder.of(TransactionType.AUTHORIZATION, amount)
                            .timestamp(ZonedDateTime.of(timestamp, ZoneId.of("UTC")))
                            .state(notification.getTransactionStatus().getCtTransactionState())
                            .interactionId(notification.getSequencenumber())
                            .build()));

                    return listBuilder.build();
                });
    }

    /**
     * Checks whether {@code transactions} contains a {@link TransactionType#CHARGE} transaction with the
     * {@code interactionId}.
     *
     * @param transactions the list of transactions
     * @param interactionId the interaction ID (aka PAYONE's sequencenumber)
     * @return whether there is a {@code CHARGE} transaction with the provided {@code interactionId} in
     *         the {@code transactions}
     */
    private boolean containsMatchingChargeTransaction(final Collection<Transaction> transactions,
                                                      final String interactionId) {
        return transactions.stream().anyMatch(transaction ->
                transaction.getType().equals(TransactionType.CHARGE)
                        && Optional.ofNullable(transaction.getInteractionId()).orElse("0").equals(interactionId));
    }

}
