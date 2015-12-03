package com.commercetools.pspadapter.payone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClientFactory;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentDraft;
import io.sphere.sdk.payments.PaymentDraftBuilder;
import io.sphere.sdk.payments.TransactionDraft;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.money.MonetaryAmount;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * @author fhaertig
 * @date 03.12.15
 */
public class PaymentQueryExecutorTest {

    private CommercetoolsClient client;

    @Before
    public void setUp() throws Exception {
        client = CommercetoolsClientFactory.createClient(
                System.getProperty("CT_PROJECT_KEY"),
                System.getProperty("CT_CLIENT_ID"),
                System.getProperty("CT_CLIENT_SECRET"));
        MonetaryAmount monetaryAmount = MoneyImpl.ofCents(20000, "EUR");

        final List<TransactionDraft> transactions = Collections.singletonList(TransactionDraftBuilder
                .of(TransactionType.CHARGE, monetaryAmount, ZonedDateTime.now())
                .timestamp(ZonedDateTime.now())
                .interactionId("1")
                .state(TransactionState.SUCCESS)
                .build());

        PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .transactions(transactions).build();

        PaymentCreateCommand paymentCreateCommand = PaymentCreateCommand.of(paymentDraft);
        final CompletionStage<Payment> payment = client.execute(paymentCreateCommand);
        payment.toCompletableFuture().get();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetPaymentsSince() throws Exception {
        PaymentQueryExecutor paymentQueryExecutor = new PaymentQueryExecutor(client);
        List<Payment> paymentList = new ArrayList<>(paymentQueryExecutor.getPaymentsSince(new Date()));
        assertThat(paymentList, hasSize(1));
    }
}