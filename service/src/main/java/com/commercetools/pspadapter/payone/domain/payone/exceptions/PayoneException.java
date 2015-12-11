package com.commercetools.pspadapter.payone.domain.payone.exceptions;

public class PayoneException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new payment exception.
     *
     * @param message
     *            the message
     * @param cause
     *            the cause
     */
    public PayoneException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new payment exception.
     *
     * @param message
     *            the message
     */
    public PayoneException(final String message) {
        super(message);
    }

    /**
     * Instantiates a new payment exception.
     *
     * @param resultCode
     *            the result code
     * @param message
     *            the message
     */
    public PayoneException(final int resultCode, final String message) {
        super(String.format("[%d] %s", resultCode, message));
    }

    /**
     * Instantiates a new payment exception.
     *
     * @param cause
     *            the cause
     */
    public PayoneException(final Throwable cause) {
        super(cause);
    }

}