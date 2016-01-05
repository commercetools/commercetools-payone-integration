package com.commercetools.pspadapter.payone;

import java.util.List;

/**
 * @author fhaertig
 * @date 16.12.15
 */
public class DispatchResult {

    private int statusCode;

    private String message;

    private List<String> errors;

    public static DispatchResult of(final int statusCode, final String message) {
        DispatchResult result = new DispatchResult();
        result.statusCode = statusCode;
        result.message = message;

        return result;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(final int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(final List<String> errors) {
        this.errors = errors;
    }
}
