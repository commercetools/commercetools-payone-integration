package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.MethodKeys;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.sepa.PaymentMethodDispatcher;
import io.sphere.sdk.client.SphereClientFactory;
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

        return new IntegrationService(
                new CommercetoolsQueryExecutor(client),
                createPaymentDispatcher(),
                new CustomTypeBuilder(client));
    }

    public static PaymentDispatcher createPaymentDispatcher() {
        final PaymentMethodDispatcher creditCardDispatcher = createPaymentMethodDispatcher();
        final PaymentMethodDispatcher sepaDispatcher = createPaymentMethodDispatcher();

        final HashMap<String, PaymentMethodDispatcher> methodDispatcherMap = new HashMap<>();
        methodDispatcherMap.put(MethodKeys.CREDIT_CARD, creditCardDispatcher);
        methodDispatcherMap.put(MethodKeys.DIRECT_DEBIT_SEPA, sepaDispatcher);

        return new PaymentDispatcher(methodDispatcherMap);
    }

    public static PaymentMethodDispatcher createPaymentMethodDispatcher() {
        return new PaymentMethodDispatcher((payment, transaction) -> payment, new HashMap<>());
    }
}
