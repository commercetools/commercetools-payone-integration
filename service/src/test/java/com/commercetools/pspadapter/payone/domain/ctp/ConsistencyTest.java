package com.commercetools.pspadapter.payone.domain.ctp;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.MethodKeys;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentDeleteCommand;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.*;
import io.sphere.sdk.payments.queries.PaymentByIdGet;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.Before;
import org.junit.Test;

import javax.money.MonetaryAmount;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ConsistencyTest {
    BlockingClient client;

    @Before
    public void setUp() throws Exception {
        final SphereClientFactory sphereClientFactory = SphereClientFactory.of();
        client =  new CommercetoolsClient(
                sphereClientFactory.createClient(
                        "payone-47",
                        "P72guf3QpxqwdzBQPVK-5rK9",
                        "GDALYUMm0ZilPhNcxwVCjDUL764HnoDz"));
    }

    @Test
    public void test() throws Exception {
        for (int i = 0; i < 1000; i++) {
            createUpdateDeletePayment();
        }
    }

    private void createUpdateDeletePayment() {
        // Create Payment
        final MonetaryAmount monetaryAmount = MoneyImpl.ofCents(1000l, "EUR");

        final List<TransactionDraft> transactions = Collections.singletonList(TransactionDraftBuilder
            .of(TransactionType.AUTHORIZATION, monetaryAmount, ZonedDateTime.now())
            .state(TransactionState.PENDING)
            .build());

        final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                        .method(MethodKeys.CREDIT_CARD)
                        .paymentInterface("PAYONE")
                        .build())
                .transactions(transactions)
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                    CustomTypeBuilder.PAYMENT_CREDIT_CARD,
                    ImmutableMap.of(
                        CustomTypeBuilder.CARD_DATA_PLACEHOLDER_FIELD, "TEST",
                        CustomTypeBuilder.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage(),
                        CustomTypeBuilder.REFERENCE_FIELD, "myGlobalKey")))
                .build();

        final Payment payment = client.complete(PaymentCreateCommand.of(paymentDraft));
        final Transaction transaction = payment.getTransactions().get(0);

        // Update 1
        final Payment updatedPayment = client.complete(
            PaymentUpdateCommand.of(payment,
                AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_REQUEST,
                    ImmutableMap.of(CustomTypeBuilder.REQUEST_FIELD, "blabla",
                        CustomTypeBuilder.TRANSACTION_ID_FIELD, transaction.getId(),
                        CustomTypeBuilder.TIMESTAMP_FIELD, ZonedDateTime.now()))));

        // Update 2
        final AddInterfaceInteraction interfaceInteraction = AddInterfaceInteraction.ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE,
                    ImmutableMap.of(CustomTypeBuilder.RESPONSE_FIELD, "blabla" /* TODO */,
                        CustomTypeBuilder.TRANSACTION_ID_FIELD, transaction.getId(),
                        CustomTypeBuilder.TIMESTAMP_FIELD, ZonedDateTime.now() /* TODO */));

        final Payment updatedPayment2 = client.complete(
            PaymentUpdateCommand.of(updatedPayment,
                ImmutableList.of(
                    interfaceInteraction,
                    ChangeTransactionState.of(TransactionState.SUCCESS, transaction.getId()),
                    ChangeTransactionTimestamp.of(ZonedDateTime.now(), transaction.getId()),
                    SetInterfaceId.of("blabla"),
                    SetAuthorization.of(payment.getAmountPlanned()))));

        // Retrieve
        final Payment complete = client.complete(PaymentByIdGet.of(payment.getId()));

        // Test
        assertThat(complete.getVersion()).isSameAs(updatedPayment2.getVersion());

        // Delete
        client.complete(PaymentDeleteCommand.of(complete));
    }

}
