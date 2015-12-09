package com.commercetools.pspadapter.payone.domain.ctp;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;

import java.util.concurrent.CompletionStage;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class CommercetoolsClient {

    private SphereClient sphereClient;

    public CommercetoolsClient(final SphereClient client) {
        this.sphereClient = client;
    }

    public <T> CompletionStage<T> execute(SphereRequest<T> sphereRequest) {
        return sphereClient.execute(sphereRequest);
    }

}
