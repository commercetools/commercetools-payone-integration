package com.commercetools.pspadapter.payone.domain.ctp;

import com.commercetools.pspadapter.payone.ServiceConfig;
import io.sphere.sdk.client.SphereClientFactory;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class CommercetoolsClientFactory {

    public static CommercetoolsClient createClient(final ServiceConfig config) {
        final SphereClientFactory factory = SphereClientFactory.of();
        return new CommercetoolsClient(factory.createClient(
                config.getCtProjectKey(),
                config.getCtClientId(),
                config.getCtClientSecret()));
    }

}
