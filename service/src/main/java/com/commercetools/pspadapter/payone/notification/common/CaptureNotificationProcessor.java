package com.commercetools.pspadapter.payone.notification.common;

import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
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
import io.sphere.sdk.utils.MoneyImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * A NotificationProcessor for notifications with txaction "appointed".
 *
 * @author Jan Wolter
 */
public class CaptureNotificationProcessor extends NotificationProcessorBase {

    /**
     * Initializes a new instance.
     *
     * @param client the client for the commercetools platform API
     */
    public CaptureNotificationProcessor(final BlockingClient client) {
        super(client);
    }

    @Override
    protected ImmutableList<UpdateAction<Payment>> createPaymentUpdates(final Payment payment,
                                                                        final Notification notification) {
        final LocalDateTime timestamp =
                LocalDateTime.ofEpochSecond(Long.valueOf(notification.getTxtime()), 0, ZoneOffset.UTC);

        final String sequenceNumber = nullToZero(notification.getSequencenumber());
        final ImmutableList.Builder<UpdateAction<Payment>> listBuilder = ImmutableList.builder();
        listBuilder.add(AddInterfaceInteraction.ofTypeKeyAndObjects(
                CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                ImmutableMap.of(
                        CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.of(timestamp, ZoneId.of("UTC")),
                        CustomFieldKeys.SEQUENCE_NUMBER_FIELD, sequenceNumber,
                        CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                        CustomFieldKeys.NOTIFICATION_FIELD, notification.toString())));

        final List<Transaction> transactions = payment.getTransactions();

        return findMatchingChargeTransaction(transactions, sequenceNumber)
                .map(transaction -> listBuilder.build())
                .orElseGet(() -> {
                    final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());

                    listBuilder.add(AddTransaction.of(TransactionDraftBuilder.of(TransactionType.CHARGE, amount)
                            .timestamp(ZonedDateTime.of(timestamp, ZoneId.of("UTC")))
                            .state(TransactionState.PENDING)
                            .interactionId(sequenceNumber)
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
    private Optional<Transaction> findMatchingChargeTransaction(final Collection<Transaction> transactions,
                                                                final String interactionId) {
        return transactions.stream()
                .filter(transaction ->
                        transaction.getType().equals(TransactionType.CHARGE)
                                && nullToZero(transaction.getInteractionId()).equals(interactionId))
                .findFirst();
    }

    /**
     * Transforms the {@code string} into "0" if necessary.
     *
     * @param string the string
     * @return the {@code string} if not null, "0" otherwise
     */
    @Nonnull
    private static String nullToZero(@Nullable final String string) {
        return Optional.ofNullable(string).orElse("0");
    }

    public NotificationAction supportedNotificationAction() {
        return NotificationAction.CAPTURE;
    }
}
