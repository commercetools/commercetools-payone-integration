package specs.paymentmethods.creditcard;

import com.commercetools.pspadapter.payone.IntegrationService;
import com.commercetools.pspadapter.payone.ServiceConfig;
import com.commercetools.pspadapter.payone.ServiceFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentDraft;
import io.sphere.sdk.payments.PaymentDraftBuilder;
import io.sphere.sdk.payments.PaymentMethodInfoBuilder;
import io.sphere.sdk.payments.TransactionDraft;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentDeleteCommand;
import io.sphere.sdk.payments.queries.PaymentByIdGet;
import io.sphere.sdk.payments.queries.PaymentQuery;
import io.sphere.sdk.utils.MoneyImpl;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.concordion.api.ExpectedToFail;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import specs.BaseFixture;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/**
 * @author fhaertig
 * @date 10.12.15
 */
@ExpectedToFail
@RunWith(ConcordionRunner.class)
public class AuthorizationFixture extends BaseFixture {

    private SphereClient sphereClient;
    private IntegrationService integrationService;
    private Map<String, Payment> payments = Maps.newHashMap();

    @Before
    public void initializeService() throws MalformedURLException {

        final ServiceConfig serviceConfig = new ServiceConfig(new URL(getPayOneApiUrl()));

        //only for creation of test data
        sphereClient = SphereClientFactory.of().createClient(
                serviceConfig.getCtProjectKey(),
                serviceConfig.getCtClientId(),
                serviceConfig.getCtClientSecret());

        integrationService = ServiceFactory.createService(serviceConfig);
        integrationService.start();
    }

    public String createPayment(
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode) throws ExecutionException, InterruptedException {

        MonetaryAmount monetaryAmount = MoneyImpl.of(centAmount, currencyCode);

        final List<TransactionDraft> transactions = Collections.singletonList(TransactionDraftBuilder
                .of(TransactionType.valueOf(transactionType), monetaryAmount, ZonedDateTime.now())
                .state(TransactionState.PENDING)
                .build());

        PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .paymentMethodInfo(PaymentMethodInfoBuilder.of().method(paymentMethod).build())
                .transactions(transactions).build();

        PaymentCreateCommand paymentCreateCommand = PaymentCreateCommand.of(paymentDraft);
        final CompletionStage<Payment> queryResult = sphereClient.execute(paymentCreateCommand);
        Payment payment = queryResult.toCompletableFuture().get();
        payments.put(payment.getId(), payment);
        return payment.getId();
    }

    public boolean handlePayment(final String paymentId) throws IOException, ExecutionException, InterruptedException {

        final HttpResponse response = Request.Get(getHandlePaymentUrl(paymentId))
                .connectTimeout(200)
                .execute()
                .returnResponse();

        payments.put(paymentId, sphereClient.execute(PaymentByIdGet.of(paymentId)).toCompletableFuture().get());
        return response.getStatusLine().getStatusCode() == 200;
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        integrationService.stop();

        //delete all payments
        List<Payment> paymentList = Lists.newArrayList(sphereClient.execute(PaymentQuery.of()).toCompletableFuture().get().getResults());
        paymentList.forEach(p -> sphereClient.execute(PaymentDeleteCommand.of(p)));
    }

    public int getInterfaceInteractionsCount(final String paymentId) throws ExecutionException, InterruptedException {
        //TODO: specific for authorization
        return payments.get(paymentId).getInterfaceInteractions().size();
    }

    public String getInterfaceInteractionState(final String paymentId) {
        //return payments.get(paymentId).getInterfaceInteractions().get(0).getField();
        return "TODO";
    }

    public String getInterfaceInteractionMethod(final String paymentId) {
        //return payments.get(paymentId).getInterfaceInteractions().get(0).getField();
        return "TODO";
    }
}
