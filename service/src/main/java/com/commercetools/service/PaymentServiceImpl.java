package com.commercetools.service;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentDraft;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.queries.PaymentQuery;
import io.sphere.sdk.queries.PagedQueryResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class PaymentServiceImpl implements PaymentService {

    private final SphereClient client;

    public PaymentServiceImpl(SphereClient sphereClient) {
        this.client = sphereClient;
    }

    @Override
    public CompletionStage<Payment> createPayment(PaymentDraft paymentDraft) {
        return client.execute(PaymentCreateCommand.of(paymentDraft));
    }

    @Override
    public CompletionStage<Optional<Payment>> getByPaymentMethodAndInterfaceId(String paymentMethodInterface, String interfaceId) {
        return client.execute(
                PaymentQuery.of()
                        .withPredicates(p -> p.interfaceId().is(interfaceId))
                        .plusPredicates(p -> p.paymentMethodInfo().paymentInterface().is(paymentMethodInterface)))
                .thenApplyAsync(PagedQueryResult::head);
    }

    @Override
    public CompletionStage<Payment> updatePayment(Payment payment, List<UpdateAction<Payment>> updateActions) {
        return client.execute(PaymentUpdateCommand.of(payment, updateActions));
    }
}
