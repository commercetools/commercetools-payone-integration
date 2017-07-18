package com.commercetools.pspadapter.tenant;

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
import com.commercetools.pspadapter.payone.transaction.common.DefaultChargeTransactionExecutor;
import com.commercetools.pspadapter.payone.transaction.common.UnsupportedTransactionExecutor;
import com.commercetools.pspadapter.payone.transaction.creditcard.AuthorizationTransactionExecutor;
import com.commercetools.pspadapter.payone.transaction.paymentinadvance.BankTransferInAdvanceChargeTransactionExecutor;
import com.commercetools.service.OrderService;
import com.commercetools.service.OrderServiceImpl;
import com.commercetools.service.PaymentService;
import com.commercetools.service.PaymentServiceImpl;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;

import static java.lang.String.format;

public class TenantFactory {

    private static final Duration DEFAULT_CTP_CLIENT_TIMEOUT = Duration.ofSeconds(10);

    private final String payoneInterfaceName;

    private final String tenantName;

    private final String urlPrefix;

    private final BlockingSphereClient blockingSphereClient;

    private final PaymentHandler paymentHandler;
    private final PaymentDispatcher paymentDispatcher;
    private final NotificationDispatcher notificationDispatcher;

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PaymentToOrderStateMapper paymentToOrderStateMapper;

    private final CustomTypeBuilder customTypeBuilder;
    private final CommercetoolsQueryExecutor commercetoolsQueryExecutor;


    public TenantFactory(String payoneInterfaceName, TenantConfig tenantConfig) {
        this.payoneInterfaceName = payoneInterfaceName;

        this.tenantName = tenantConfig.getName();

        this.urlPrefix = "/" + tenantName;

        this.paymentToOrderStateMapper = createPaymentToOrderStateMapper();

        this.blockingSphereClient = createBlockingSphereClient(tenantConfig);

        this.paymentService = createPaymentService(blockingSphereClient);
        this.orderService = createOrderService(blockingSphereClient);

        this.paymentDispatcher = createPaymentDispatcher(tenantConfig, createTypeCache(blockingSphereClient), blockingSphereClient);

        this.notificationDispatcher = createNotificationDispatcher(tenantConfig);

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

    protected BlockingSphereClient createBlockingSphereClient(TenantConfig tenantConfig) {
        return BlockingSphereClient.of(
                SphereClientFactory.of().createClient(tenantConfig.getSphereClientConfig()),
                DEFAULT_CTP_CLIENT_TIMEOUT);
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

    protected NotificationDispatcher createNotificationDispatcher(TenantConfig tenantConfig) {
        final NotificationProcessor defaultNotificationProcessor = new DefaultNotificationProcessor(this, tenantConfig);

        final ImmutableMap.Builder<NotificationAction, NotificationProcessor> builder = ImmutableMap.builder();
        builder.put(NotificationAction.APPOINTED, new AppointedNotificationProcessor(this, tenantConfig));
        builder.put(NotificationAction.CAPTURE, new CaptureNotificationProcessor(this, tenantConfig));
        builder.put(NotificationAction.PAID, new PaidNotificationProcessor(this, tenantConfig));
        builder.put(NotificationAction.UNDERPAID, new UnderpaidNotificationProcessor(this, tenantConfig));

        return new NotificationDispatcher(defaultNotificationProcessor, builder.build(), this, tenantConfig.getPayoneConfig());
    }

    protected PaymentHandler createPaymentHandler(String payoneInterfaceName, String tenantName,
                                                  CommercetoolsQueryExecutor commercetoolsQueryExecutor,
                                                  PaymentDispatcher paymentDispatcher) {
        return new PaymentHandler(payoneInterfaceName, tenantName, commercetoolsQueryExecutor, paymentDispatcher);
    }

    protected PaymentDispatcher createPaymentDispatcher(final TenantConfig tenantConfig,
                                                        final LoadingCache<String, Type> typeCache,
                                                        final BlockingSphereClient client) {

        final HashMap<PaymentMethod, PaymentMethodDispatcher> methodDispatcherMap = new HashMap<>();

        final TransactionExecutor defaultExecutor = new UnsupportedTransactionExecutor(client);
        final PayonePostServiceImpl postService = PayonePostServiceImpl.of(tenantConfig.getPayoneConfig().getApiUrl());

        final ImmutableSet<PaymentMethod> supportedMethods = ImmutableSet.of(
                PaymentMethod.CREDIT_CARD,
                PaymentMethod.WALLET_PAYPAL,
                PaymentMethod.BANK_TRANSFER_SOFORTUEBERWEISUNG,
                PaymentMethod.BANK_TRANSFER_ADVANCE,
                PaymentMethod.BANK_TRANSFER_POSTFINANCE_CARD,
                PaymentMethod.BANK_TRANSFER_POSTFINANCE_EFINANCE,
                PaymentMethod.INVOICE_KLARNA
        );

        for (final PaymentMethod paymentMethod : supportedMethods) {
            final PayoneRequestFactory requestFactory = createRequestFactory(paymentMethod, tenantConfig);
            final ImmutableMap.Builder<TransactionType, TransactionExecutor> executors = ImmutableMap.builder();
            for (final TransactionType type : paymentMethod.getSupportedTransactionTypes()) {
                // FIXME jw: shouldn't be nullable anymore when payment method is implemented completely
                final TransactionExecutor executor = Optional
                        .ofNullable(createTransactionExecutor(type, typeCache, client, requestFactory, postService, paymentMethod))
                        .orElse(defaultExecutor);

                executors.put(type, executor);
            }
            methodDispatcherMap.put(paymentMethod, new PaymentMethodDispatcher(defaultExecutor, executors.build()));
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
                return new AuthorizationTransactionExecutor(typeCache, requestFactory, postService, client);
            case CANCEL_AUTHORIZATION:
                break;
            case CHARGE:
                switch (paymentMethod) {
                    case BANK_TRANSFER_ADVANCE:
                        return new BankTransferInAdvanceChargeTransactionExecutor(typeCache, requestFactory, postService, client);
                    default:
                        return new DefaultChargeTransactionExecutor(typeCache, requestFactory, postService, client);
                }
            case REFUND:
                break;
            case CHARGEBACK:
                break;
        }
        return null;
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
                return new PaypalRequestFactory(tenantConfig);
            case BANK_TRANSFER_SOFORTUEBERWEISUNG:
                return new SofortBankTransferRequestFactory(tenantConfig);
            case BANK_TRANSFER_POSTFINANCE_CARD:
            case BANK_TRANSFER_POSTFINANCE_EFINANCE:
                return new PostFinanceBanktransferRequestFactory(tenantConfig);
            case BANK_TRANSFER_ADVANCE:
                return new BanktTransferInAdvanceRequestFactory(tenantConfig);
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
}
