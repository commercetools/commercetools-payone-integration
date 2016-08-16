package com.commercetools.pspadapter.payone.transaction.common;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.commercetools.pspadapter.payone.transaction.IdempotentTransactionExecutor;
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
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionTimestamp;
import io.sphere.sdk.payments.commands.updateactions.SetAuthorization;
import io.sphere.sdk.payments.commands.updateactions.SetCustomField;
import io.sphere.sdk.payments.commands.updateactions.SetInterfaceId;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;

import java.time.ZonedDateTime;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Optional;

/**
 * Default Implementation for Charge Transaction Executuion. Calls PayOne Authorization Endpoint
 * @author mht@dotsource.de
 *
 */
public class DefaultChargeTransactionExecutor extends IdempotentTransactionExecutor {
    private final PayoneRequestFactory requestFactory;
    private final PayonePostService payonePostService;
    private final BlockingSphereClient client;

    public DefaultChargeTransactionExecutor(LoadingCache<String, Type> typeCache, PayoneRequestFactory requestFactory, PayonePostService payonePostService, BlockingSphereClient client) {
        super(typeCache);
        this.requestFactory = requestFactory;
        this.payonePostService = payonePostService;
        this.client = client;
    }

    @Override
    public TransactionType supportedTransactionType() {
        return TransactionType.CHARGE;
    }

    @Override
    protected boolean wasExecuted(PaymentWithCartLike paymentWithCartLike, Transaction transaction) {
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
    public PaymentWithCartLike attemptFirstExecution(PaymentWithCartLike paymentWithCartLike, Transaction transaction) {
        return attemptExecution(paymentWithCartLike, transaction);
    }

    @Override
    public Optional<CustomFields> findLastExecutionAttempt(PaymentWithCartLike paymentWithCartLike, Transaction transaction) {
        return getCustomFieldsOfType(paymentWithCartLike, CustomTypeBuilder.PAYONE_INTERACTION_REQUEST)
            .filter(i -> i.getFieldAsString(CustomFieldKeys.TRANSACTION_ID_FIELD).equals(transaction.getId()))
            .reduce((previous, current) -> current); // .findLast()
    }

    @Override
    public PaymentWithCartLike retryLastExecutionAttempt(PaymentWithCartLike paymentWithCartLike, Transaction transaction, CustomFields lastExecutionAttempt) {
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

    private PaymentWithCartLike attemptExecution(PaymentWithCartLike paymentWithCartLike, Transaction transaction) {

        final int sequenceNumber = getNextSequenceNumber(paymentWithCartLike);

        final AuthorizationRequest request = requestFactory.createAuthorizationRequest(paymentWithCartLike);

        final Payment updatedPayment = client.executeBlocking(
            PaymentUpdateCommand.of(paymentWithCartLike.getPayment(),
                ImmutableList.of(
                        AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_REQUEST,
                            ImmutableMap.of(CustomFieldKeys.REQUEST_FIELD, request.toStringMap(true).toString() /* TODO */,
                                    CustomFieldKeys.TRANSACTION_ID_FIELD, transaction.getId(),
                                    CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.now() /* TODO */)),
                        ChangeTransactionInteractionId.of(String.valueOf(sequenceNumber), transaction.getId()))
            ));

        try {
            final Map<String, String> response = payonePostService.executePost(request);

            final String status = response.get("status");
            if (status.equals("REDIRECT")) {
                final AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_REDIRECT,
                    ImmutableMap.of(CustomFieldKeys.RESPONSE_FIELD, response.toString() /* TODO */,
                            CustomFieldKeys.REDIRECT_URL_FIELD, response.get("redirecturl"),
                            CustomFieldKeys.TRANSACTION_ID_FIELD, transaction.getId(),
                            CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.now() /* TODO */));
                return update(paymentWithCartLike, updatedPayment, ImmutableList.of(
                        interfaceInteraction,
                        SetInterfaceId.of(response.get("txid")),
                        SetCustomField.ofObject(CustomFieldKeys.REDIRECT_URL_FIELD, response.get("redirecturl"))));
            } else {
                final AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE,
                    ImmutableMap.of(CustomFieldKeys.RESPONSE_FIELD, response.toString() /* TODO */,
                            CustomFieldKeys.TRANSACTION_ID_FIELD, transaction.getId(),
                            CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.now() /* TODO */));

                if (status.equals("APPROVED")) {
                    return update(paymentWithCartLike, updatedPayment, ImmutableList.of(
                            interfaceInteraction,
                            SetInterfaceId.of(response.get("txid")),
                            SetAuthorization.of(updatedPayment.getAmountPlanned()),
                            ChangeTransactionState.of(TransactionState.SUCCESS, transaction.getId()),
                            ChangeTransactionTimestamp.of(ZonedDateTime.now(), transaction.getId())
                    ));
                } else if (status.equals("ERROR")) {
                    return update(paymentWithCartLike, updatedPayment, ImmutableList.of(
                        interfaceInteraction,
                        ChangeTransactionState.of(TransactionState.FAILURE, transaction.getId()),
                        ChangeTransactionTimestamp.of(ZonedDateTime.now(), transaction.getId())
                    ));
                } else if (status.equals("PENDING")) {
                    return update(paymentWithCartLike, updatedPayment, ImmutableList.of(
                            interfaceInteraction,
                            SetInterfaceId.of(response.get("txid"))));
                }
            }

            throw new IllegalStateException("Unknown PayOne status");
        }
        catch (PayoneException pe) {
            final AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE,
                    ImmutableMap.of(CustomFieldKeys.RESPONSE_FIELD, pe.getMessage() /* TODO */,
                            CustomFieldKeys.TRANSACTION_ID_FIELD, transaction.getId(),
                            CustomFieldKeys.TIMESTAMP_FIELD, ZonedDateTime.now() /* TODO */));
            return update(paymentWithCartLike, updatedPayment, ImmutableList.of(interfaceInteraction));
        }
    }

    private PaymentWithCartLike update(PaymentWithCartLike paymentWithCartLike, Payment payment, ImmutableList<UpdateActionImpl<Payment>> updateActions) {
        return paymentWithCartLike.withPayment(
            client.executeBlocking(PaymentUpdateCommand.of(payment, updateActions)));
    }
}
