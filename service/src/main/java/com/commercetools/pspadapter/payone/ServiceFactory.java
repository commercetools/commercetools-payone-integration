package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.TypeCacheLoader;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethodDispatcher;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.TransactionExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.UnsupportedTransactionExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.creditcard.AuthorizationTransactionExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.creditcard.ChargeTransactionExecutor;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostServiceImpl;
import com.commercetools.pspadapter.payone.mapping.CreditCardRequestFactory;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.types.Type;
import org.quartz.CronScheduleBuilder;
import org.quartz.SchedulerException;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class ServiceFactory {

    private static final String SCHEDULED_JOB_KEY = "commercetools-platform-polling-1";

    public static void main(String [] args) throws SchedulerException, MalformedURLException {
        final ServiceConfig serviceConfig = new ServiceConfig();
        final CommercetoolsClient commercetoolsClient = ServiceFactory.createCommercetoolsClient(serviceConfig);
        final LoadingCache<String, Type> typeCache = ServiceFactory.createTypeCache(commercetoolsClient);
        final PaymentDispatcher paymentDispatcher = ServiceFactory.createPaymentDispatcher(
                typeCache,
                serviceConfig.getPayoneConfig(),
                commercetoolsClient);

        final IntegrationService integrationService = ServiceFactory.createService(
                new CommercetoolsQueryExecutor(commercetoolsClient),
                paymentDispatcher,
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

    public static IntegrationService createService(final ServiceConfig config) {
        final CommercetoolsClient client = ServiceFactory.createCommercetoolsClient(config);
        final LoadingCache<String, Type> typeCache = ServiceFactory.createTypeCache(client);

        return ServiceFactory.createService(
                new CommercetoolsQueryExecutor(client),
                createPaymentDispatcher(typeCache, config.getPayoneConfig(), client),
                new CustomTypeBuilder(
                        client,
                        CustomTypeBuilder.PermissionToStartFromScratch.fromBoolean(config.getStartFromScratch())));
    }

    private static IntegrationService createService(
            final CommercetoolsQueryExecutor queryExecutor,
            final PaymentDispatcher paymentDispatcher,
            final CustomTypeBuilder customTypeBuilder) {
        return new IntegrationService(queryExecutor, paymentDispatcher, customTypeBuilder);
    }

    private static LoadingCache<String, Type> createTypeCache(final CommercetoolsClient client) {
        return CacheBuilder.newBuilder().build(new TypeCacheLoader(client));
    }

    public static PaymentDispatcher createPaymentDispatcher(final LoadingCache<String, Type> typeCache, final PayoneConfig config, final CommercetoolsClient client) {
        final HashMap<PaymentMethod, PaymentMethodDispatcher> methodDispatcherMap = new HashMap<>();

        TransactionExecutor defaultExecutor = new UnsupportedTransactionExecutor(client);
        final PayonePostServiceImpl postService = PayonePostServiceImpl.of(config.getApiUrl());

        methodDispatcherMap.put(PaymentMethod.CREDIT_CARD, null);

        for (PaymentMethod method : methodDispatcherMap.keySet()) {
            PayoneRequestFactory requestFactory = Optional.ofNullable(createRequestFactory(method, config))
                    .orElseThrow(() -> new IllegalArgumentException("No PayoneRequestFactory could be created for payment method " + method));
            Map<TransactionType, TransactionExecutor> executorsMap = new HashMap<>();
            for (TransactionType type : method.getSupportedTransactionTypes()) {
                Optional
                    .ofNullable(createTransactionExecutor(type, typeCache, client, requestFactory, postService))
                    .ifPresent(executor -> executorsMap.put(type, executor));
            }
            methodDispatcherMap.put(method, new PaymentMethodDispatcher(defaultExecutor, executorsMap));
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

    private static CommercetoolsClient createCommercetoolsClient(final ServiceConfig config) {
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
                return null;
        }
    }
}
