package com.commercetools.pspadapter.payone.notification.common;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import com.commercetools.pspadapter.payone.notification.NotificationProcessorBase;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.pspadapter.tenant.TenantFactory;
import com.google.common.collect.ImmutableList;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionInteractionId;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionState;
import io.sphere.sdk.utils.MoneyImpl;

import javax.money.MonetaryAmount;
import java.util.List;
import java.util.Optional;

/**
 * An implementation of a NotificationProcessor specifically for notifications with txaction 'appointed'.
 * Determines the necessary UpdateActions for the payment and applies them.
 *
 * @author fhaertig
 * @author Jan Wolter
 * @since 08.01.16
 */
public class AppointedNotificationProcessor extends NotificationProcessorBase {

    /**
     * Initializes a new instance.
     *
     * @param serviceFactory the services factory for commercetools platform API
     */
    public AppointedNotificationProcessor(TenantFactory serviceFactory, TenantConfig tenantConfig) {
        super(serviceFactory, tenantConfig);
    }

    @Override
    protected boolean canProcess(final Notification notification) {
        return NotificationAction.APPOINTED.equals(notification.getTxaction());
    }

    @Override
    protected ImmutableList<UpdateAction<Payment>> createPaymentUpdates(final Payment payment,
                                                                        final Notification notification) {
        final ImmutableList.Builder<UpdateAction<Payment>> listBuilder = ImmutableList.builder();
        listBuilder.addAll(super.createPaymentUpdates(payment, notification));

        final List<Transaction> transactions = payment.getTransactions();
        final String sequenceNumber = toSequenceNumber(notification.getSequencenumber());

        if (findMatchingTransaction(transactions, TransactionType.CHARGE, sequenceNumber).isPresent()) {
            return listBuilder.build();
        }

        if (sequenceNumber.equals("1")) {
            final Optional<Transaction> transaction =
                    findMatchingTransaction(transactions, TransactionType.CHARGE, "0");
            if (transaction.isPresent()) {
                listBuilder.add(ChangeTransactionInteractionId.of(sequenceNumber, transaction.get().getId()));
            } else {
                addPaymentUpdatesForNewChargeTransaction(notification, listBuilder);
            }
            return listBuilder.build();
        }

        final MonetaryAmount balance = MoneyImpl.of(notification.getBalance(), notification.getCurrency());
        if (balance.isZero()) {
            return transactions.stream()
                    .filter(t -> t.getType().equals(TransactionType.AUTHORIZATION))
                    .findFirst()
                    .map(transaction -> {
                        //set transactionState if still pending and notification has status "complete"
                        if (transaction.getState().equals(TransactionState.PENDING) &&
                                notification.getTransactionStatus().equals(TransactionStatus.COMPLETED)) {
                            listBuilder.add(ChangeTransactionState.of(
                                    notification.getTransactionStatus().getCtTransactionState(), transaction.getId()));
                        }

                        return listBuilder;
                    })
                    .orElseGet(() -> {
                        addPaymentUpdatesForNewAuthorizationTransaction(notification, listBuilder);
                        return listBuilder;
                    })
                    .build();
        }

        addPaymentUpdatesForNewChargeTransaction(notification, listBuilder);
        return listBuilder.build();
    }

    private static void addPaymentUpdatesForNewAuthorizationTransaction(
            final Notification notification,
            final ImmutableList.Builder<UpdateAction<Payment>> listBuilder) {
        final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());

        final TransactionState ctTransactionState = notification.getTransactionStatus().getCtTransactionState();

        listBuilder.add(AddTransaction.of(TransactionDraftBuilder.of(TransactionType.AUTHORIZATION, amount)
                .timestamp(toZonedDateTime(notification))
                .state(ctTransactionState)
                .interactionId(notification.getSequencenumber())
                .build()));
    }

    private static void addPaymentUpdatesForNewChargeTransaction(
            final Notification notification,
            final ImmutableList.Builder<UpdateAction<Payment>> listBuilder) {
        final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());

        listBuilder.add(AddTransaction.of(TransactionDraftBuilder.of(TransactionType.CHARGE, amount)
                .timestamp(toZonedDateTime(notification))
                .state(TransactionState.PENDING)
                .interactionId(notification.getSequencenumber())
                .build()));
    }
}
