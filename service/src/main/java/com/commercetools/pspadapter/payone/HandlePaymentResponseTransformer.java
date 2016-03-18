package com.commercetools.pspadapter.payone;

import spark.ResponseTransformer;

/**
 * Is able to transform the result content of a handle payment request
 * to a valid http response body.
 * Currently the API is defined to return an empty body in any case.
 *
 * @author fhaertig
 * @since 18.03.16
 *
 */
public class HandlePaymentResponseTransformer implements ResponseTransformer {


    public HandlePaymentResponseTransformer() {
    }

    @Override
    public String render(final Object model) throws Exception {
        return "";
    }
}
