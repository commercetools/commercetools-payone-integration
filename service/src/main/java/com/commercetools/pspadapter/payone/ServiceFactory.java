package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.TypeCacheLoader;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethodDispatcher;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.TransactionExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.UnsupportedTransactionExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.creditcard.AuthorizationTransactionExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.creditcard.ChargeTransactionExecutor;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostServiceImpl;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.mapping.CreditCardRequestFactory;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.types.Type;
import org.quartz.CronScheduleBuilder;
import org.quartz.SchedulerException;
import spark.Response;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Optional;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class ServiceFactory {

    private static final String SCHEDULED_JOB_KEY = "commercetools-platform-polling-1";

    private final ServiceConfig config;

    private ServiceFactory(final ServiceConfig config) {
        this.config = config;
    }

    /**
     * Creates a new service factory initialized with a default {@link ServiceConfig}.
     * @return the new factory instance, never null
     */
    public static ServiceFactory createWithDefaultServiceConfig() {
        return ServiceFactory.createWithServiceConfig(new ServiceConfig(new PropertyProvider()));
    }

    /**
     * Creates a new service factory initialized with the provided {@code config}.
     * @param config the service configuration
     * @return the new service factory instance, never null
     */
    public static ServiceFactory createWithServiceConfig(final ServiceConfig config) {
        return new ServiceFactory(config);
    }

    public static void main(String [] args) throws SchedulerException, MalformedURLException {
        final ServiceConfig serviceConfig = new ServiceConfig(new PropertyProvider());
        final ServiceFactory serviceFactory = ServiceFactory.createWithServiceConfig(serviceConfig);
        final CommercetoolsClient commercetoolsClient = serviceFactory.createCommercetoolsClient();
        final LoadingCache<String, Type> typeCache = serviceFactory.createTypeCache(commercetoolsClient);
        final PaymentDispatcher paymentDispatcher = ServiceFactory.createPaymentDispatcher(
                typeCache,
                serviceConfig.getPayoneConfig(),
                commercetoolsClient);
        final NotificationDispatcher notificationDispatcher = ServiceFactory.createNotificationDispatcher(
                commercetoolsClient);

        final IntegrationService integrationService = ServiceFactory.createService(
                new CommercetoolsQueryExecutor(commercetoolsClient),
                paymentDispatcher,
                notificationDispatcher,
                new CustomTypeBuilder(
                        commercetoolsClient,
                        CustomTypeBuilder.PermissionToStartFromScratch.fromBoolean(
                                serviceConfig.getStartFromScratch())));

        integrationService.start();

        ScheduledJobFactory.createScheduledJob(
                CronScheduleBuilder.cronSchedule(serviceConfig.getCronNotation()),
                integrationService,
                SCHEDULED_JOB_KEY,
                paymentDispatcher);
    }

    public IntegrationService createService() {
        final CommercetoolsClient client = createCommercetoolsClient();
        final LoadingCache<String, Type> typeCache = createTypeCache(client);

        return ServiceFactory.createService(
                new CommercetoolsQueryExecutor(client),
                createPaymentDispatcher(typeCache, config.getPayoneConfig(), client),
                createNotificationDispatcher(client),
                new CustomTypeBuilder(
                        client,
                        CustomTypeBuilder.PermissionToStartFromScratch.fromBoolean(config.getStartFromScratch())));
    }


    private static IntegrationService createService(
            final CommercetoolsQueryExecutor queryExecutor,
            final PaymentDispatcher paymentDispatcher,
            final NotificationDispatcher notificationDispatcher,
            final CustomTypeBuilder customTypeBuilder) {
        // TODO jw: use actual result processor
        return new IntegrationService(customTypeBuilder, queryExecutor, paymentDispatcher, notificationDispatcher, new ResultProcessor() {
            @Override
            public void process(final PaymentWithCartLike paymentWithCartLike, final Response response) {
                response.status(200);
            }
        });
    }

    public LoadingCache<String, Type> createTypeCache(final BlockingClient client) {
        return CacheBuilder.newBuilder().build(new TypeCacheLoader(client));
    }

    public static NotificationDispatcher createNotificationDispatcher(final CommercetoolsClient client) {
        //TODO fh: use actual NotificationProcessor implementation
        NotificationProcessor defaultNotificationProcessor = (notification, payment) -> false;

        final ImmutableMap<NotificationAction, NotificationProcessor> notificationProcessorMap = ImmutableMap.of();
        return new NotificationDispatcher(defaultNotificationProcessor, notificationProcessorMap, client);
    }

    public static PaymentDispatcher createPaymentDispatcher(final LoadingCache<String, Type> typeCache, final PayoneConfig config, final CommercetoolsClient client) {
        // TODO jw: use immutable map
        final HashMap<PaymentMethod, PaymentMethodDispatcher> methodDispatcherMap = new HashMap<>();

        final TransactionExecutor defaultExecutor = new UnsupportedTransactionExecutor(client);
        final PayonePostServiceImpl postService = PayonePostServiceImpl.of(config.getApiUrl());

        final ImmutableSet<PaymentMethod> supportedMethods = ImmutableSet.of(
                PaymentMethod.CREDIT_CARD
        );

        for (final PaymentMethod paymentMethod : supportedMethods) {
            final PayoneRequestFactory requestFactory = createRequestFactory(paymentMethod, config);
            final ImmutableMap.Builder<TransactionType, TransactionExecutor> executors = ImmutableMap.builder();
            for (final TransactionType type : paymentMethod.getSupportedTransactionTypes()) {
                // FIXME jw: shouldn't be nullable anymore when payment method is implemented completely
                final TransactionExecutor executor = Optional
                            .ofNullable(createTransactionExecutor(type, typeCache, client, requestFactory, postService))
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
            final CommercetoolsClient client,
            final PayoneRequestFactory requestFactory,
            final PayonePostService postService) {

        switch(transactionType) {
            case AUTHORIZATION:
                return new AuthorizationTransactionExecutor(typeCache, requestFactory, postService, client);
            case CANCEL_AUTHORIZATION:
                break;
            case CHARGE:
                return new ChargeTransactionExecutor(typeCache, requestFactory, postService, client);
            case REFUND:
                break;
            case CHARGEBACK:
                break;
        }
        return null;
    }

    public CommercetoolsClient createCommercetoolsClient() {
        final SphereClientFactory sphereClientFactory = SphereClientFactory.of();
        return new CommercetoolsClient(
                sphereClientFactory.createClient(
                        config.getCtProjectKey(),
                        config.getCtClientId(),
                        config.getCtClientSecret()));
    }

    private static PayoneRequestFactory createRequestFactory(final PaymentMethod method, final PayoneConfig config) {
        switch(method) {
            case CREDIT_CARD:
                return new CreditCardRequestFactory(config);
            default:
                throw new IllegalArgumentException(String.format("No PayoneRequestFactory could be created for payment method %s", method));
        }
    }
}
