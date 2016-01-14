package com.commercetools.pspadapter.payone.domain.ctp.exceptions;

/**
 * @author fhaertig
 * @date 14.01.16
 */
public class NoCartLikeFoundException extends RuntimeException {

    public NoCartLikeFoundException() {
        super("No Order or Cart found for the payment");
    }
}
