package com.commercetools.pspadapter.payone.domain.ctp;

import com.commercetools.pspadapter.payone.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import io.sphere.sdk.client.SphereClientFactory;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class CommercetoolsClientFactory {

    public static CommercetoolsClient createClient(final ServiceConfig config) {
       return createClient(config.getCtProjectKey(), config.getCtClientId(), config.getCtClientSecret());
    }

    public static CommercetoolsClient createClient(
            final String projectKey,
            final String clientId,
            final String clientSecret) {
        SphereClientFactory factory = SphereClientFactory.of();
        return new CommercetoolsClient(factory.createClient(projectKey, clientId, clientSecret));
    }
}
