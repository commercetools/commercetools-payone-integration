package specs.common;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.commercetools.pspadapter.payone.IntegrationService;
import com.commercetools.pspadapter.payone.ServiceConfig;
import com.commercetools.pspadapter.payone.ServiceFactory;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Request;
import org.concordion.api.ExpectedToFail;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.After;
import org.junit.Rule;
import org.junit.runner.RunWith;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

@ExpectedToFail
@RunWith(ConcordionRunner.class)
public class IdempotenceFixture {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private List<Payment> payments = Lists.newArrayList();
    private Map<String, String> uniquePaymentId = Maps.newHashMap();

    private String payoneApiUrl;

    private SphereClient sphereClient;
    private IntegrationService integrationService;

    @Rule
    public WireMockRule payoneProxy = new WireMockRule(wireMockConfig().port(1709).httpsPort(2707));

    @After
    public void tearDown() {
        integrationService.stop();

        payments.forEach(p -> sphereClient.execute(PaymentDeleteCommand.of(p)));
    }

    public void initializeService(final String payoneApiUrl) throws MalformedURLException {
        this.payoneApiUrl = payoneApiUrl;

        final URL payoneProxyBaseUrl = new URL(payoneApiUrl);

        final URL payoneProxyUrl = new URL(
                payoneProxyBaseUrl.getProtocol(),
                "localhost",
                payoneProxy.httpsPort(),
                payoneProxyBaseUrl.getFile());

        payoneProxy.stubFor(any(urlPathEqualTo(payoneProxyBaseUrl.getPath()))
                .willReturn(aResponse().proxiedFrom(payoneProxyBaseUrl.toExternalForm())));

        final ServiceConfig serviceConfig = new ServiceConfig(payoneProxyUrl);

        sphereClient = SphereClientFactory.of().createClient(
                serviceConfig.getCtProjectKey(),
                serviceConfig.getCtClientId(),
                serviceConfig.getCtClientSecret());

        integrationService = ServiceFactory.createService(serviceConfig);
        integrationService.start();
    }

    public String createPayment(final String paymentName, final String transactionStatus)
            throws ExecutionException, InterruptedException {
        Preconditions.checkArgument(!this.uniquePaymentId.containsKey(paymentName), "Payment " + paymentName + " already exists!");

        final MonetaryAmount monetaryAmount = MoneyImpl.ofCents(20000, "EUR");

        final List<TransactionDraft> transactions = Collections.singletonList(TransactionDraftBuilder
                .of(TransactionType.CHARGE, monetaryAmount, ZonedDateTime.now())
                .timestamp(ZonedDateTime.now())
                .interactionId("1")
                .state(TransactionState.ofSphereValue(transactionStatus))
                .build());

        final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .transactions(transactions).build();

        final PaymentCreateCommand paymentCreateCommand = PaymentCreateCommand.of(paymentDraft);
        final CompletionStage<Payment> payment = sphereClient.execute(paymentCreateCommand);
        payments.add(payment.toCompletableFuture().get());

        final String paymentId = Iterables.getLast(payments).getId();
        uniquePaymentId.put(paymentName, paymentId);

        return paymentId;
    }

    public Result handlePayment(final String requestMethod, final String paymentUri) throws IOException {
        Preconditions.checkArgument("GET".equals(requestMethod), "GET request expected to handle payment.");

        final HttpResponse response = Request.Get("http://localhost:8080" + resolvePaymentId(paymentUri))
                .connectTimeout(200)
                .execute()
                .returnResponse();

        final String header = toStatus(response);

        final JsonResponse jsonResponse = gson.fromJson(
                new InputStreamReader(response.getEntity().getContent()),
                JsonResponse.class);

        return new Result(header, gson.toJson(jsonResponse));
    }

    /**
     * Gets the actual payment ID for the (human-readable) ID used in the {@code paymentUri}
     * @param paymentUri an URI containing a human-readable payment ID
     * @return the unique payment ID
     */
    private String resolvePaymentId(final String paymentUri) {
        final int startOfPaymentName = paymentUri.lastIndexOf('/') + 1;
        final String paymentName = paymentUri.substring(startOfPaymentName);

        final String paymentId = Preconditions.checkNotNull(uniquePaymentId.get(paymentName), "Unknown payment " + paymentName);
        final String uriWithResolvedId = paymentUri.substring(0, startOfPaymentName) + paymentId;
        return uriWithResolvedId;
    }

    private static String toStatus(final HttpResponse response) throws IOException {
        final StatusLine statusLine = response.getStatusLine();
        return Joiner.on(" ").join(
                statusLine.getProtocolVersion(),
                statusLine.getStatusCode(),
                statusLine.getReasonPhrase());
    }

    public String getNumberOfRequestsToPayone() {
        final List<LoggedRequest> loggedRequests = payoneProxy.findAll(anyRequestTo(this.payoneApiUrl));
        final String count = Integer.toString(loggedRequests.size());
        return count;
    }

    private static RequestPatternBuilder anyRequestTo(String targetUrl) {
        return new RequestPatternBuilder(RequestMethod.ANY, urlMatching(targetUrl));
    }

    static class Result {
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

    public static class JsonResponse {
        private int statusCode;
        private String message;
        private Integer paymentVersion;
    }

}
