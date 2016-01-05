package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import spark.Response;

/**
 * A ResultProcessor knows which response to create for the result of a payment dispatch.
 * @author Jan Wolter
 */
public interface ResultProcessor {
    /**
     * Fills a response for a payment dispatch result.
     * @param paymentWithCartLike the result of a dispatched payment
     * @param response the response to be filled with the result of a dispatched payment
     */
    default void process(final PaymentWithCartLike paymentWithCartLike, final Response response) {
        response.status(500);
        response.type("text/plain; charset=utf-8");
        response.body("Sorry, but you hit us between the eyes.");
    }
}
