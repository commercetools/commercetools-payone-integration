package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
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
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.types.Type;
import org.quartz.CronScheduleBuilder;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.commercetools.pspadapter.payone.util.PayoneConstants.PAYONE;
import static java.lang.String.format;

/**
 * @author fhaertig
 * @since 02.12.15
 */
public class ServiceFactory {

    private static final long DEFAULT_CTP_CLIENT_TIMEOUT = 10;

    private static final String PAYONE_INTERFACE_NAME = PAYONE;

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFactory.class);

    private final ServiceConfig serviceConfig;

    private final BlockingSphereClient blockingSphereClient;

    private final PaymentService paymentService;

    private final OrderService orderService;

    private final PaymentToOrderStateMapper paymentToOrderStateMapper;

    private ServiceFactory(final ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;

        this.blockingSphereClient = createBlockingSphereClient();
        this.paymentService = createPaymentService();
        this.orderService = createOrderService();
        this.paymentToOrderStateMapper = createPaymentToOrderStateMapper();
    }

    /**
     * Creates a new service factory initialized with the provided {@code propertyProvider}.
     *
     * @param serviceConfig provides the service configuration parameters
     * @return the new service factory instance, never null
     */
    public static ServiceFactory withPropertiesFrom(final ServiceConfig serviceConfig) {
        return new ServiceFactory(serviceConfig);
    }

    public static void main(String[] args) throws SchedulerException, MalformedURLException {

        final PropertyProvider propertyProvider = new PropertyProvider();

        final ServiceConfig serviceConfig = new ServiceConfig(propertyProvider, new PayoneConfig(propertyProvider));

        final ServiceFactory serviceFactory = withPropertiesFrom(serviceConfig);

        final BlockingSphereClient commercetoolsClient = serviceFactory.getBlockingCommercetoolsClient();

        final LoadingCache<String, Type> typeCache = serviceFactory.createTypeCache(commercetoolsClient);
        final PaymentDispatcher paymentDispatcher = createPaymentDispatcher(
                typeCache,
                serviceConfig,
                commercetoolsClient);

        final NotificationDispatcher notificationDispatcher = createNotificationDispatcher(serviceFactory,
                serviceConfig.getPayoneConfig(), serviceConfig);

        final IntegrationService integrationService = createService(
                new CommercetoolsQueryExecutor(commercetoolsClient),
                paymentDispatcher,
                notificationDispatcher,
                new CustomTypeBuilder(
                        commercetoolsClient,
                        CustomTypeBuilder.PermissionToStartFromScratch.fromBoolean(
                                serviceConfig.getStartFromScratch())));

        integrationService.start();

        ScheduledJobFactory scheduledJobFactory = new ScheduledJobFactory();

        scheduledJobFactory.setAllScheduledItemsStartedListener(() -> {
                    LOG.info(format("%n%1$s%nPayone Integration Service is STARTED%n%1$s",
                            "============================================================"));
                }
        );

        scheduledJobFactory.createScheduledJob(
                CronScheduleBuilder.cronSchedule(serviceConfig.getScheduledJobCronForShortTimeFramePoll()),
                ScheduledJobShortTimeframe.class,
                integrationService,
                paymentDispatcher);

        scheduledJobFactory.createScheduledJob(
                CronScheduleBuilder.cronSchedule(serviceConfig.getScheduledJobCronForLongTimeFramePoll()),
                ScheduledJobLongTimeframe.class,
                integrationService,
                paymentDispatcher);
    }

    protected BlockingSphereClient createBlockingSphereClient() {
        return BlockingSphereClient.of(
                SphereClientFactory.of().createClient(serviceConfig.getSphereClientConfig()),
                DEFAULT_CTP_CLIENT_TIMEOUT,
                TimeUnit.SECONDS);
    }


    protected PaymentService createPaymentService() {
        return new PaymentServiceImpl(getBlockingCommercetoolsClient());
    }

    public BlockingSphereClient getBlockingCommercetoolsClient() {
        return blockingSphereClient;
    }

    protected OrderService createOrderService() {
        return new OrderServiceImpl(getBlockingCommercetoolsClient());
    }

    protected PaymentToOrderStateMapper createPaymentToOrderStateMapper() {
        return new DefaultPaymentToOrderStateMapper();
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
        return PAYONE_INTERFACE_NAME;
    }

    // FIXME return shared instance
    public LoadingCache<String, Type> createTypeCache(final BlockingSphereClient client) {
        return CacheBuilder.newBuilder().build(new TypeCacheLoader(client));
    }

    // FIXME get rid of this static method
    private static IntegrationService createService(
            final CommercetoolsQueryExecutor queryExecutor,
            final PaymentDispatcher paymentDispatcher,
            final NotificationDispatcher notificationDispatcher,
            final CustomTypeBuilder customTypeBuilder) {
        return new IntegrationService(customTypeBuilder, queryExecutor, paymentDispatcher, notificationDispatcher, PAYONE);
    }

    public static NotificationDispatcher createNotificationDispatcher(final ServiceFactory serviceFactory,
                                                                      final PayoneConfig config,
                                                                      final ServiceConfig serviceConfig) {
        final NotificationProcessor defaultNotificationProcessor = new DefaultNotificationProcessor(serviceFactory, serviceConfig);

        final ImmutableMap.Builder<NotificationAction, NotificationProcessor> builder = ImmutableMap.builder();
        builder.put(NotificationAction.APPOINTED, new AppointedNotificationProcessor(serviceFactory, serviceConfig));
        builder.put(NotificationAction.CAPTURE, new CaptureNotificationProcessor(serviceFactory, serviceConfig));
        builder.put(NotificationAction.PAID, new PaidNotificationProcessor(serviceFactory, serviceConfig));
        builder.put(NotificationAction.UNDERPAID, new UnderpaidNotificationProcessor(serviceFactory, serviceConfig));

        return new NotificationDispatcher(defaultNotificationProcessor, builder.build(), serviceFactory, config);
    }

    /**
     * TODO transform into instance method
     *
     * @param typeCache
     * @param client
     * @return
     */
    public static PaymentDispatcher createPaymentDispatcher(final LoadingCache<String, Type> typeCache, final ServiceConfig serviceConfig, final BlockingSphereClient client) {
        // TODO jw: use immutable map
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
        return new PaymentDispatcher(methodDispatcherMap);
    }

    private static TransactionExecutor createTransactionExecutor(
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

    private static PayoneRequestFactory createRequestFactory(final PaymentMethod method, final ServiceConfig serviceConfig) {
        switch (method) {
            case CREDIT_CARD:
                return new CreditCardRequestFactory(serviceConfig.getPayoneConfig());
            case WALLET_PAYPAL:
                return new PaypalRequestFactory(serviceConfig.getPayoneConfig());
            case BANK_TRANSFER_SOFORTUEBERWEISUNG:
                return new SofortBankTransferRequestFactory(serviceConfig);
            case BANK_TRANSFER_POSTFINANCE_CARD:
            case BANK_TRANSFER_POSTFINANCE_EFINANCE:
                return new PostFinanceBanktransferRequestFactory(serviceConfig.getPayoneConfig());
            case BANK_TRANSFER_ADVANCE:
                return new BanktTransferInAdvanceRequestFactory(serviceConfig.getPayoneConfig());
            default:
                throw new IllegalArgumentException(format("No PayoneRequestFactory could be created for payment method %s", method));
        }
    }
}
