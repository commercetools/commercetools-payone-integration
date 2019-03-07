package com.commercetools.pspadapter.tenant;

import com.commercetools.payments.TransactionStateResolver;
import com.commercetools.payments.TransactionStateResolverImpl;
import com.commercetools.pspadapter.payone.PaymentDispatcher;
import com.commercetools.pspadapter.payone.PaymentHandler;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.TypeCacheLoader;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostServiceImpl;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.mapping.*;
import com.commercetools.pspadapter.payone.mapping.klarna.KlarnaRequestFactory;
import com.commercetools.pspadapter.payone.mapping.klarna.PayoneKlarnaCountryToLanguageMapper;
import com.commercetools.pspadapter.payone.mapping.order.DefaultPaymentToOrderStateMapper;
import com.commercetools.pspadapter.payone.mapping.order.PaymentToOrderStateMapper;
import com.commercetools.pspadapter.payone.notification.NotificationDispatcher;
import com.commercetools.pspadapter.payone.notification.NotificationProcessor;
import com.commercetools.pspadapter.payone.notification.common.*;
import com.commercetools.pspadapter.payone.transaction.PaymentMethodDispatcher;
import com.commercetools.pspadapter.payone.transaction.TransactionExecutor;
import com.commercetools.pspadapter.payone.transaction.common.AuthorizationTransactionExecutor;
import com.commercetools.pspadapter.payone.transaction.common.ChargeTransactionExecutor;
import com.commercetools.pspadapter.payone.transaction.common.UnsupportedTransactionExecutor;
import com.commercetools.pspadapter.payone.transaction.paymentinadvance.BankTransferInAdvanceAuthorizationTransactionExecutor;
import com.commercetools.pspadapter.payone.transaction.paymentinadvance.BankTransferInAdvanceChargeTransactionExecutor;
import com.commercetools.service.OrderService;
import com.commercetools.service.OrderServiceImpl;
import com.commercetools.service.PaymentService;
import com.commercetools.service.PaymentServiceImpl;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.HashMap;

import static com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod.supportedPaymentMethods;
import static com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod.supportedTransactionTypes;
import static java.lang.String.format;

public class TenantFactory {

    private static final Duration DEFAULT_CTP_CLIENT_TIMEOUT = Duration.ofSeconds(10);

    private final String payoneInterfaceName;

    private final String tenantName;

    private final String urlPrefix;

    private final BlockingSphereClient blockingSphereClient;

    private final PayonePostService payonePostService;

    private final PaymentHandler paymentHandler;
    private final PaymentDispatcher paymentDispatcher;
    private final NotificationDispatcher notificationDispatcher;

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PaymentToOrderStateMapper paymentToOrderStateMapper;

    private final CustomTypeBuilder customTypeBuilder;
    private final CommercetoolsQueryExecutor commercetoolsQueryExecutor;

    private final TransactionStateResolver transactionStateResolver;


    public TenantFactory(String payoneInterfaceName, TenantConfig tenantConfig) {
        this.payoneInterfaceName = payoneInterfaceName;

        this.tenantName = tenantConfig.getName();

        this.urlPrefix = "/" + tenantName;

        this.paymentToOrderStateMapper = createPaymentToOrderStateMapper();

        this.blockingSphereClient = createBlockingSphereClient(tenantConfig);
        this.payonePostService = getPayonePostService(tenantConfig);

        this.paymentService = createPaymentService(blockingSphereClient);
        this.orderService = createOrderService(blockingSphereClient);

        this.transactionStateResolver = createTransactionStateResolver();

        this.paymentDispatcher = createPaymentDispatcher(tenantConfig, createTypeCache(blockingSphereClient),
                blockingSphereClient, payonePostService, transactionStateResolver);

        this.notificationDispatcher = createNotificationDispatcher(tenantConfig, transactionStateResolver);

        this.commercetoolsQueryExecutor = new CommercetoolsQueryExecutor(blockingSphereClient);

        this.paymentHandler = createPaymentHandler(payoneInterfaceName, tenantConfig.getName(), commercetoolsQueryExecutor, paymentDispatcher);

        this.customTypeBuilder = createCustomTypeBuilder(blockingSphereClient, tenantConfig.getStartFromScratch());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public getters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public String getTenantName() {
        return tenantName;
    }

    public String getPayoneInterfaceName() {
        return payoneInterfaceName;
    }

    public String getPaymentHandlerUrl() {
        return urlPrefix + "/commercetools/handle/payments/:id";
    }

    public String getPayoneNotificationUrl() {
        return urlPrefix + "/payone/notification";
    }

    public PaymentService getPaymentService() {
        return paymentService;
    }

    public OrderService getOrderService() {
        return orderService;
    }

    public PaymentToOrderStateMapper getPaymentToOrderStateMapper() {
        return paymentToOrderStateMapper;
    }


    public PaymentHandler getPaymentHandler() {
        return paymentHandler;
    }

    public PaymentDispatcher getPaymentDispatcher() {
        return paymentDispatcher;
    }

    public NotificationDispatcher getNotificationDispatcher() {
        return notificationDispatcher;
    }

    public CommercetoolsQueryExecutor getCommercetoolsQueryExecutor() {
        return commercetoolsQueryExecutor;
    }

    public CustomTypeBuilder getCustomTypeBuilder() {
        return customTypeBuilder;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Encapsulated Factory Creators
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Nonnull
    protected BlockingSphereClient createBlockingSphereClient(TenantConfig tenantConfig) {
        return BlockingSphereClient.of(
                SphereClientFactory.of().createClient(tenantConfig.getSphereClientConfig()),
                DEFAULT_CTP_CLIENT_TIMEOUT);
    }
    public BlockingSphereClient getBlockingSphereClient() {
        return blockingSphereClient;
    }

    @Nonnull
    protected PayonePostService getPayonePostService(TenantConfig tenantConfig) {
        return PayonePostServiceImpl.of(tenantConfig.getPayoneConfig().getApiUrl());
    }



    protected PaymentService createPaymentService(SphereClient sphereClient) {
        return new PaymentServiceImpl(sphereClient);
    }

    protected OrderService createOrderService(SphereClient sphereClient) {
        return new OrderServiceImpl(sphereClient);
    }

    protected PaymentToOrderStateMapper createPaymentToOrderStateMapper() {
        return new DefaultPaymentToOrderStateMapper();
    }

    protected NotificationDispatcher createNotificationDispatcher(TenantConfig tenantConfig,
                                                                  TransactionStateResolver transactionStateResolver) {
        final NotificationProcessor defaultNotificationProcessor =
                new DefaultNotificationProcessor(this, tenantConfig, transactionStateResolver);

        final ImmutableMap.Builder<NotificationAction, NotificationProcessor> builder = ImmutableMap.builder();
        builder.put(NotificationAction.APPOINTED, new AppointedNotificationProcessor(this, tenantConfig, transactionStateResolver));
        builder.put(NotificationAction.CAPTURE, new CaptureNotificationProcessor(this, tenantConfig, transactionStateResolver));
        builder.put(NotificationAction.PAID, new PaidNotificationProcessor(this, tenantConfig, transactionStateResolver));
        builder.put(NotificationAction.UNDERPAID, new UnderpaidNotificationProcessor(this, tenantConfig, transactionStateResolver));

        return new NotificationDispatcher(defaultNotificationProcessor, builder.build(), this, tenantConfig.getPayoneConfig());
    }

    protected PaymentHandler createPaymentHandler(String payoneInterfaceName, String tenantName,
                                                  CommercetoolsQueryExecutor commercetoolsQueryExecutor,
                                                  PaymentDispatcher paymentDispatcher) {
        return new PaymentHandler(payoneInterfaceName, tenantName, commercetoolsQueryExecutor, paymentDispatcher);
    }

    protected PaymentDispatcher createPaymentDispatcher(final TenantConfig tenantConfig,
                                                        final LoadingCache<String, Type> typeCache,
                                                        final BlockingSphereClient client,
                                                        final PayonePostService payonePostService,
                                                        final TransactionStateResolver transactionStateResolver) {

        final HashMap<PaymentMethod, PaymentMethodDispatcher> methodDispatcherMap = new HashMap<>();

        final TransactionExecutor defaultExecutor = new UnsupportedTransactionExecutor(client);

        for (final PaymentMethod paymentMethod : supportedPaymentMethods) {
            final PayoneRequestFactory requestFactory = createRequestFactory(paymentMethod, tenantConfig);
            final ImmutableMap.Builder<TransactionType, TransactionExecutor> executors = ImmutableMap.builder();

            supportedTransactionTypes
                    .forEach(transactionType ->
                            executors.put(transactionType,
                                    createTransactionExecutor(transactionType, typeCache, client, requestFactory, payonePostService, paymentMethod)));

            methodDispatcherMap.put(paymentMethod,
                    new PaymentMethodDispatcher(defaultExecutor, executors.build(), transactionStateResolver));
        }
        return new PaymentDispatcher(methodDispatcherMap, getPayoneInterfaceName());
    }

    protected TransactionExecutor createTransactionExecutor(
            final TransactionType transactionType,
            final LoadingCache<String, Type> typeCache,
            final BlockingSphereClient client,
            final PayoneRequestFactory requestFactory,
            final PayonePostService postService,
            final PaymentMethod paymentMethod) {

        switch (transactionType) {
            case AUTHORIZATION:
                switch (paymentMethod) {
                    case BANK_TRANSFER_ADVANCE:
                        return new BankTransferInAdvanceAuthorizationTransactionExecutor(typeCache, requestFactory, postService, client);
                    default:
                        return new AuthorizationTransactionExecutor(typeCache, requestFactory, postService, client);
                }
            case CHARGE:
                switch (paymentMethod) {
                    case BANK_TRANSFER_ADVANCE:
                        return new BankTransferInAdvanceChargeTransactionExecutor(typeCache, requestFactory, postService, client);
                    default:
                        return new ChargeTransactionExecutor(typeCache, requestFactory, postService, client);
                }
        }
        throw new IllegalArgumentException(format("Transaction type \"%s\" is not supported", transactionType));
    }

    /**
     * Get a new instance of {@link PayoneRequestFactory} based on payment method and tenant config.
     *
     * @param method       one of {@link PaymentMethod} instances.
     * @param tenantConfig tenant specific configuration.
     * @return new instance of {@link PayoneRequestFactory}.
     */
    @Nonnull
    protected PayoneRequestFactory createRequestFactory(@Nonnull PaymentMethod method, @Nonnull TenantConfig tenantConfig) {
        switch (method) {
            case CREDIT_CARD:
                return new CreditCardRequestFactory(tenantConfig);

            case WALLET_PAYPAL:
            case WALLET_PAYDIREKT:
                return new WalletRequestFactory(tenantConfig);

            case BANK_TRANSFER_SOFORTUEBERWEISUNG:
                return new SofortBankTransferRequestFactory(tenantConfig);

            case BANK_TRANSFER_POSTFINANCE_CARD:
            case BANK_TRANSFER_POSTFINANCE_EFINANCE:
                return new PostFinanceBanktransferRequestFactory(tenantConfig);

            case BANK_TRANSFER_ADVANCE:
                return new BankTransferInAdvanceRequestFactory(tenantConfig);
            case INVOICE_KLARNA:
                return new KlarnaRequestFactory(tenantConfig, createCountryToLanguageMapper());

            default:
                throw new IllegalArgumentException(format("No PayoneRequestFactory could be created for payment method %s", method));
        }
    }

    protected CustomTypeBuilder createCustomTypeBuilder(BlockingSphereClient blockingSphereClient, boolean startFromScratch) {
        return new CustomTypeBuilder(blockingSphereClient,
                CustomTypeBuilder.PermissionToStartFromScratch.fromBoolean(startFromScratch));
    }

    protected LoadingCache<String, Type> createTypeCache(final BlockingSphereClient client) {
        return CacheBuilder.newBuilder().build(new TypeCacheLoader(client));
    }

    @Nonnull
    protected CountryToLanguageMapper createCountryToLanguageMapper() {
        return new PayoneKlarnaCountryToLanguageMapper();
    }

    @Nonnull
    protected TransactionStateResolver createTransactionStateResolver() {
        return new TransactionStateResolverImpl();
    }
}
