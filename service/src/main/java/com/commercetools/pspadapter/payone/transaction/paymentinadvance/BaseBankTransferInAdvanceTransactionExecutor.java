package com.commercetools.pspadapter.payone.transaction.paymentinadvance;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseStatus;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.commercetools.pspadapter.payone.transaction.TransactionBaseExecutor;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionInteractionId;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionState;
import io.sphere.sdk.payments.commands.updateactions.SetCustomField;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields.*;
import static java.lang.String.format;

/**
 * Responsible to create the PayOne Request (PreAuthorization) and check if answer is approved or Error
 *
 * @author mht@dotsource.de
 */
abstract class BaseBankTransferInAdvanceTransactionExecutor extends TransactionBaseExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseBankTransferInAdvanceTransactionExecutor.class);
    protected final TransactionType transactionType;
    protected final PayoneRequestFactory requestFactory;
    protected final PayonePostService payonePostService;

    protected BaseBankTransferInAdvanceTransactionExecutor(@Nonnull final TransactionType transactionType,
                                                           @Nonnull final LoadingCache<String, Type> typeCache,
                                                           @Nonnull final PayoneRequestFactory requestFactory,
                                                           @Nonnull final PayonePostService payonePostService,
                                                           @Nonnull final BlockingSphereClient client) {
        super(typeCache, client);
        this.transactionType = transactionType;
        this.requestFactory = requestFactory;
        this.payonePostService = payonePostService;
    }

    @Nonnull
    @Override
    public TransactionType supportedTransactionType() {
        return transactionType;
    }

    /**
     * Create respective authorization or preauthorization request for
     * {@link #attemptExecution(PaymentWithCartLike, Transaction)} call.
     *
     * @param paymentWithCartLike payment/cart which supply in the request
     * @return
     */
    @Nonnull
    protected abstract AuthorizationRequest createRequest(@Nonnull PaymentWithCartLike paymentWithCartLike);

    @Override
    protected boolean wasExecuted(PaymentWithCartLike paymentWithCartLike, Transaction transaction) {
        if (getCustomFieldsOfType(paymentWithCartLike,
                CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE)
                .noneMatch(fields -> transaction.getId().equals(fields.getFieldAsString(CustomFieldKeys.TRANSACTION_ID_FIELD)))) {

            return getCustomFieldsOfType(paymentWithCartLike, CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION)
                    //sequenceNumber field is mandatory -> can't be null
                    .anyMatch(fields -> fields.getFieldAsString(CustomFieldKeys.SEQUENCE_NUMBER_FIELD).equals(transaction.getInteractionId()));
        } else {
            return true;
        }
    }

    @Override
    public Optional<CustomFields> findLastExecutionAttempt(PaymentWithCartLike paymentWithCartLike, Transaction transaction) {
        return getCustomFieldsOfType(paymentWithCartLike, CustomTypeBuilder.PAYONE_INTERACTION_REQUEST)
                .filter(i -> i.getFieldAsString(CustomFieldKeys.TRANSACTION_ID_FIELD).equals(transaction.getId()))
                .reduce((previous, current) -> current);
    }

    @Override
    @Nonnull
    public PaymentWithCartLike retryLastExecutionAttempt(@Nonnull PaymentWithCartLike paymentWithCartLike,
                                                         @Nonnull Transaction transaction,
                                                         @Nonnull CustomFields lastExecutionAttempt) {
        if (lastExecutionAttempt.getFieldAsDateTime(CustomFieldKeys.TIMESTAMP_FIELD).isBefore(ZonedDateTime.now().minusMinutes(5))) {
            return attemptExecution(paymentWithCartLike, transaction);
        } else {
            if (lastExecutionAttempt.getFieldAsDateTime(CustomFieldKeys.TIMESTAMP_FIELD).isAfter(ZonedDateTime.now().minusMinutes(1)))
                throw new ConcurrentModificationException(String.format(
                        "A processing of payment with ID \"%s\" started during the last 60 seconds and is likely to be finished soon, no need to retry now.",
                        paymentWithCartLike.getPayment().getId()));
        }
        return paymentWithCartLike;
    }

    @Override
    @Nonnull
    protected PaymentWithCartLike attemptExecution(final PaymentWithCartLike paymentWithCartLike,
                                                   final Transaction transaction) {
        final String transactionId = transaction.getId();
        final int sequenceNumber = getNextSequenceNumber(paymentWithCartLike);

        final AuthorizationRequest request = createRequest(paymentWithCartLike);

        final Payment updatedPayment = client.executeBlocking(
                PaymentUpdateCommand.of(paymentWithCartLike.getPayment(),
                        ImmutableList.of(
                                AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_REQUEST,
                                        ImmutableMap.of(CustomFieldKeys.REQUEST_FIELD, request.toStringMap(true).toString(),
                                                CustomFieldKeys.TRANSACTION_ID_FIELD, transactionId,
                                                CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.now())),
                                ChangeTransactionInteractionId.of(String.valueOf(sequenceNumber), transactionId))
                ));

        try {
            final Map<String, String> response = payonePostService.executePost(request);

            final String status = response.get(STATUS);

            final AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE,
                    ImmutableMap.of(CustomFieldKeys.RESPONSE_FIELD, responseToJsonString(response),
                            CustomFieldKeys.TRANSACTION_ID_FIELD, transactionId,
                            CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.now()));

            if (ResponseStatus.APPROVED.getStateCode().equals(status)) {

                return update(paymentWithCartLike, updatedPayment, getBankTransferAdvancedUpdateActions(TransactionState.PENDING, updatedPayment, transactionId, response, interfaceInteraction));

            } else if (ResponseStatus.ERROR.getStateCode().equals(status)) {

                return update(paymentWithCartLike, updatedPayment, getDefaultUpdateActions(TransactionState.FAILURE, updatedPayment, transactionId, response, interfaceInteraction));

            } else if (ResponseStatus.PENDING.getStateCode().equals(status)) {

                return update(paymentWithCartLike, updatedPayment, getDefaultSuccessUpdateActions(TransactionState.PENDING, updatedPayment, transactionId, response, interfaceInteraction));

            }

            // TODO: https://github.com/commercetools/commercetools-payone-integration/issues/199
            throw new IllegalStateException("Unknown PayOne status: " + status);
        } catch (PayoneException paymentException) {
            LOGGER.error(format("Request to Payone failed for commercetools Payment with id '%s' and "
                    + "Transaction with id '%s'.", paymentWithCartLike.getPayment().getId(), transactionId),
                paymentException);


            final AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE,
                    ImmutableMap.of(CustomFieldKeys.RESPONSE_FIELD, exceptionToResponseJsonString(paymentException),
                            CustomFieldKeys.TRANSACTION_ID_FIELD, transactionId,
                            CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.now()));

            final ChangeTransactionState failureTransaction = ChangeTransactionState.of(TransactionState.FAILURE, transactionId);

            return update(paymentWithCartLike, updatedPayment, ImmutableList.of(interfaceInteraction, failureTransaction));
        }
    }

    /**
     * Additionally to {@link #getDefaultSuccessUpdateActions(TransactionState, Payment, String, Map, AddInterfaceInteraction)}
     * adds payer and IBAN/BIC custom fields.
     * <p>
     * This update actions list is used for all success payment handling (including redirect, approved and pending)
     */
    protected List<UpdateActionImpl<Payment>> getBankTransferAdvancedUpdateActions(@Nonnull TransactionState newState,
                                                                                   @Nonnull Payment updatedPayment,
                                                                                   @Nonnull String transactionId,
                                                                                   @Nonnull Map<String, String> response,
                                                                                   @Nonnull AddInterfaceInteraction interfaceInteraction) {

        List<UpdateActionImpl<Payment>> updateActions = getDefaultSuccessUpdateActions(newState, updatedPayment, transactionId, response, interfaceInteraction);

        updateActions.add(SetCustomField.ofObject(CustomFieldKeys.PAY_TO_BIC_FIELD, response.get(BIC)));
        updateActions.add(SetCustomField.ofObject(CustomFieldKeys.PAY_TO_IBAN_FIELD, response.get(IBAN)));
        updateActions.add(SetCustomField.ofObject(CustomFieldKeys.PAY_TO_NAME_FIELD, response.get(ACCOUNT_HOLDER)));

        return updateActions;
    }


}
