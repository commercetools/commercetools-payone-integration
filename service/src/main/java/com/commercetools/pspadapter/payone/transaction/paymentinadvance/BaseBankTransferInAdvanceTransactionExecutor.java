package com.commercetools.pspadapter.payone.transaction.paymentinadvance;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseStatus;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.commercetools.pspadapter.payone.transaction.TransactionBaseExecutor;
import com.github.benmanes.caffeine.cache.LoadingCache;
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
import io.sphere.sdk.types.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields.ACCOUNT_HOLDER;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields.BIC;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields.IBAN;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields.STATUS;
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
     * {@link #execute(PaymentWithCartLike, Transaction)} call.
     *
     * @param paymentWithCartLike payment/cart which supply in the request
     * @return
     */
    @Nonnull
    protected abstract PayoneRequest createRequest(@Nonnull PaymentWithCartLike paymentWithCartLike);

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
    @Nonnull
    protected PaymentWithCartLike execute(final PaymentWithCartLike paymentWithCartLike,
                                          final Transaction transaction) {
        final String transactionId = transaction.getId();
        final int sequenceNumber = getNextSequenceNumber(paymentWithCartLike);

        final PayoneRequest request = createRequest(paymentWithCartLike);

        final Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put(CustomFieldKeys.REQUEST_FIELD, request.toStringMap(true).toString());
        requestInfo.put(CustomFieldKeys.TRANSACTION_ID_FIELD, transactionId);
        requestInfo.put(CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.now());

        final AddInterfaceInteraction interfaceInteraction1 =
            AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_REQUEST,
                requestInfo);

        final Payment updatedPayment = client.executeBlocking(
            PaymentUpdateCommand.of(paymentWithCartLike.getPayment(),
                Arrays.asList(interfaceInteraction1,
                    ChangeTransactionInteractionId.of(String.valueOf(sequenceNumber), transactionId))
            ));

        final Map<String, Object> responseInfo = new HashMap<>();
        try {
            final Map<String, String> response = payonePostService.executePost(request);

            final String status = response.get(STATUS);

            responseInfo.put(CustomFieldKeys.RESPONSE_FIELD, responseToJsonString(response));
            responseInfo.put(CustomFieldKeys.TRANSACTION_ID_FIELD, transactionId);
            responseInfo.put(CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.now());

            final AddInterfaceInteraction interfaceInteraction2 = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE,
                    responseInfo);

            if (ResponseStatus.APPROVED.getStateCode().equals(status)) {

                return update(paymentWithCartLike, updatedPayment, getBankTransferAdvancedUpdateActions(TransactionState.PENDING, updatedPayment, transactionId, response, interfaceInteraction2));

            } else if (ResponseStatus.ERROR.getStateCode().equals(status)) {

                return update(paymentWithCartLike, updatedPayment, getDefaultUpdateActions(TransactionState.FAILURE, updatedPayment, transactionId, response, interfaceInteraction2));

            } else if (ResponseStatus.PENDING.getStateCode().equals(status)) {

                return update(paymentWithCartLike, updatedPayment, getDefaultSuccessUpdateActions(TransactionState.PENDING, updatedPayment, transactionId, response, interfaceInteraction2));

            }

            // TODO: https://github.com/commercetools/commercetools-payone-integration/issues/199
            throw new IllegalStateException("Unknown Payone status: " + status);
        } catch (PayoneException paymentException) {
            LOGGER.error(format("Request to Payone failed for commercetools Payment with id '%s' and "
                    + "Transaction with id '%s'.", paymentWithCartLike.getPayment().getId(), transactionId),
                paymentException);

            responseInfo.clear();
            responseInfo.put(CustomFieldKeys.RESPONSE_FIELD, exceptionToResponseJsonString(paymentException));
            responseInfo.put(CustomFieldKeys.TRANSACTION_ID_FIELD, transactionId);
            responseInfo.put(CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.now());

            final AddInterfaceInteraction interfaceInteraction =
                AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE,
                    responseInfo);

            final ChangeTransactionState failureTransaction =
                ChangeTransactionState.of(TransactionState.FAILURE, transactionId);

            return update(paymentWithCartLike, updatedPayment, Arrays.asList(interfaceInteraction, failureTransaction));
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
