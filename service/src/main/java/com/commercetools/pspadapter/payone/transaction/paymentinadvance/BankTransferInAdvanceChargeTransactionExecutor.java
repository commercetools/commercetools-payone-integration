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
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.*;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Optional;

import static com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields.*;

/**
 * Responsible to create the PayOne Request (PreAuthorization) and check if answer is approved or Error
 * @author mht@dotsource.de
 *
 */
public class BankTransferInAdvanceChargeTransactionExecutor extends TransactionBaseExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BankTransferInAdvanceChargeTransactionExecutor.class);

    private final PayoneRequestFactory requestFactory;
    private final PayonePostService payonePostService;

    public BankTransferInAdvanceChargeTransactionExecutor(@Nonnull final LoadingCache<String, Type> typeCache,
                                                          @Nonnull final PayoneRequestFactory requestFactory,
                                                          @Nonnull final PayonePostService payonePostService,
                                                          @Nonnull final BlockingSphereClient client) {
        super(typeCache, client);
        this.requestFactory = requestFactory;
        this.payonePostService = payonePostService;
    }

    @Override
    public TransactionType supportedTransactionType() {
        return TransactionType.CHARGE;
    }

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
        }
        else {
            if (lastExecutionAttempt.getFieldAsDateTime(CustomFieldKeys.TIMESTAMP_FIELD).isAfter(ZonedDateTime.now().minusMinutes(1)))
                throw new ConcurrentModificationException( String.format(
                        "A processing of payment with ID \"%s\" started during the last 60 seconds and is likely to be finished soon, no need to retry now.",
                        paymentWithCartLike.getPayment().getId()));
        }
        return paymentWithCartLike;
    }

    protected PaymentWithCartLike attemptExecution(final PaymentWithCartLike paymentWithCartLike,
                                                   final Transaction transaction) {
        final String transactionId = transaction.getId();
        final int sequenceNumber = getNextSequenceNumber(paymentWithCartLike);

        final AuthorizationRequest request = requestFactory.createPreauthorizationRequest(paymentWithCartLike);

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
                return update(paymentWithCartLike, updatedPayment, ImmutableList.of(
                        interfaceInteraction,
                        setStatusInterfaceCode(response),
                        setStatusInterfaceText(response),
                        SetInterfaceId.of(response.get(TXID)),
                        ChangeTransactionState.of(TransactionState.SUCCESS, transactionId),
                        ChangeTransactionTimestamp.of(ZonedDateTime.now(), transactionId),
                        SetCustomField.ofObject(CustomFieldKeys.PAY_TO_BIC_FIELD, response.get(BIC)),
                        SetCustomField.ofObject(CustomFieldKeys.PAY_TO_IBAN_FIELD, response.get(IBAN)),
                        SetCustomField.ofObject(CustomFieldKeys.PAY_TO_NAME_FIELD, response.get(ACCOUNT_HOLDER))
                ));
            } else if (ResponseStatus.ERROR.getStateCode().equals(status)) {
                return update(paymentWithCartLike, updatedPayment, ImmutableList.of(
                        interfaceInteraction,
                        setStatusInterfaceCode(response),
                        setStatusInterfaceText(response),
                        ChangeTransactionState.of(TransactionState.FAILURE, transactionId),
                        ChangeTransactionTimestamp.of(ZonedDateTime.now(), transactionId)
                ));
            } else if (ResponseStatus.PENDING.getStateCode().equals(status)) {
                return update(paymentWithCartLike, updatedPayment, ImmutableList.of(
                        interfaceInteraction,
                        ChangeTransactionState.of(TransactionState.PENDING, transactionId),
                        setStatusInterfaceCode(response),
                        setStatusInterfaceText(response),
                        SetInterfaceId.of(response.get(TXID))));
            }

            // TODO: https://github.com/commercetools/commercetools-payone-integration/issues/199
            throw new IllegalStateException("Unknown PayOne status");
        }
        catch (PayoneException pe) {
            LOGGER.error("Payone request exception: ", pe);

            final AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE,
                    ImmutableMap.of(CustomFieldKeys.RESPONSE_FIELD, exceptionToResponseJsonString(pe),
                            CustomFieldKeys.TRANSACTION_ID_FIELD, transactionId,
                            CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.now()));

            final ChangeTransactionState failureTransaction = ChangeTransactionState.of(TransactionState.FAILURE, transactionId);

            return update(paymentWithCartLike, updatedPayment, ImmutableList.of(interfaceInteraction, failureTransaction));
        }
    }


}
