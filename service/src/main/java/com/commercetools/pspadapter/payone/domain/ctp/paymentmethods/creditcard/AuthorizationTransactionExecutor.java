package com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.creditcard;

import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.IdempotentTransactionExecutor;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PreauthorizationRequest;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionState;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionTimestamp;
import io.sphere.sdk.payments.commands.updateactions.SetAuthorization;
import io.sphere.sdk.payments.commands.updateactions.SetInterfaceId;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class AuthorizationTransactionExecutor implements IdempotentTransactionExecutor {
    private final LoadingCache<String, Type> typeCache;
    private final PayoneRequestFactory requestFactory;
    private final PayonePostService payonePostService;
    private final BlockingClient client;

    public AuthorizationTransactionExecutor(LoadingCache<String, Type> typeCache, PayoneRequestFactory requestFactory, PayonePostService payonePostService, BlockingClient client) {
        this.typeCache = typeCache;
        this.requestFactory = requestFactory;
        this.payonePostService = payonePostService;
        this.client = client;
    }

    @Override
    public TransactionType supportedTransactionType() {
        return TransactionType.AUTHORIZATION;
    }

    @Override
    public boolean wasExecuted(PaymentWithCartLike paymentWithCartLike, Transaction transaction) {
        return getCustomFieldsOfType(paymentWithCartLike, CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE, CustomTypeBuilder.PAYONE_INTERACTION_REDIRECT)
            .anyMatch(i -> i.getFieldAsString(CustomTypeBuilder.TRANSACTION_ID_FIELD).equals(transaction.getId()));
    }

    @Override
    public PaymentWithCartLike attemptFirstExecution(PaymentWithCartLike paymentWithCartLike, Transaction transaction) {
        return attemptExecution(paymentWithCartLike, transaction);
    }

    @Override
    public Optional<CustomFields> findLastExecutionAttempt(PaymentWithCartLike paymentWithCartLike, Transaction transaction) {
        return getCustomFieldsOfType(paymentWithCartLike, CustomTypeBuilder.PAYONE_INTERACTION_REQUEST)
            .filter(i -> i.getFieldAsString(CustomTypeBuilder.TRANSACTION_ID_FIELD).equals(transaction.getId()))
            .reduce((previous, current) -> current); // .findLast()
    }

    @Override
    public PaymentWithCartLike retryLastExecutionAttempt(PaymentWithCartLike paymentWithCartLike, Transaction transaction, CustomFields lastExecutionAttempt) {
        if (lastExecutionAttempt.getFieldAsDateTime(CustomTypeBuilder.TIMESTAMP_FIELD).isBefore(ZonedDateTime.now().minusMinutes(5))) {
            return attemptExecution(paymentWithCartLike, transaction);
        }
        return paymentWithCartLike;
    }

    private Stream<CustomFields> getCustomFieldsOfType(PaymentWithCartLike paymentWithCartLike, String... typeKeys) {
        return paymentWithCartLike
            .getPayment()
            .getInterfaceInteractions()
            .stream()
            .filter(i -> Arrays.stream(typeKeys)
                .map(t -> typeCache.getUnchecked(t).toReference())
                .anyMatch(t -> t.equals(i.getType())));
    }

    private PaymentWithCartLike attemptExecution(PaymentWithCartLike paymentWithCartLike, Transaction transaction) {
        final PreauthorizationRequest request = requestFactory.createPreauthorizationRequest(paymentWithCartLike);

        final Payment updatedPayment = client.complete(
            PaymentUpdateCommand.of(paymentWithCartLike.getPayment(),
                AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_REQUEST,
                    ImmutableMap.of(CustomTypeBuilder.REQUEST_FIELD, request.toStringMap(true).toString() /* TODO */,
                        CustomTypeBuilder.TRANSACTION_ID_FIELD, transaction.getId(),
                        CustomTypeBuilder.TIMESTAMP_FIELD, ZonedDateTime.now() /* TODO */))));

        try {
            final Map<String, String> response = payonePostService.executePost(request);

            final String status = response.get("status");
            if (status.equals("REDIRECT")) {
                final AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_REDIRECT,
                    ImmutableMap.of(CustomTypeBuilder.REDIRECT_URL_FIELD, response.get("redirecturl"),
                        CustomTypeBuilder.TRANSACTION_ID_FIELD, transaction.getId(),
                        CustomTypeBuilder.TIMESTAMP_FIELD, ZonedDateTime.now() /* TODO */));
                return update(paymentWithCartLike, updatedPayment, ImmutableList.of(interfaceInteraction));
            } else {
                final AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE,
                    ImmutableMap.of(CustomTypeBuilder.RESPONSE_FIELD, response.toString() /* TODO */,
                        CustomTypeBuilder.TRANSACTION_ID_FIELD, transaction.getId(),
                        CustomTypeBuilder.TIMESTAMP_FIELD, ZonedDateTime.now() /* TODO */));

                if (status.equals("APPROVED")) {
                    return update(paymentWithCartLike, updatedPayment, ImmutableList.of(
                        interfaceInteraction,
                        ChangeTransactionState.of(TransactionState.SUCCESS, transaction.getId()),
                        ChangeTransactionTimestamp.of(ZonedDateTime.now(), transaction.getId()),
                        SetInterfaceId.of(response.get("txid")),
                        SetAuthorization.of(paymentWithCartLike.getPayment().getAmountPlanned())
                    ));
                } else if (status.equals("ERROR")) {
                    return update(paymentWithCartLike, updatedPayment, ImmutableList.of(
                        interfaceInteraction,
                        ChangeTransactionState.of(TransactionState.FAILURE, transaction.getId()),
                        ChangeTransactionTimestamp.of(ZonedDateTime.now(), transaction.getId())
                    ));
                } else if (status.equals("PENDING")) {
                    return update(paymentWithCartLike, updatedPayment, ImmutableList.of(interfaceInteraction));
                }
            }

            throw new IllegalStateException("Unknown PayOne status");
        }
        catch (PayoneException pe) {
            final AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE,
                    ImmutableMap.of(CustomTypeBuilder.RESPONSE_FIELD, pe.getMessage() /* TODO */,
                        CustomTypeBuilder.TRANSACTION_ID_FIELD, transaction.getId(),
                        CustomTypeBuilder.TIMESTAMP_FIELD, ZonedDateTime.now() /* TODO */));
            return update(paymentWithCartLike, updatedPayment, ImmutableList.of(interfaceInteraction));
        }
    }

    private PaymentWithCartLike update(PaymentWithCartLike paymentWithCartLike, Payment payment, ImmutableList<UpdateActionImpl<Payment>> updateActions) {
        return paymentWithCartLike.withPayment(
            client.complete(PaymentUpdateCommand.of(payment, updateActions)));
    }
}
