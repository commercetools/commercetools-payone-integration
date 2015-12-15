package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.TypeCacheLoader;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.MethodKeys;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethodDispatcher;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.types.Type;
import org.quartz.CronScheduleBuilder;
import org.quartz.SchedulerException;

import java.net.MalformedURLException;
import java.util.HashMap;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class ServiceFactory {

    private static final String SCHEDULED_JOB_KEY = "commercetools-platform-polling-1";

    public static void main(String [] args) throws SchedulerException, MalformedURLException {
        final ServiceConfig serviceConfig = new ServiceConfig();
        final CommercetoolsClient commercetoolsClient = ServiceFactory.createCommercetoolsClient(serviceConfig);
        final Cache<String, Type> typeCache = ServiceFactory.createTypeCache(commercetoolsClient);
        final PaymentDispatcher paymentDispatcher = ServiceFactory.createPaymentDispatcher(typeCache);
        final CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(serviceConfig.getCronNotation());

        final IntegrationService integrationService = ServiceFactory.createService(
                new CommercetoolsQueryExecutor(commercetoolsClient),
                paymentDispatcher,
                new CustomTypeBuilder(commercetoolsClient));

        integrationService.start();

        ScheduledJobFactory.createScheduledJob(
                cronScheduleBuilder,
                integrationService,
                SCHEDULED_JOB_KEY,
                paymentDispatcher);
    }

    public static IntegrationService createService(final ServiceConfig config) {
        final CommercetoolsClient client = ServiceFactory.createCommercetoolsClient(config);
        final Cache<String, Type> typeCache = ServiceFactory.createTypeCache(client);

        return ServiceFactory.createService(
                new CommercetoolsQueryExecutor(client),
                createPaymentDispatcher(typeCache),
                new CustomTypeBuilder(client));
    }

    private static Cache<String, Type> createTypeCache(final CommercetoolsClient client) {
        return CacheBuilder.newBuilder().build(new TypeCacheLoader(client));
    }

    public static PaymentDispatcher createPaymentDispatcher(final Cache<String, Type> typeCache) {
        final PaymentMethodDispatcher creditCardDispatcher = createPaymentMethodDispatcher(typeCache);
        final PaymentMethodDispatcher sepaDispatcher = createPaymentMethodDispatcher(typeCache);

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

    private static IntegrationService createService(
            final CommercetoolsQueryExecutor queryExecutor,
            final PaymentDispatcher paymentDispatcher,
            final CustomTypeBuilder customTypeBuilder) {
        return new IntegrationService(queryExecutor, paymentDispatcher, customTypeBuilder);
    }

    private static PaymentMethodDispatcher createPaymentMethodDispatcher(final Cache<String, Type> typeCache) {
        return new PaymentMethodDispatcher((payment, transaction) -> payment, new HashMap<>());
    }
}
