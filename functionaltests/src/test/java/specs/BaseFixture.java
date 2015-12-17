package specs;

import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.ctp.TypeCacheLoader;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import io.sphere.sdk.carts.commands.CartDeleteCommand;
import io.sphere.sdk.carts.queries.CartQuery;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.orders.commands.OrderDeleteCommand;
import io.sphere.sdk.orders.queries.OrderQuery;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.commands.PaymentDeleteCommand;
import io.sphere.sdk.payments.queries.PaymentByIdGet;
import io.sphere.sdk.payments.queries.PaymentQuery;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.utils.MoneyImpl;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.After;
import org.junit.Before;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/**
 * @author fhaertig
 * @date 10.12.15
 */
public abstract class BaseFixture {
    private static final String TEST_DATA_VISA_CREDIT_CARD_NO_3_DS = "TEST_DATA_VISA_CREDIT_CARD_NO_3DS";
    private static final String TEST_DATA_VISA_CREDIT_CARD_3_DS = "TEST_DATA_VISA_CREDIT_CARD_NO_3DS";

    private static final Random randomSource = new Random();
    private BlockingClient ctpClient;
    private URL ctPayoneIntegrationBaseUrl;
    private LoadingCache<String, Type> typeCache;

    @Before
    public void initializeCommercetoolsClient() throws MalformedURLException {
        final PropertyProvider propertyProvider = new PropertyProvider();

        ctPayoneIntegrationBaseUrl = new URL(propertyProvider.getEnvironmentOrSystemValue("CT_PAYONE_INTEGRATION_URL")
                .filter(string -> !string.isEmpty())
                .orElseThrow(() -> new IllegalStateException("CT_PAYONE_INTEGRATION_URL missing")));

        final ServiceConfig serviceConfig = new ServiceConfig(propertyProvider);

        //only for creation of test data
        ctpClient = new CommercetoolsClient(SphereClientFactory.of().createClient(
                serviceConfig.getCtProjectKey(),
                serviceConfig.getCtClientId(),
                serviceConfig.getCtClientSecret()));

        resetCommercetoolsPlatform();

        typeCache = CacheBuilder.newBuilder().build(new TypeCacheLoader(ctpClient));
    }

    @After
    public void tearDown() {
        resetCommercetoolsPlatform();
    }

    public URL ctPayOneIntegrationBaseUrl() {
        return ctPayoneIntegrationBaseUrl;
    }

    public String getPayOneApiUrl() {
        return "https://api.pay1.de/post-gateway/";
    }

    public String getHandlePaymentUrl(final String paymentId) throws MalformedURLException {
        return new URL(
                ctPayoneIntegrationBaseUrl.getProtocol(),
                ctPayoneIntegrationBaseUrl.getHost(),
                ctPayoneIntegrationBaseUrl.getPort(),
                "/commercetools/handle/payments/" + paymentId)
                .toExternalForm();
    }

    public HttpResponse sendGetRequestToUrl(final String url) throws IOException {
        return Request.Get(url)
                .connectTimeout(200)
                .execute()
                .returnResponse();
    }

    protected String getGetConfigurationParameter(final String configParameterName) {
        final String envVariable = System.getenv(configParameterName);
        if (!Strings.isNullOrEmpty(envVariable)) {
            return envVariable;
        }

        final String sysProperty = System.getProperty(configParameterName);
        if (!Strings.isNullOrEmpty(sysProperty)) {
            return sysProperty;
        }

        throw new RuntimeException(String.format(
                "Environment variable required for configuration must not be null or empty: %s",
                configParameterName));
    }

    protected MonetaryAmount createMonetaryAmountFromCent(final Long centAmount, final String currencyCode) {
        return MoneyImpl.ofCents(centAmount, currencyCode);
    }

    protected String getUnconfirmedVisaPseudoCardPan() {
        return getGetConfigurationParameter(TEST_DATA_VISA_CREDIT_CARD_NO_3_DS);
    }

    protected String getVerifiedVisaPseudoCardPan() {
        return getGetConfigurationParameter(TEST_DATA_VISA_CREDIT_CARD_3_DS);
    }

    protected String getRandomOrderNumber() {
        return String.valueOf(randomSource.nextInt() + System.currentTimeMillis());
    }

    protected BlockingClient ctpClient() {
        return ctpClient;
    }

    protected void resetCommercetoolsPlatform() {
        // TODO jw: use futures
        // delete all orders
        ctpClient.complete(OrderQuery.of().withLimit(500)).getResults()
                .forEach(order -> ctpClient.complete(OrderDeleteCommand.of(order)));

        // delete all carts
        ctpClient.complete(CartQuery.of().withLimit(500)).getResults()
                .forEach(cart -> ctpClient.complete(CartDeleteCommand.of(cart)));

        // delete all payments
        ctpClient.complete(PaymentQuery.of().withLimit(500)).getResults()
                .forEach(payment -> ctpClient.complete(PaymentDeleteCommand.of(payment)));
    }

    protected Payment fetchPayment(final String paymentId) {
        return ctpClient.complete(PaymentByIdGet.of(paymentId));
    }

    /**
     * Gets the ID of the type with name (aka key) {@code typeName}.
     * @param typeName the name (key) of a type
     * @return the type's ID
     * @throws ExecutionException if a checked exception was thrown while loading the value.
     */
    public String typeIdOfFromTypeName(final String typeName) throws ExecutionException {
        return typeCache.get(typeName).getId();
    }

    public String getTransactionState(final String paymentId, final String transactionId) {
        final Optional<Transaction> transaction = fetchPayment(paymentId).getTransactions()
                .stream()
                .filter(i -> transactionId.equals(i.getId()))
                .findFirst();

        final TransactionState transactionState = transaction.isPresent()? transaction.get().getState() : null;
        return transactionState != null? transactionState.toSphereName() : "<UNKNOWN>";
    }

    public String getIdOfLastTransaction(final String paymentId) {
        return Iterables.getLast(fetchPayment(paymentId).getTransactions()).getId();
    }
}

