package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.TypeCacheLoader;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.MethodKeys;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethodDispatcher;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.TransactionExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.creditcard.AuthorizationTransactionExecutor;
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
        final PayonePostServiceImpl postService = PayonePostServiceImpl.of(config.getApiUrl());

        final PaymentMethodDispatcher creditCardDispatcher = createPaymentMethodDispatcher(typeCache, MethodKeys.CREDIT_CARD, postService, client, config);
        final PaymentMethodDispatcher sepaDispatcher = createPaymentMethodDispatcher(typeCache, MethodKeys.DIRECT_DEBIT_SEPA, postService, client, config);

        final HashMap<String, PaymentMethodDispatcher> methodDispatcherMap = new HashMap<>();
        methodDispatcherMap.put(MethodKeys.CREDIT_CARD, creditCardDispatcher);
        methodDispatcherMap.put(MethodKeys.DIRECT_DEBIT_SEPA, sepaDispatcher);

        return new PaymentDispatcher(methodDispatcherMap);
    }

    private static CommercetoolsClient createCommercetoolsClient(final ServiceConfig config) {
        final SphereClientFactory sphereClientFactory = SphereClientFactory.of();
        return new CommercetoolsClient(
                sphereClientFactory.createClient(
                        config.getCtProjectKey(),
                        config.getCtClientId(),
                        config.getCtClientSecret()));
    }

    private static PaymentMethodDispatcher createPaymentMethodDispatcher(
            final LoadingCache<String, Type> typeCache,
            final String methodKey,
            final PayonePostService postService,
            final CommercetoolsClient ctpClient,
            final PayoneConfig config) {

        PayoneRequestFactory requestFactory = createPayoneRequestFactory(methodKey, config);

        Map<TransactionType, TransactionExecutor> executors = new HashMap<>();
        executors.put(TransactionType.AUTHORIZATION, new AuthorizationTransactionExecutor(typeCache, requestFactory, postService, ctpClient));

        return new PaymentMethodDispatcher((payment, transaction) -> payment, executors);
    }

    private static PayoneRequestFactory createPayoneRequestFactory(final String methodKey, final PayoneConfig config) {
        if (MethodKeys.CREDIT_CARD.equals(methodKey)) {
            return new CreditCardRequestFactory(config);
        }
        return null;
    }
}
