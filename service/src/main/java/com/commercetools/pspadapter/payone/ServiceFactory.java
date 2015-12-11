package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.paymentmethods.sepa.SepaDispatcher;
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
        return new IntegrationService(new CommercetoolsQueryExecutor(client), createPaymentDispatcher());
    }

    public static PaymentDispatcher createPaymentDispatcher() {
        return new PaymentDispatcher(
            new SepaDispatcher((payment, transaction) -> payment, new HashMap<>()));
    }
}
