package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.TypeCacheLoader;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.MethodKeys;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethodDispatcher;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.types.Type;
import org.quartz.SchedulerException;

import java.util.HashMap;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class ServiceFactory {

    public static void main(String [] args) throws SchedulerException {
    }

    public static IntegrationService createService(final ServiceConfig config) {
        final SphereClientFactory sphereClientFactory = SphereClientFactory.of();
        CommercetoolsClient client = new CommercetoolsClient(
                sphereClientFactory.createClient(
                    config.getCtProjectKey(),
                    config.getCtClientId(),
                    config.getCtClientSecret()));

        final Cache<String, Type> typeCache = CacheBuilder.newBuilder()
            .build(new TypeCacheLoader(client));

        return new IntegrationService(
                new CommercetoolsQueryExecutor(client),
                createPaymentDispatcher(typeCache),
                new CustomTypeBuilder(client));
    }

    public static PaymentDispatcher createPaymentDispatcher(final Cache<String, Type> typeCache) {
        final PaymentMethodDispatcher creditCardDispatcher = createPaymentMethodDispatcher(typeCache);
        final PaymentMethodDispatcher sepaDispatcher = createPaymentMethodDispatcher(typeCache);

        final HashMap<String, PaymentMethodDispatcher> methodDispatcherMap = new HashMap<>();
        methodDispatcherMap.put(MethodKeys.CREDIT_CARD, creditCardDispatcher);
        methodDispatcherMap.put(MethodKeys.DIRECT_DEBIT_SEPA, sepaDispatcher);

        return new PaymentDispatcher(methodDispatcherMap);
    }

    private static PaymentMethodDispatcher createPaymentMethodDispatcher(final Cache<String, Type> typeCache) {
        return new PaymentMethodDispatcher((payment, transaction) -> payment, new HashMap<>());
    }
}
