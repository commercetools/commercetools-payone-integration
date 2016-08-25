package com.commercetools.pspadapter.payone.notification.common;

import java.util.List;

import javax.money.MonetaryAmount;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.notification.NotificationProcessorBase;
import com.google.common.collect.ImmutableList;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.SetAmountPaid;
import io.sphere.sdk.utils.MoneyImpl;

/**
 * A NotificationProcessor for notifications with {@code txaction} "underpaid".
 *
 * @author mht@dotsource.de
 */
public class UnderpaidNotificationProcessor extends NotificationProcessorBase {
    /**
     * Initializes a new instance.
     *
     * @param client the client for the commercetools platform API
     */
    public UnderpaidNotificationProcessor(final BlockingSphereClient client) {
        super(client);
    }

    @Override
    protected boolean canProcess(final Notification notification) {
        return NotificationAction.UNDERPAID.equals(notification.getTxaction());
    }

    @Override
    protected ImmutableList<UpdateAction<Payment>> createPaymentUpdates(final Payment payment,
                                                                        final Notification notification) {
        final ImmutableList.Builder<UpdateAction<Payment>> listBuilder = ImmutableList.builder();
        listBuilder.addAll(super.createPaymentUpdates(payment, notification));

        final List<Transaction> transactions = payment.getTransactions();
        final String sequenceNumber = toSequenceNumber(notification.getSequencenumber());

        final TransactionState ctTransactionState = notification.getTransactionStatus().getCtTransactionState();

        return findMatchingTransaction(transactions, TransactionType.CHARGE, sequenceNumber)
                .map(transaction -> {
                    addSetAmountPaidForChangedAmount(payment, notification, listBuilder);
                    return listBuilder.build();
                })
                .orElseGet(() -> {
                    final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());

                    listBuilder.add(AddTransaction.of(TransactionDraftBuilder.of(TransactionType.CHARGE, amount)
                            .timestamp(toZonedDateTime(notification))
                            .state(ctTransactionState)
                            .interactionId(sequenceNumber)
                            .build()));

                    if (ctTransactionState.equals(TransactionState.SUCCESS)) {
                        addSetAmountPaidForChangedAmount(payment, notification, listBuilder);
                    }

                    return listBuilder.build();
                });
    }

    /**
     * Adds a {@link SetAmountPaid} action to the list builder if the result of
     * <pre>
     *     amountPaid = notification.receivable - notification.balance
     * </pre>
     * differs from {@code payment.getAmountPaid()}.
     *
     * @param payment the payment
     * @param notification the "underpaid" notification
     * @param actions the builder for the list of payment update actions
     */
    private static void addSetAmountPaidForChangedAmount(final Payment payment,
                                                         final Notification notification,
                                                         final ImmutableList.Builder<UpdateAction<Payment>> actions) {
        final MonetaryAmount receivable = MoneyImpl.of(notification.getReceivable(), notification.getCurrency());
        final MonetaryAmount balance = MoneyImpl.of(notification.getBalance(), notification.getCurrency());
        final MonetaryAmount amountPaid = receivable.subtract(balance);
        if ((payment.getAmountPaid() == null) || !amountPaid.isEqualTo(payment.getAmountPaid())) {
            actions.add(SetAmountPaid.of(amountPaid));
        }
    }
}
