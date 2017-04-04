package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.TypeCacheLoader;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostServiceImpl;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.mapping.*;
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

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * @author fhaertig
 * @since 02.12.15
 */
public class ServiceFactory {

    private static final long DEFAULT_CTP_CLIENT_TIMEOUT = 10;

    private final ServiceConfig serviceConfig;
    private final BlockingSphereClient blockingSphereClient;
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PaymentDispatcher paymentDispatcher;
    private final NotificationDispatcher notificationDispatcher;
    private final CustomTypeBuilder customTypeBuilder;
    private final CommercetoolsQueryExecutor commercetoolsQueryExecutor;
    private final PaymentToOrderStateMapper paymentToOrderStateMapper;
    private final String payoneInterfaceName;

    public ServiceFactory(final ServiceConfig serviceConfig, String payoneInterfaceName) {
        this.payoneInterfaceName = payoneInterfaceName;
        this.serviceConfig = serviceConfig;
        this.paymentToOrderStateMapper = createPaymentToOrderStateMapper();

        this.blockingSphereClient = createBlockingSphereClient(serviceConfig);

        this.paymentService = createPaymentService(blockingSphereClient);
        this.orderService = createOrderService(blockingSphereClient);
        this.paymentDispatcher = createPaymentDispatcher(serviceConfig, createTypeCache(blockingSphereClient), blockingSphereClient);
        this.notificationDispatcher = createNotificationDispatcher(serviceConfig);
        this.commercetoolsQueryExecutor = new CommercetoolsQueryExecutor(blockingSphereClient);
        this.customTypeBuilder = createCustomTypeBuilder(blockingSphereClient, serviceConfig.getStartFromScratch());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public getters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public BlockingSphereClient getBlockingCommercetoolsClient() {
        return blockingSphereClient;
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

    public String getPayoneInterfaceName() {
        return payoneInterfaceName;
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

    protected BlockingSphereClient createBlockingSphereClient(ServiceConfig serviceConfig) {
        return BlockingSphereClient.of(
                SphereClientFactory.of().createClient(serviceConfig.getSphereClientConfig()),
                DEFAULT_CTP_CLIENT_TIMEOUT,
                TimeUnit.SECONDS);
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

    protected LoadingCache<String, Type> createTypeCache(final BlockingSphereClient client) {
        return CacheBuilder.newBuilder().build(new TypeCacheLoader(client));
    }

    protected NotificationDispatcher createNotificationDispatcher(ServiceConfig serviceConfig) {
        final NotificationProcessor defaultNotificationProcessor = new DefaultNotificationProcessor(this, serviceConfig);

        final ImmutableMap.Builder<NotificationAction, NotificationProcessor> builder = ImmutableMap.builder();
        builder.put(NotificationAction.APPOINTED, new AppointedNotificationProcessor(this, serviceConfig));
        builder.put(NotificationAction.CAPTURE, new CaptureNotificationProcessor(this, serviceConfig));
        builder.put(NotificationAction.PAID, new PaidNotificationProcessor(this, serviceConfig));
        builder.put(NotificationAction.UNDERPAID, new UnderpaidNotificationProcessor(this, serviceConfig));

        return new NotificationDispatcher(defaultNotificationProcessor, builder.build(), this, serviceConfig.getPayoneConfig());
    }

    protected PaymentDispatcher createPaymentDispatcher(final ServiceConfig serviceConfig,
                                                        final LoadingCache<String, Type> typeCache,
                                                        final BlockingSphereClient client) {

        final HashMap<PaymentMethod, PaymentMethodDispatcher> methodDispatcherMap = new HashMap<>();

        final TransactionExecutor defaultExecutor = new UnsupportedTransactionExecutor(client);
        final PayonePostServiceImpl postService = PayonePostServiceImpl.of(serviceConfig.getPayoneConfig().getApiUrl());

        final ImmutableSet<PaymentMethod> supportedMethods = ImmutableSet.of(
                PaymentMethod.CREDIT_CARD,
                PaymentMethod.WALLET_PAYPAL,
                PaymentMethod.BANK_TRANSFER_SOFORTUEBERWEISUNG,
                PaymentMethod.BANK_TRANSFER_ADVANCE,
                PaymentMethod.BANK_TRANSFER_POSTFINANCE_CARD,
                PaymentMethod.BANK_TRANSFER_POSTFINANCE_EFINANCE
        );

        for (final PaymentMethod paymentMethod : supportedMethods) {
            final PayoneRequestFactory requestFactory = createRequestFactory(paymentMethod, serviceConfig);
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

    protected PayoneRequestFactory createRequestFactory(final PaymentMethod method, final ServiceConfig serviceConfig) {
        switch (method) {
            case CREDIT_CARD:
                return new CreditCardRequestFactory(serviceConfig.getPayoneConfig());
            case WALLET_PAYPAL:
                return new PaypalRequestFactory(serviceConfig.getPayoneConfig());
            case BANK_TRANSFER_SOFORTUEBERWEISUNG:
                return new SofortBankTransferRequestFactory(serviceConfig.getPayoneConfig(), serviceConfig.getSecureKey());
            case BANK_TRANSFER_POSTFINANCE_CARD:
            case BANK_TRANSFER_POSTFINANCE_EFINANCE:
                return new PostFinanceBanktransferRequestFactory(serviceConfig.getPayoneConfig());
            case BANK_TRANSFER_ADVANCE:
                return new BanktTransferInAdvanceRequestFactory(serviceConfig.getPayoneConfig());
            default:
                throw new IllegalArgumentException(format("No PayoneRequestFactory could be created for payment method %s", method));
        }
    }

    protected CustomTypeBuilder createCustomTypeBuilder(BlockingSphereClient blockingSphereClient, boolean startFromScratch) {
        return new CustomTypeBuilder(blockingSphereClient,
                CustomTypeBuilder.PermissionToStartFromScratch.fromBoolean(startFromScratch));
    }
}
