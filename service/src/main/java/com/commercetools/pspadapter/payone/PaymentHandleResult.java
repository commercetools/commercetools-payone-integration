package com.commercetools.pspadapter.payone;

/**
 * Holds the result of a request to handle a payment.
 *
 * @author Jan Wolter
 */
public class PaymentHandleResult {
    private final int statusCode;

    private final String body;

    /**
     * Initializes a new instance.
     * @param statusCode the server status code
     * @param body the response body
     */
    public PaymentHandleResult(final int statusCode, final String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    /**
     * Initializes a new instance with an empty response body.
     * @param statusCode the server status code
     */
    public PaymentHandleResult(final int statusCode) {
        this(statusCode, "");
    }

    /**
     * Gets the server status code; see RFC 2068.
     *
     * @return the server status code
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Gets the body as string.
     *
     * @return the body
     */
    public String body() {
        return body;
    }
}
