package com.commercetools.pspadapter.payone.notification.common;

import com.commercetools.payments.TransactionStateResolver;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.notification.NotificationProcessorBase;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.pspadapter.tenant.TenantFactory;
import com.google.common.collect.ImmutableList;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.utils.MoneyImpl;

import javax.money.MonetaryAmount;
import java.util.List;
import java.util.Optional;

/**
 * A NotificationProcessor for notifications with {@code txaction} "underpaid".
 *
 * @author mht@dotsource.de
 */
public class UnderpaidNotificationProcessor extends NotificationProcessorBase {
    /**
     * Initializes a new instance.
     *
     * @param serviceFactory the services factory for commercetools platform API
     */
    public UnderpaidNotificationProcessor(TenantFactory serviceFactory, TenantConfig tenantConfig,
                                          TransactionStateResolver transactionStateResolver) {
        super(serviceFactory, tenantConfig, transactionStateResolver);
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

        // if there is no matching transaction - add a new one
        Optional<Transaction> matchingTransaction = findMatchingTransaction(transactions, TransactionType.CHARGE, sequenceNumber);
        if (!matchingTransaction.isPresent()) {
            final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());

            listBuilder.add(AddTransaction.of(TransactionDraftBuilder.of(TransactionType.CHARGE, amount)
                    .timestamp(toZonedDateTime(notification))
                    .state(ctTransactionState)
                    .interactionId(sequenceNumber)
                    .build()));
        }

        return listBuilder.build();
    }

}
