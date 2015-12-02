package specs.common;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentDraft;
import io.sphere.sdk.payments.PaymentDraftBuilder;
import io.sphere.sdk.payments.TransactionDraft;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentDeleteCommand;
import io.sphere.sdk.utils.MoneyImpl;
import org.concordion.api.ExpectedToFail;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import javax.money.MonetaryAmount;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

@ExpectedToFail
@RunWith(ConcordionRunner.class)
public class IdempotenceFixture {

    private SphereClient client;
    private Payment p;

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        String ctProjectKey = System.getProperty("CT_PROJECT_KEY");
        String ctClientId = System.getProperty( "CT_CLIENT_ID" );
        String ctClientSecret = System.getProperty( "CT_CLIENT_SECRET" );

        SphereClientFactory factory = SphereClientFactory.of();
        client = factory.createClient(ctProjectKey, ctClientId, ctClientSecret);

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
        p = payment.toCompletableFuture().get();
    }

    @After
    public void tearDown() {
        client.execute(PaymentDeleteCommand.of(p));
    }

    public Result handleSuccessfulPayment() {

        String header = "TODO";
        String body = "TODO";

        return new Result(header, body);
    }

    public Result handleFailedPayment() {

        String header = "TODO";
        String body = "TODO";

        return new Result(header, body);
    }

    class Result {
        public String header;
        public String body;

        public Result(final String header, final String body) {
            this.header = header;
            this.body = body;
        }

        public boolean hasEmptyBody() {
            return this.body.isEmpty();
        }
    }
}
