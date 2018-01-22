package com.commercetools.pspadapter.payone.transaction;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.commercetools.pspadapter.payone.domain.payone.model.common.BaseRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseStatus;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.*;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.*;


/**
 * Base class for validating/executing/re-trying preauthorization/authorization (authorization/charge) transactions.
 */
abstract public class BaseDefaultTransactionExecutor extends TransactionBaseExecutor {

    protected PayoneRequestFactory requestFactory;
    protected final PayonePostService payonePostService;

    public BaseDefaultTransactionExecutor(@Nonnull LoadingCache<String, Type> typeCache,
                                          @Nonnull final PayoneRequestFactory requestFactory,
                                          @Nonnull final PayonePostService payonePostService,
                                          @Nonnull BlockingSphereClient client) {
        super(typeCache, client);
        this.requestFactory = requestFactory;
        this.payonePostService = payonePostService;

    }

    /**
     * Create transaction with respective preauthorization/authorization (authorization/charge) type.
     *
     * @param paymentWithCartLike payment/cart which include to the request
     * @return {@link BaseRequest} implementation with respective payment and transaction type
     */
    @Nonnull
    abstract protected BaseRequest createRequest(PaymentWithCartLike paymentWithCartLike);

    /**
     * @return implementation specific logger
     */
    @Nonnull
    abstract protected Logger getClassLogger();

    @Override
    public boolean wasExecuted(PaymentWithCartLike paymentWithCartLike, Transaction transaction) {
        if (getCustomFieldsOfType(paymentWithCartLike,
                CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE,
                CustomTypeBuilder.PAYONE_INTERACTION_REDIRECT)
                .noneMatch(fields -> transaction.getId().equals(fields.getFieldAsString(CustomFieldKeys.TRANSACTION_ID_FIELD)))) {

            return getCustomFieldsOfType(paymentWithCartLike, CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION)
                    //sequenceNumber field is mandatory -> can't be null
                    .anyMatch(fields -> fields.getFieldAsString(CustomFieldKeys.SEQUENCE_NUMBER_FIELD).equals(transaction.getInteractionId()));
        } else {
            return true;
        }
    }

    @Override
    protected Optional<CustomFields> findLastExecutionAttempt(PaymentWithCartLike paymentWithCartLike, Transaction transaction) {
        return getCustomFieldsOfType(paymentWithCartLike, CustomTypeBuilder.PAYONE_INTERACTION_REQUEST)
                .filter(i -> transaction.getId().equals(i.getFieldAsString(CustomFieldKeys.TRANSACTION_ID_FIELD)))
                .reduce((previous, current) -> current); // .findLast()
    }

    @Override
    @Nonnull
    protected PaymentWithCartLike retryLastExecutionAttempt(@Nonnull PaymentWithCartLike paymentWithCartLike,
                                                            @Nonnull Transaction transaction,
                                                            @Nonnull CustomFields lastExecutionAttempt) {
        ZonedDateTime fieldAsDateTime = lastExecutionAttempt.getFieldAsDateTime(CustomFieldKeys.TIMESTAMP_FIELD);
        if (fieldAsDateTime == null || fieldAsDateTime.isBefore(ZonedDateTime.now().minusMinutes(5))) {
            return attemptExecution(paymentWithCartLike, transaction);
        } else {
            if (fieldAsDateTime.isAfter(ZonedDateTime.now().minusMinutes(1)))
                throw new ConcurrentModificationException(String.format(
                        "A processing of payment with ID \"%s\" started during the last 60 seconds and is likely to be finished soon, no need to retry now.",
                        paymentWithCartLike.getPayment().getId()));
        }
        return paymentWithCartLike;
    }

    protected PaymentWithCartLike attemptExecution(final PaymentWithCartLike paymentWithCartLike,
                                                   final Transaction transaction) {
        final String transactionId = transaction.getId();
        final String sequenceNumber = String.valueOf(getNextSequenceNumber(paymentWithCartLike));

        final BaseRequest request = createRequest(paymentWithCartLike);

        final Payment updatedPayment = client.executeBlocking(
                PaymentUpdateCommand.of(paymentWithCartLike.getPayment(),
                        ImmutableList.of(
                                AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_REQUEST,
                                        ImmutableMap.of(CustomFieldKeys.REQUEST_FIELD, request.toStringMap(true).toString() /* TODO */,
                                                CustomFieldKeys.TRANSACTION_ID_FIELD, transactionId,
                                                CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.now() /* TODO */)),
                                ChangeTransactionInteractionId.of(sequenceNumber, transactionId)
                        )
                ));

        try {
            final Map<String, String> response = payonePostService.executePost(request);

            final String status = response.get(PayoneResponseFields.STATUS);
            if (ResponseStatus.REDIRECT.getStateCode().equals(status)) {
                final AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_REDIRECT,
                        ImmutableMap.of(CustomFieldKeys.RESPONSE_FIELD, responseToJsonString(response),
                                CustomFieldKeys.REDIRECT_URL_FIELD, response.get(PayoneResponseFields.REDIRECT_URL),
                                CustomFieldKeys.TRANSACTION_ID_FIELD, transactionId,
                                CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.now() /* TODO */));

                return update(paymentWithCartLike, updatedPayment, getRedirectUpdateActionsList(TransactionState.PENDING, updatedPayment, transactionId, response, interfaceInteraction));

            } else {
                final AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE,
                        ImmutableMap.of(CustomFieldKeys.RESPONSE_FIELD, responseToJsonString(response),
                                CustomFieldKeys.TRANSACTION_ID_FIELD, transactionId,
                                CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.now() /* TODO */));

                if (ResponseStatus.APPROVED.getStateCode().equals(status)) {

                    return update(paymentWithCartLike, updatedPayment, getDefaultSuccessUpdateActionsList(TransactionState.SUCCESS, updatedPayment, transactionId, response, interfaceInteraction));

                } else if (ResponseStatus.ERROR.getStateCode().equals(status)) {

                    return update(paymentWithCartLike, updatedPayment, getDefaultUpdateActionsList(TransactionState.FAILURE, updatedPayment, transactionId, response, interfaceInteraction));

                } else if (ResponseStatus.PENDING.getStateCode().equals(status)) {

                    return update(paymentWithCartLike, updatedPayment, getDefaultSuccessUpdateActionsList(TransactionState.PENDING, updatedPayment, transactionId, response, interfaceInteraction));

                }
            }

            // TODO: https://github.com/commercetools/commercetools-payone-integration/issues/199
            throw new IllegalStateException("Unknown PayOne status: " + status);
        } catch (PayoneException pe) {
            getClassLogger().error("Payone request exception: ", pe);

            final AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE,
                    ImmutableMap.of(CustomFieldKeys.RESPONSE_FIELD, exceptionToResponseJsonString(pe),
                            CustomFieldKeys.TRANSACTION_ID_FIELD, transactionId,
                            CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.now() /* TODO */));

            final ChangeTransactionState failureTransaction = ChangeTransactionState.of(TransactionState.FAILURE, transactionId);

            return update(paymentWithCartLike, updatedPayment, ImmutableList.of(interfaceInteraction, failureTransaction));
        }
    }
}
