package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import io.sphere.sdk.client.SphereClientFactory;
import org.quartz.SchedulerException;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class ServiceFactory {

    public static void main(String [] args) throws SchedulerException {
    }

    public static IntegrationService createService(final ServiceConfig config) {
        final SphereClientFactory factory = SphereClientFactory.of();
        CommercetoolsClient client = new CommercetoolsClient(factory.createClient(
                config.getCtProjectKey(),
                config.getCtClientId(),
                config.getCtClientSecret()));
        return new IntegrationService(new PaymentQueryExecutor(client));
    }
}
