package com.commercetools.pspadapter.payone.domain.ctp.paymentmethods;

import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionState;

/**
 * @author Jan Wolter
 *
 * "Executes" a transaction by setting the status to Failure.
 */
public class UnsupportedTransactionExecutor implements TransactionExecutor {
    private final BlockingClient client;

    /**
     * Initializes the executor.
     * @param client the commercetools platform client
     */
    public UnsupportedTransactionExecutor(final BlockingClient client) {
        this.client = client;
    }

    @Override
    public Payment executeTransaction(final Payment payment, final Transaction transaction) {
        final ChangeTransactionState changeTransactionState = ChangeTransactionState.of(
                TransactionState.FAILURE,
                transaction.getId());

        final AddInterfaceInteraction addInterfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(
                CustomTypeBuilder.PAYONE_UNSUPPORTED_TRANSACTION,
                ImmutableMap.<String, Object>of(
                        CustomTypeBuilder.TRANSACTION_ID_FIELD, transaction.getId(),
                        CustomTypeBuilder.MESSAGE_FIELD, "Transaction type not supported."));

        return client.complete(
                PaymentUpdateCommand.of(payment, ImmutableList.of(changeTransactionState, addInterfaceInteraction)));
    }

}
