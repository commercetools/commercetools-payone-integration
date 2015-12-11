package com.commercetools.pspadapter.payone.domain.payone.exceptions;


public class PayoneConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new configuration exception
     * @param message - the message
     */
    public PayoneConfigurationException(final String message) {
        super(message);
    }

    /**
     * Instantiates a new configuration exception
     * @param message - the message
     * @param cause - the cause
     */
    public PayoneConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new configuration exception
     * @param cause - the cause
     */
    public PayoneConfigurationException(final Throwable cause) {
        super(cause);
    }

}
