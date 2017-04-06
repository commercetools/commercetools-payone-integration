package com.commercetools.pspadapter.payone.notification.common;

import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
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

/**
 * A NotificationProcessor for notifications with txaction "capture".
 *
 * @author Jan Wolter
 */
public class CaptureNotificationProcessor extends NotificationProcessorBase {

    /**
     * Initializes a new instance.
     *
     * @param serviceFactory the services factory for commercetools platform API
     */
    public CaptureNotificationProcessor(TenantFactory serviceFactory, TenantConfig tenantConfig) {
        super(serviceFactory, tenantConfig);
    }

        @Override
    protected boolean canProcess(final Notification notification) {
        return NotificationAction.CAPTURE.equals(notification.getTxaction());
    }

    @Override
    protected ImmutableList<UpdateAction<Payment>> createPaymentUpdates(final Payment payment,
                                                                        final Notification notification) {
        final ImmutableList.Builder<UpdateAction<Payment>> listBuilder = ImmutableList.builder();
        listBuilder.addAll(super.createPaymentUpdates(payment, notification));

        final List<Transaction> transactions = payment.getTransactions();
        final String sequenceNumber = toSequenceNumber(notification.getSequencenumber());

        return findMatchingTransaction(transactions, TransactionType.CHARGE, sequenceNumber)
                .map(transaction -> listBuilder.build())
                .orElseGet(() -> {
                    final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());

                    //For all payment methods that use PayOne preauthorization and capture but only one CTP-charge
                    //we must not create a second charge transaction
                    //at the moment that is only "BANK_TRANSFER_ADVANCE"
                    if (ClearingType.PAYONE_VOR != ClearingType.getClearingTypeByCode(notification.getClearingtype())) {
                    listBuilder.add(AddTransaction.of(TransactionDraftBuilder.of(TransactionType.CHARGE, amount)
                            .timestamp(toZonedDateTime(notification))
                            .state(TransactionState.PENDING)
                            .interactionId(sequenceNumber)
                            .build()));
                    }

                    return listBuilder.build();
                });
    }
}
