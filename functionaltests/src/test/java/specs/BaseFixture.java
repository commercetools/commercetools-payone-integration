package specs;

import com.commercetools.pspadapter.payone.ServiceFactory;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterables;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.queries.PaymentByIdGet;
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
 * @since  10.12.15
 */
public abstract class BaseFixture {
    protected static final String EMPTY_STRING = "";
    private static final String TEST_DATA_VISA_CREDIT_CARD_NO_3_DS = "TEST_DATA_VISA_CREDIT_CARD_NO_3DS";
    private static final String TEST_DATA_VISA_CREDIT_CARD_3_DS = "TEST_DATA_VISA_CREDIT_CARD_NO_3DS";

    private static final Random randomSource = new Random();
    private BlockingClient ctpClient;
    private URL ctPayoneIntegrationBaseUrl;
    private LoadingCache<String, Type> typeCache;

    /** maps legible payment names to payment IDs (and vice versa)*/
    private BiMap<String, String> payments = HashBiMap.create();

    // FIXME use BeforeClass
    @Before
    public void initializeCommercetoolsClient() throws MalformedURLException {
        final PropertyProvider propertyProvider = new PropertyProvider();
        ctPayoneIntegrationBaseUrl =
                new URL(propertyProvider.getMandatoryNonEmptyProperty("CT_PAYONE_INTEGRATION_URL"));

        //only for creation of test data
        final ServiceFactory serviceFactory = ServiceFactory.withPropertiesFrom(propertyProvider);
        ctpClient = serviceFactory.createCommercetoolsClient();

        resetCommercetoolsPlatform();

        typeCache = serviceFactory.createTypeCache(ctpClient);
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

    protected String getConfigurationParameter(final String configParameterName) {
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
        return getConfigurationParameter(TEST_DATA_VISA_CREDIT_CARD_NO_3_DS);
    }

    protected String getVerifiedVisaPseudoCardPan() {
        return getConfigurationParameter(TEST_DATA_VISA_CREDIT_CARD_3_DS);
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
        //ctpClient.complete(OrderQuery.of().withLimit(500)).getResults()
        //        .forEach(order -> ctpClient.complete(OrderDeleteCommand.of(order)));

        // delete all carts
        //ctpClient.complete(CartQuery.of().withLimit(500)).getResults()
        //        .forEach(cart -> ctpClient.complete(CartDeleteCommand.of(cart)));

        // delete all payments
        //ctpClient.complete(PaymentQuery.of().withLimit(500)).getResults()
        //        .forEach(payment -> ctpClient.complete(PaymentDeleteCommand.of(payment)));
    }

    /**
     * Gets the latest version of the payment via its legible name - a name that exits only in this test context.
     * @param paymentName legible name of the payment
     * @return the payment fetched from the commercetools client
     * @see #fetchPaymentById(String)
     */
    protected Payment fetchPaymentByLegibleName(final String paymentName) {
        return fetchPaymentById(payments.get(paymentName));
    }

    /**
     * Gets the latest version of the payment via its ID.
     * @param paymentId unique ID of the payment
     * @return the payment fetched from the commercetools client
     * @see #fetchPaymentByLegibleName(String)
     */
    protected Payment fetchPaymentById(final String paymentId) {
        return ctpClient.complete(PaymentByIdGet.of(
                Preconditions.checkNotNull(paymentId, "paymentId must not be null!")));
    }

    /**
     * Gets the ID of the type with name (aka key) {@code typeName}.
     * @param typeName the name (key) of a type
     * @return the type's ID
     * @throws ExecutionException if a checked exception was thrown while loading the value.
     */
    protected String typeIdFromTypeName(final String typeName) throws ExecutionException {
        return typeCache.get(typeName).getId();
    }

    public String getTransactionStateByPaymentId(final String paymentId, final String transactionId) {
        return getTransactionState(fetchPaymentById(paymentId), transactionId);
    }

    protected String getTransactionState(final Payment payment, final String transactionId) {
        final Optional<Transaction> transaction = payment.getTransactions()
                .stream()
                .filter(i -> transactionId.equals(i.getId()))
                .findFirst();

        final TransactionState transactionState = transaction.isPresent()? transaction.get().getState() : null;
        return transactionState != null? transactionState.toSphereName() : "<UNKNOWN>";
    }

    public String getIdOfLastTransactionByPaymentId(final String paymentId) {
        return getIdOfLastTransaction(fetchPaymentById(paymentId));
    }

    protected String getIdOfLastTransaction(final Payment payment) {
        Preconditions.checkNotNull(payment, "payment must not be null");
        return Iterables.getLast(payment.getTransactions()).getId();
    }

    protected void registerPaymentWithLegibleName(final String paymentName, final Payment payment) {
        Preconditions.checkState(!payments.containsKey(paymentName),
                String.format("Legible payment name '%s' already in use for ID '%s'.",
                        paymentName, payments.get(paymentName)));

        Preconditions.checkState(!payments.containsValue(payment.getId()),
                String.format("Payment with ID '%s' already known as '%s' instead of '%s'",
                        payment.getId(), payments.inverse().get(payment.getId()), paymentName));

        payments.put(paymentName, payment.getId());
    }

    protected HttpResponse requestToHandlePaymentByLegibleName(final String paymentName) throws IOException {
        Preconditions.checkState(payments.containsKey(paymentName),
                String.format("Legible payment name '%s' not mapped to any payment ID.", paymentName));

        return Request.Get(getHandlePaymentUrl(payments.get(paymentName)))
                .connectTimeout(1000)
                .execute()
                .returnResponse();
    }
}

