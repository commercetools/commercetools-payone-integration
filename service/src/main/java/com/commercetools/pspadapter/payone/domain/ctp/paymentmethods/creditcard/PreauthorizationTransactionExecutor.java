package com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.creditcard;

import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.IdempotentTransactionExecutor;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CCPreauthorizationRequest;
import com.commercetools.pspadapter.payone.mapping.CreditCardRequestFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionState;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionTimestamp;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class PreauthorizationTransactionExecutor implements IdempotentTransactionExecutor {
    private final LoadingCache<String, Type> typeCache;
    private final CreditCardRequestFactory requestFactory;
    private final PayonePostService payonePostService;
    private final BlockingClient client;

    public PreauthorizationTransactionExecutor(LoadingCache<String, Type> typeCache, CreditCardRequestFactory requestFactory, PayonePostService payonePostService, BlockingClient client) {
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
        final CCPreauthorizationRequest request = requestFactory.createPreauthorizationRequest(paymentWithCartLike, null);

        final Payment updatedPayment = client.complete(
            PaymentUpdateCommand.of(paymentWithCartLike.getPayment(),
                AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_REQUEST,
                    ImmutableMap.of(CustomTypeBuilder.REQUEST_FIELD, request.toString() /* TODO */,
                        CustomTypeBuilder.TRANSACTION_ID_FIELD, transaction.getId(),
                        CustomTypeBuilder.TIMESTAMP_FIELD, ZonedDateTime.now() /* TODO */))));

        final Map<String, String> response = payonePostService.executePost(request.toStringMap());


        final String status = response.get("status");
        if (status.equals("REDIRECT")) {
            return paymentWithCartLike.withPayment(client.complete(
                PaymentUpdateCommand.of(updatedPayment,
                    AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_REDIRECT,
                        ImmutableMap.of(CustomTypeBuilder.REDIRECT_URL, response.get("redirecturl"),
                            CustomTypeBuilder.TRANSACTION_ID_FIELD, transaction.getId(),
                            CustomTypeBuilder.TIMESTAMP_FIELD, ZonedDateTime.now() /* TODO */)))));
        }
        else {
            // TODO: Check for error response
            final AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE,
                ImmutableMap.of(CustomTypeBuilder.RESPONSE_FIELD, response.toString() /* TODO */,
                    CustomTypeBuilder.TRANSACTION_ID_FIELD, transaction.getId(),
                    CustomTypeBuilder.TIMESTAMP_FIELD, ZonedDateTime.now() /* TODO */));
            if (status.equals("APPROVED")) {
                return paymentWithCartLike.withPayment(client.complete(
                        PaymentUpdateCommand.of(updatedPayment,
                            ImmutableList.of(
                                interfaceInteraction,
                                ChangeTransactionState.of(TransactionState.SUCCESS, transaction.getId()),
                                ChangeTransactionTimestamp.of(ZonedDateTime.now(), transaction.getId())
                            )))
                );
            }
            else if (status.equals("ERROR")) {
                return paymentWithCartLike.withPayment(client.complete(
                        PaymentUpdateCommand.of(updatedPayment,
                            ImmutableList.of(
                                interfaceInteraction,
                                ChangeTransactionState.of(TransactionState.FAILURE, transaction.getId()),
                                ChangeTransactionTimestamp.of(ZonedDateTime.now(), transaction.getId())
                            )))
                );
            }
            else if (status.equals("PENDING")) {
                return paymentWithCartLike.withPayment(client.complete(
                        PaymentUpdateCommand.of(updatedPayment,
                            ImmutableList.of(
                                interfaceInteraction
                            )))
                );
            }
        }

        throw new IllegalStateException("Unknown PayOne status");
    }

    @Override
    public Optional<CustomFields> findLastExecutionAttempt(PaymentWithCartLike paymentWithCartLike, Transaction transaction) {
        return null;
    }

    @Override
    public PaymentWithCartLike retryLastExecutionAttempt(PaymentWithCartLike paymentWithCartLike, Transaction transaction, CustomFields lastExecutionAttempt) {
        return null;
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

}
