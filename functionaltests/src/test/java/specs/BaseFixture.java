package specs;

import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.TypeCacheLoader;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.*;
import com.google.common.hash.Hashing;
import com.mashape.unirest.http.Unirest;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.CartDraft;
import io.sphere.sdk.carts.CartDraftBuilder;
import io.sphere.sdk.carts.commands.CartCreateCommand;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.AddLineItem;
import io.sphere.sdk.carts.commands.updateactions.AddPayment;
import io.sphere.sdk.carts.commands.updateactions.SetBillingAddress;
import io.sphere.sdk.carts.commands.updateactions.SetShippingAddress;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.OrderFromCartDraft;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.orders.commands.OrderFromCartCreateCommand;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.queries.PaymentByIdGet;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.utils.MoneyImpl;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;

/**
 * @author fhaertig
 * @since 10.12.15
 */
public abstract class BaseFixture {
    private static final Logger LOG = LoggerFactory.getLogger(BaseFixture.class);
    protected final MonetaryAmountFormat currencyFormatterDe = MonetaryFormats.getAmountFormat(Locale.GERMANY);

    protected static final String EMPTY_STRING = "";
    protected static final String NULL_STRING = "null";
    protected static final long PAYONE_NOTIFICATION_TIMEOUT = TimeUnit.MINUTES.toMillis(10);
    protected static final long RETRY_DELAY = TimeUnit.SECONDS.toMillis(15);
    protected static final long INTERMEDIATE_REPORT_DELAY = TimeUnit.MINUTES.toMillis(3);

    // looks like heroku may have some lags, so we use 10 seconds to avoid false test fails because of timeouts
    protected static final int REQUEST_TIMEOUT = 10000;

    // timeout for simple requests, like health or malformed id, where heavy processing is not expected
    protected static final int SIMPLE_REQUEST_TIMEOUT = 2000;

    protected static final Duration CTP_REQUEST_TIMEOUT = Duration.ofMinutes(2);

    protected static final Splitter thePaymentNamesSplitter = Splitter.on(",").trimResults().omitEmptyStrings();

    protected static final PropertyProvider propertyProvider = new PropertyProvider();

    // Environment variables names
    protected static final String TEST_DATA_CT_PAYONE_INTEGRATION_URL = "TEST_DATA_CT_PAYONE_INTEGRATION_URL";

    protected static final String TEST_DATA_TENANT_NAME = "TEST_DATA_TENANT_NAME";
    protected static final String TEST_DATA_VISA_CREDIT_CARD_NO_3_DS = "TEST_DATA_VISA_CREDIT_CARD_NO_3DS";
    protected static final String TEST_DATA_VISA_CREDIT_CARD_3_DS = "TEST_DATA_VISA_CREDIT_CARD_3DS";
    protected static final String TEST_DATA_3_DS_PASSWORD = "TEST_DATA_3_DS_PASSWORD";
    protected static final String TEST_DATA_SW_BANK_TRANSFER_IBAN = "TEST_DATA_SW_BANK_TRANSFER_IBAN";
    protected static final String TEST_DATA_SW_BANK_TRANSFER_BIC = "TEST_DATA_SW_BANK_TRANSFER_BIC";

    protected static final String TEST_DATA_PAYONE_MERCHANT_ID = "TEST_DATA_PAYONE_MERCHANT_ID";
    protected static final String TEST_DATA_PAYONE_SUBACC_ID = "TEST_DATA_PAYONE_SUBACC_ID";
    protected static final String TEST_DATA_PAYONE_PORTAL_ID = "TEST_DATA_PAYONE_PORTAL_ID";
    protected static final String TEST_DATA_PAYONE_KEY = "TEST_DATA_PAYONE_KEY";

    protected static final String TEST_DATA_CT_PROJECT_KEY = "TEST_DATA_CT_PROJECT_KEY";
    protected static final String TEST_DATA_CT_CLIENT_ID = "TEST_DATA_CT_CLIENT_ID";
    protected static final String TEST_DATA_CT_CLIENT_SECRET = "TEST_DATA_CT_CLIENT_SECRET";

    // other TEST_DATA_ variables see in BaseTenant2Fixture

    protected static final Random randomSource = new Random();

    /**
     * Only for creation of test data
     */
    private static BlockingSphereClient ctpClient;
    private static LoadingCache<String, Type> typeCache;

    private static URL ctPayoneIntegrationBaseUrl;

    /**
     * maps legible payment names to payment IDs (and vice versa)
     */
    private BiMap<String, String> payments = HashBiMap.create();


    @BeforeClass
    static public void initializeCommercetoolsClient() throws MalformedURLException {
        ctPayoneIntegrationBaseUrl =
                new URL(propertyProvider.getMandatoryNonEmptyProperty(TEST_DATA_CT_PAYONE_INTEGRATION_URL));

        ctpClient = BlockingSphereClient.of(
                SphereClientFactory.of().createClient(
                        propertyProvider.getMandatoryNonEmptyProperty(TEST_DATA_CT_PROJECT_KEY),
                        propertyProvider.getMandatoryNonEmptyProperty(TEST_DATA_CT_CLIENT_ID),
                        propertyProvider.getMandatoryNonEmptyProperty(TEST_DATA_CT_CLIENT_SECRET)),
                CTP_REQUEST_TIMEOUT
        );

        typeCache = CacheBuilder.newBuilder().build(new TypeCacheLoader(ctpClient));
    }

    public String getHandlePaymentUrl(final String paymentId) throws MalformedURLException {
        return getServiceUrl(getHandlePaymentPath(paymentId));
    }

    public String getHandlePaymentPath(String paymentId) {
        return format("/%s/commercetools/handle/payments/%s", getTenantName(), paymentId);
    }

    public String getNotificationUrl() throws MalformedURLException {
        return getServiceUrl(getNotificationPath());
    }

    public String getNotificationPath() {
        return format("/%s/payone/notification", getTenantName());
    }

    public String getHealthUrl() throws MalformedURLException {
        return getServiceUrl("/health");
    }

    final String getServiceUrl(String suffix) throws MalformedURLException {
        return new URL(
                ctPayoneIntegrationBaseUrl.getProtocol(),
                ctPayoneIntegrationBaseUrl.getHost(),
                ctPayoneIntegrationBaseUrl.getPort(),
                suffix)
                .toExternalForm();
    }

    public HttpResponse sendGetRequestToUrl(final String url) throws IOException {
        return Request.Get(url)
                .connectTimeout(REQUEST_TIMEOUT)
                .execute()
                .returnResponse();
    }

    protected static String getConfigurationParameter(final String configParameterName) {
        final String envVariable = System.getenv(configParameterName);
        if (!Strings.isNullOrEmpty(envVariable)) {
            return envVariable;
        }

        final String sysProperty = System.getProperty(configParameterName);
        if (!Strings.isNullOrEmpty(sysProperty)) {
            return sysProperty;
        }

        throw new RuntimeException(format(
                "Environment variable required for configuration must not be null or empty: %s",
                configParameterName));
    }

    protected MonetaryAmount createMonetaryAmountFromCent(final Long centAmount, final String currencyCode) {
        return MoneyImpl.ofCents(centAmount, currencyCode);
    }

    protected String createCartAndOrderForPayment(final Payment payment, final String currencyCode) {
        return createCartAndOrderForPayment(payment, currencyCode, "TestBuyer", null);
    }

    protected String createCartAndOrderForPayment(final Payment payment, final String currencyCode, final PaymentState paymentState) {
        return createCartAndOrderForPayment(payment, currencyCode, "TestBuyer", paymentState);
    }

    protected String createCartAndOrderForPayment(final Payment payment, final String currencyCode, final String buyerLastName) {
        return createCartAndOrderForPayment(payment, currencyCode, buyerLastName, null);
    }

    protected String createCartAndOrderForPayment(final Payment payment, final String currencyCode, final String buyerLastName,
                                                  final PaymentState paymentState) {
        Order order = createAndGetOrder(payment, currencyCode, buyerLastName, paymentState);
        return order.getOrderNumber();
    }

    protected Order createAndGetOrder(Payment payment, String currencyCode) {
        return createAndGetOrder(payment, currencyCode, "TestBuyer", null);
    }

    protected Order createAndGetOrder(Payment payment, String currencyCode, PaymentState paymentState) {
        return createAndGetOrder(payment, currencyCode, "TestBuyer", paymentState);
    }

    protected Order createAndGetOrder(Payment payment, String currencyCode, String buyerLastName, PaymentState paymentState) {
        // create cart and order with product
        final Product product = ctpClient().executeBlocking(ProductQuery.of()).getResults().get(0);

        final CartDraft cardDraft = CartDraftBuilder.of(Monetary.getCurrency(currencyCode)).build();

        final Cart cart = ctpClient().executeBlocking(CartUpdateCommand.of(
                ctpClient().executeBlocking(CartCreateCommand.of(cardDraft)),
                ImmutableList.of(
                        AddPayment.of(payment),
                        AddLineItem.of(product.getId(), product.getMasterData().getCurrent().getMasterVariant().getId(), 1),
                        SetShippingAddress.of(Address.of(CountryCode.DE)),
                        SetBillingAddress.of(Address.of(CountryCode.DE).withLastName(buyerLastName))
                )));

        final String orderNumber = getRandomOrderNumber();

        return ctpClient().executeBlocking(OrderFromCartCreateCommand.of(
                OrderFromCartDraft.of(cart, orderNumber, paymentState)));
    }

    private static String PSEUDO_CARD_PAN;

    private static String PSEUDO_CARD_PAN_3DS;

    /**
     * Fetch new or get cached pseudocardpan from Payone service based on supplied test VISA card number
     * (see {@link #getTestDataVisaCreditCardNo3Ds()}).
     * <p>
     * Why <i>synchronized</i>: the card pan is fetched over HTTP POST request from pay1.de site and the operation
     * might be slow, so we cache the value. But our tests run concurrently, so to avoid double HTTP request
     * to pay1.de we use synchronized: the access to the method is blocked till the response from pay1.de, but it is
     * blocked once, all other get access will be fast (when {@code PSEUDO_CARD_PAN} is set).
     *
     * @see #getVerifiedVisaPseudoCardPan()
     */
    synchronized protected String getUnconfirmedVisaPseudoCardPan() {
      if (PSEUDO_CARD_PAN == null) {
          String fetchedPseudoCardPan = fetchPseudoCardPan(getTestDataVisaCreditCardNo3Ds(),
                                                            getTestDataPayoneMerchantId(),
                                                            getTestDataPayoneSubaccId(),
                                                            getTestDataPayonePortalId(),
                                                            getTestDataPayoneKey());
          // TODO: remove the assert and logging if the future if everything goes fine
          assert PSEUDO_CARD_PAN == null : "PSEUDO_CARD_PAN multiple initialization";
          PSEUDO_CARD_PAN = fetchedPseudoCardPan;
          LOG.info("Unconfirmed pseudocardpan fetched successfully");
      }

      return PSEUDO_CARD_PAN;
    }

    /**
     * Fetch new or get cached pseudocardpan from Payone service based on supplied test VISA with 3DS check card number
     * (see {@link #getTestVisaCreditCard3Ds()}).
     * <p>
     *
     * @see #getUnconfirmedVisaPseudoCardPan()
     */
    synchronized protected String getVerifiedVisaPseudoCardPan() {
        if (PSEUDO_CARD_PAN_3DS == null) {
            String fetchedPseudoCardPan = fetchPseudoCardPan(getTestVisaCreditCard3Ds(),
                                                            getTestDataPayoneMerchantId(),
                                                            getTestDataPayoneSubaccId(),
                                                            getTestDataPayonePortalId(),
                                                            getTestDataPayoneKey());
            // TODO: remove the assert and logging if the future if everything goes fine
            assert PSEUDO_CARD_PAN_3DS == null : "PSEUDO_CARD_PAN_3DS multiple initialization";
            PSEUDO_CARD_PAN_3DS = fetchedPseudoCardPan;
            LOG.info("3DS pseudocardpan fetched successfully");
        }

        return PSEUDO_CARD_PAN_3DS;
    }

    /**
     * Request a new pseudocardpan for {@code cardPan} Visa number.
     * <p>
     * <b>Note:</b> Pseudocardpan is a unique number for every merchant ID, thus ensure you tested service and these
     * integration tests use the same payone merchant credentials, like {@link #getTestDataPayoneMerchantId()}.
     * Verify your {@code PAYONE_MERCHANT_ID} and {@code TEST_DATA_PAYONE_MERCHANT_ID} for both applications if
     * you get "pseudocardpan is not found" response from Payone.
     *
     * @param cardPan Visa card number (expected to be test Visa number from Payone)
     * @return pseudocardpan string, registered in Payone merchant center
     * @throws RuntimeException if the response from Payone can't be parsed
     */
    protected String fetchPseudoCardPan(String cardPan, String mid, String aid, String pid, String key) {
        //curl --data "request=3dscheck&mid=$PAYONE_MERCHANT_ID&aid=$PAYONE_SUBACC_ID&portalid=$PAYONE_PORTAL_ID&key=$(md5 -qs $PAYONE_KEY)&mode=test&api_version=3.9&amount=2&currency=EUR&clearingtype=cc&exiturl=http://www.example.com&storecarddata=yes&cardexpiredate=2512&cardcvc2=123&cardtype=V&cardpan=<VISA_CREDIT_CARD_3DS_NUMBER>"

        String cardPanResponse = null;
        try {
            cardPanResponse = Unirest.post("https://api.pay1.de/post-gateway/")
              .fields(ImmutableMap.<String, Object>builder()
                  .put("request", "3dscheck")
                  .put("mid", mid)
                  .put("aid", aid)
                  .put("portalid", pid)
                  .put("key", Hashing.md5().hashString(key, Charsets.UTF_8).toString())
                  .put("mode", "test")
                  .put("api_version", "3.9")
                  .put("amount", "2")
                  .put("currency", "EUR")
                  .put("clearingtype", "cc")
                  .put("exiturl", "http://www.example.com")
                  .put("storecarddata", "yes")
                  .put("cardexpiredate", "2512")
                  .put("cardcvc2", "123")
                  .put("cardtype", "V")
                  .put("cardpan", cardPan)
                  .build())
              .asString().getBody();
        } catch (Throwable e) {
          throw new RuntimeException("Error on pseudocardpan fetch", e);
        }

        Pattern p = Pattern.compile("^.*pseudocardpan\\s*=\\s*(\\d+).*$", CASE_INSENSITIVE | DOTALL);
        Matcher m = p.matcher(cardPanResponse);

        if (!m.matches()) {
          throw new RuntimeException(format("Unexpected pseudocardpan response: %s", cardPanResponse));
        }

        return m.group(1);
    }

    public String getTenantName() {
        return getConfigurationParameter(TEST_DATA_TENANT_NAME);
    }

    protected static String getTestDataVisaCreditCardNo3Ds() {
        return getConfigurationParameter(TEST_DATA_VISA_CREDIT_CARD_NO_3_DS);
    }

    protected static String getTestVisaCreditCard3Ds() {
        return getConfigurationParameter(TEST_DATA_VISA_CREDIT_CARD_3_DS);
    }

    protected static String getTestData3DsPassword() {
        return getConfigurationParameter(TEST_DATA_3_DS_PASSWORD);
    }

    protected static String getTestDataSwBankTransferIban() {
        return getConfigurationParameter(TEST_DATA_SW_BANK_TRANSFER_IBAN);
    }

    protected static String getTestDataSwBankTransferBic() {
        return getConfigurationParameter(TEST_DATA_SW_BANK_TRANSFER_BIC);
    }

    protected String getTestDataPayoneMerchantId() {
        return getConfigurationParameter(TEST_DATA_PAYONE_MERCHANT_ID);
    }

    protected String getTestDataPayoneSubaccId() {
        return getConfigurationParameter(TEST_DATA_PAYONE_SUBACC_ID);
    }

    public   String getTestDataPayonePortalId() {
        return getConfigurationParameter(TEST_DATA_PAYONE_PORTAL_ID);
    }

    protected String getTestDataPayoneKey() {
        return getConfigurationParameter(TEST_DATA_PAYONE_KEY);
    }

    protected static String getRandomOrderNumber() {
        return String.valueOf(randomSource.nextInt() + System.currentTimeMillis());
    }

    protected BlockingSphereClient ctpClient() {
        return ctpClient;
    }

    public LoadingCache<String, Type> getTypeCache() {
        return typeCache;
    }

    /**
     * Gets the latest version of the payment via its legible name - a name that exits only in this test context.
     * @param paymentName legible name of the payment
     * @return the payment fetched from the commercetools client, can be null!
     * @see #fetchPaymentById(String)
     */
    protected Payment fetchPaymentByLegibleName(final String paymentName) {
        return fetchPaymentById(payments.get(paymentName));
    }

    /**
     * Gets the latest version of the payment via its ID.
     * @param paymentId unique ID of the payment
     * @return the payment fetched from the commercetools client, can be null!
     * @see #fetchPaymentByLegibleName(String)
     */
    protected Payment fetchPaymentById(final String paymentId) {
        return ctpClient().executeBlocking(PaymentByIdGet.of(
                Preconditions.checkNotNull(paymentId, "paymentId must not be null!")));
    }

    /**
     * Gets the ID of the type with name (aka key) {@code typeName}.
     * @param typeName the name (key) of a type
     * @return the type's ID
     * @throws ExecutionException if a checked exception was thrown while loading the value.
     */
    protected String typeIdFromTypeName(final String typeName) throws ExecutionException {
        return getTypeCache().get(typeName).getId();
    }

    public String getTransactionStateByPaymentId(final String paymentId, final String transactionId) {
        return getTransactionState(fetchPaymentById(paymentId), transactionId);
    }

    protected String getTransactionState(final Payment payment, final String transactionId) {
        final Optional<Transaction> transaction = payment.getTransactions()
                .stream()
                .filter(i -> transactionId.equals(i.getId()))
                .findFirst();

        final TransactionState transactionState = transaction.isPresent() ? transaction.get().getState() : null;
        return transactionState != null ? transactionState.toSphereName() : "<UNKNOWN>";
    }

    public String getIdOfLastTransactionByPaymentId(final String paymentId) {
        return getIdOfLastTransaction(fetchPaymentById(paymentId));
    }

    protected String getIdOfLastTransaction(final Payment payment) {
        Preconditions.checkNotNull(payment,
                "payment is null. This could be due to a restarting service instance which has not finished cleaning the platform from custom types, payments, orders and carts!");
        return Iterables.getLast(payment.getTransactions()).getId();
    }

    protected String getIdOfFirstTransaction(final Payment payment) {
        return Objects.requireNonNull(payment,
                "payment is null. This could be due to a restarting service instance which has not finished cleaning the platform from custom types, payments, orders and carts!")
                .getTransactions().get(0).getId();
    }

    protected void registerPaymentWithLegibleName(final String paymentName, final Payment payment) {
        Preconditions.checkState(!payments.containsKey(paymentName),
                format("Legible payment name '%s' already in use for ID '%s'.",
                        paymentName, payments.get(paymentName)));

        Preconditions.checkState(!payments.containsValue(payment.getId()),
                format("Payment with ID '%s' already known as '%s' instead of '%s'",
                        payment.getId(), payments.inverse().get(payment.getId()), paymentName));

        payments.put(paymentName, payment.getId());
    }

    protected HttpResponse requestToHandlePaymentByLegibleName(final String paymentName) throws IOException {
        Preconditions.checkState(payments.containsKey(paymentName),
                format("Legible payment name '%s' not mapped to any payment ID.", paymentName));

        return Request.Get(getHandlePaymentUrl(payments.get(paymentName)))
                .connectTimeout(REQUEST_TIMEOUT)
                .execute()
                .returnResponse();
    }

    protected String getIdForLegibleName(final String paymentName) {
        return payments.get(paymentName);
    }


    protected String getInteractionRequestCount(final Payment payment,
                                                final String transactionId,
                                                final String requestType) throws ExecutionException {
        final String interactionTypeId = typeIdFromTypeName(CustomTypeBuilder.PAYONE_INTERACTION_REQUEST);
        return Long.toString(payment.getInterfaceInteractions().stream()
                .filter(i -> interactionTypeId.equals(i.getType().getId()))
                .filter(i -> transactionId.equals(i.getFieldAsString(CustomFieldKeys.TRANSACTION_ID_FIELD)))
                .filter(i -> {
                    final String requestField = i.getFieldAsString(CustomFieldKeys.REQUEST_FIELD);
                    return (requestField != null) && requestField.contains("request=" + requestType);
                })
                .count());
    }

    protected String getInteractionRequestCountOverAllTransactions(final Payment payment,
                                                                   final String requestType) throws ExecutionException {
        final String interactionTypeId = typeIdFromTypeName(CustomTypeBuilder.PAYONE_INTERACTION_REQUEST);
        return Long.toString(payment.getInterfaceInteractions().stream()
                .filter(i -> interactionTypeId.equals(i.getType().getId()))
                .filter(i -> {
                    final String requestField = i.getFieldAsString(CustomFieldKeys.REQUEST_FIELD);
                    return (requestField != null) && requestField.contains("request=" + requestType);
                })
                .count());
    }

    protected Optional<CustomFields> getInteractionRedirect(final Payment payment,
                                                            final String transactionId) throws ExecutionException {
        final String interactionTypeId = typeIdFromTypeName(CustomTypeBuilder.PAYONE_INTERACTION_REDIRECT);
        return payment.getInterfaceInteractions().stream()
                .filter(i -> interactionTypeId.equals(i.getType().getId()))
                .filter(i -> transactionId.equals(i.getFieldAsString(CustomFieldKeys.TRANSACTION_ID_FIELD)))
                .filter(i -> {
                    final String redirectField = i.getFieldAsString(CustomFieldKeys.REDIRECT_URL_FIELD);
                    return redirectField != null && !redirectField.isEmpty();
                })
                .findFirst();

    }

    protected long countPaymentsWithNotificationOfAction(final List<String> paymentNames, final String txaction) {
        final List<RuntimeException> exceptions = Lists.newArrayList();
        final long result = paymentNames.stream().mapToLong(paymentName -> {
            final Payment payment = fetchPaymentByLegibleName(paymentName);
            try {
                return getTotalNotificationCountOfAction(payment, txaction);
            } catch (final ExecutionException e) {
                LOG.error("Exception: %s", e);
                exceptions.add(new RuntimeException(e));
                return 0L;
            }
        }).filter(notifications -> notifications > 0L).count();

        if (!exceptions.isEmpty()) {
            throw exceptions.get(0);
        }

        return result;
    }

    protected long getTotalNotificationCountOfAction(final Payment payment, final String txaction) throws ExecutionException {
        final String interactionTypeId = typeIdFromTypeName(CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION);

        return payment.getInterfaceInteractions().stream()
                .filter(i -> interactionTypeId.equals(i.getType().getId()))
                .filter(i -> txaction.equals(i.getFieldAsString(CustomFieldKeys.TX_ACTION_FIELD)))
                .filter(i -> {
                    final String notificationField = i.getFieldAsString(CustomFieldKeys.NOTIFICATION_FIELD);
                    return (notificationField != null) &&
                            (notificationField.toLowerCase().contains("transactionstatus=completed")
                                    || notificationField.toLowerCase().contains("transactionstatus=null"));
                })
                .count();
    }

    public boolean customStringFieldIsNull(final String paymentName, final String fieldName) {
        final Payment payment = fetchPaymentByLegibleName(paymentName);
        return payment.getCustom().getFieldAsString(fieldName) == null;
    }
}

