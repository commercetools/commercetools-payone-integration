package util;

import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientFactory;

import java.util.function.Function;

/**
 * @author fhaertig
 * @date 07.12.15
 */
public class SphereClientDoubleCreator {

    public static SphereClient getSphereClientWithPaymentRoute(final Function<HttpRequestIntent, Object> responseFunction) {
        return SphereClientFactory.createObjectTestDouble(httpRequest -> {
            final Object result;
            if (httpRequest.getPath().startsWith("/payments?where=")) {
                result = responseFunction.apply(httpRequest);
            } else {
                //here you can put if else blocks for further preconfigured responses
                throw new UnsupportedOperationException("I'm not prepared for this request: " + httpRequest);
            }
            return result;
        });
    }
}
