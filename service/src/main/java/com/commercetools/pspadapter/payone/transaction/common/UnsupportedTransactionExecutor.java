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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

import static java.lang.String.format;

/**
 * @author Jan Wolter
 *
 * "Executes" a transaction by setting the status to Failure.
 */
public class UnsupportedTransactionExecutor implements TransactionExecutor {
    private final BlockingSphereClient client;

    public static final Logger LOG = LoggerFactory.getLogger(UnsupportedTransactionExecutor.class);

    /**
     * Initializes the executor.
     * @param client the commercetools platform client
     */
    public UnsupportedTransactionExecutor(final BlockingSphereClient client) {
        this.client = client;
    }

    @Override
    public PaymentWithCartLike executeTransaction(final PaymentWithCartLike paymentWithCartLike, final Transaction transaction) {
        final Payment payment = paymentWithCartLike.getPayment();

        LOG.warn("Trying to execute an unsupported transaction type [{}] for method [{}] on payment [{}]",
                transaction.getType().toString(), payment.getPaymentMethodInfo().getMethod(), payment.getId());

        final ChangeTransactionState changeTransactionState = ChangeTransactionState.of(
                TransactionState.FAILURE,
                transaction.getId());

        final AddInterfaceInteraction addInterfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(
                CustomTypeBuilder.PAYONE_UNSUPPORTED_TRANSACTION,
                ImmutableMap.of(
                        CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.now(),
                        CustomFieldKeys.TRANSACTION_ID_FIELD, transaction.getId(),
                        CustomFieldKeys.MESSAGE_FIELD, format("Transaction type [%s] not supported for method [%s].",
                                payment.getPaymentMethodInfo().getMethod(), transaction.getType())));

        return paymentWithCartLike.withPayment(
            client.executeBlocking(
                PaymentUpdateCommand.of(payment, ImmutableList.of(changeTransactionState, addInterfaceInteraction))));
    }

}
