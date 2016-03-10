package com.commercetools.pspadapter.payone.notification.common;

import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import com.commercetools.pspadapter.payone.notification.NotificationProcessorBase;
import com.google.common.collect.ImmutableList;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionInteractionId;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionState;
import io.sphere.sdk.payments.commands.updateactions.SetAuthorization;
import io.sphere.sdk.utils.MoneyImpl;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

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
    protected boolean canProcess(final Notification notification) {
        return NotificationAction.APPOINTED.equals(notification.getTxaction());
    }

    @Override
    protected ImmutableList<UpdateAction<Payment>> createPaymentUpdates(final Payment payment,
                                                                        final Notification notification) {
        final ImmutableList.Builder<UpdateAction<Payment>> listBuilder = ImmutableList.builder();

        listBuilder.add(createNotificationAddAction(notification));

        final List<Transaction> transactions = payment.getTransactions();
        final String sequenceNumber = toSequenceNumber(notification.getSequencenumber());

        if (findMatchingTransaction(transactions, TransactionType.CHARGE, sequenceNumber).isPresent()) {
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
                            notification.getTransactionStatus().equals(TransactionStatus.COMPLETED)) {
                        listBuilder.add(ChangeTransactionState.of(
                                notification.getTransactionStatus().getCtTransactionState(), t.getId()));

                        listBuilder.add(createSetAuthorizationAction(payment, notification));
                    }

                    return listBuilder.build();
                })
                .orElseGet(() -> {
                    addPaymentUpdatesForNewTransaction(payment, notification, listBuilder);
                    return listBuilder.build();
                });
    }

    private static void addPaymentUpdatesForNewTransaction(
            final Payment payment,
            final Notification notification,
            final ImmutableList.Builder<UpdateAction<Payment>> listBuilder) {
        final String currency = notification.getCurrency();
        final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), currency);
        final MonetaryAmount balance = MoneyImpl.of(notification.getBalance(), currency);

        final TransactionType transactionType;
        final TransactionState ctTransactionState;
        if (balance.isZero()) {
            transactionType = TransactionType.AUTHORIZATION;
            ctTransactionState = notification.getTransactionStatus().getCtTransactionState();
            if (ctTransactionState.equals(TransactionState.SUCCESS)) {
                final SetAuthorization setAuthorizationAction = createSetAuthorizationAction(payment, notification);
                listBuilder.add(setAuthorizationAction);
            }
        } else {
            transactionType = TransactionType.CHARGE;
            ctTransactionState = TransactionState.PENDING;
        }

        listBuilder.add(AddTransaction.of(TransactionDraftBuilder.of(transactionType, amount)
                .timestamp(toZonedDateTime(notification))
                .state(ctTransactionState)
                .interactionId(notification.getSequencenumber())
                .build()));
    }

    private static SetAuthorization createSetAuthorizationAction(final Payment payment,
                                                                 final Notification notification) {
        final ZonedDateTime authorizedUntil =
                ZonedDateTime.of(
                        LocalDateTime.ofEpochSecond(
                                Long.parseLong(notification.getTxtime()),
                                0,
                                ZoneOffset.UTC),
                        ZoneId.of("UTC"))
                .plusDays(7);

        return SetAuthorization.of(payment.getAmountPlanned(), authorizedUntil);
    }

}
