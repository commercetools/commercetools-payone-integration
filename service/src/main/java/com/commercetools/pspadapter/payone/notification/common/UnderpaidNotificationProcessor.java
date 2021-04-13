package com.commercetools.pspadapter.payone.notification.common;

import com.commercetools.payments.TransactionStateResolver;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.notification.NotificationProcessorBase;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.pspadapter.tenant.TenantFactory;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.utils.MoneyImpl;

import javax.money.MonetaryAmount;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A NotificationProcessor for notifications with {@code txaction} "underpaid".
 *
 * @author mht@dotsource.de
 */
public class UnderpaidNotificationProcessor extends NotificationProcessorBase {

    public UnderpaidNotificationProcessor(TenantFactory tenantFactory, TenantConfig tenantConfig,
                                          TransactionStateResolver transactionStateResolver) {
        super(tenantFactory, tenantConfig, transactionStateResolver);
    }

    @Override
    protected boolean canProcess(final Notification notification) {
        return NotificationAction.UNDERPAID.equals(notification.getTxaction());
    }

    @Override
    protected List<UpdateAction<Payment>> createPaymentUpdates(final Payment payment,
                                                               final Notification notification) {
        final List<UpdateAction<Payment>> updateActions = new ArrayList<>();
        updateActions.addAll(super.createPaymentUpdates(payment, notification));

        final List<Transaction> transactions = payment.getTransactions();
        final String sequenceNumber = toSequenceNumber(notification.getSequencenumber());

        final TransactionState ctTransactionState = notification.getTransactionStatus().getCtTransactionState();

        // if there is no matching transaction - add a new one
        Optional<Transaction> matchingTransaction = findMatchingTransaction(transactions, TransactionType.CHARGE, sequenceNumber);
        if (!matchingTransaction.isPresent()) {
            final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());

            updateActions.add(AddTransaction.of(TransactionDraftBuilder.of(TransactionType.CHARGE, amount)
                    .timestamp(toZonedDateTime(notification))
                    .state(ctTransactionState)
                    .interactionId(sequenceNumber)
                    .build()));
        }

        return updateActions;
    }

}
