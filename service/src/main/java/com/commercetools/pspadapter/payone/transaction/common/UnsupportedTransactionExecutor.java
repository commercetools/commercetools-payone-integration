package com.commercetools.pspadapter.payone.transaction.common;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.commercetools.pspadapter.payone.transaction.TransactionExecutor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionState;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;

/**
 * @author Jan Wolter
 * <p>
 * "Executes" a transaction by setting the status to Failure.
 */
public class UnsupportedTransactionExecutor implements TransactionExecutor {
    private final BlockingSphereClient client;

    /**
     * Initializes the executor.
     *
     * @param client the commercetools platform client
     */
    public UnsupportedTransactionExecutor(final BlockingSphereClient client) {
        this.client = client;
    }

    @Override
    @Nonnull
    public PaymentWithCartLike executeTransaction(@Nonnull final PaymentWithCartLike paymentWithCartLike,
                                                  @Nonnull final Transaction transaction) {
        final Payment payment = paymentWithCartLike.getPayment();
        final ChangeTransactionState changeTransactionState = ChangeTransactionState.of(
                TransactionState.FAILURE,
                transaction.getId());

        final AddInterfaceInteraction addInterfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(
                CustomTypeBuilder.PAYONE_UNSUPPORTED_TRANSACTION,
                ImmutableMap.<String, Object>of(
                        CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.now(),
                        CustomFieldKeys.TRANSACTION_ID_FIELD, transaction.getId(),
                        CustomFieldKeys.MESSAGE_FIELD, "Transaction type not supported."));

        return paymentWithCartLike.withPayment(
            client.executeBlocking(
                PaymentUpdateCommand.of(payment, ImmutableList.of(changeTransactionState, addInterfaceInteraction))));
    }

}
