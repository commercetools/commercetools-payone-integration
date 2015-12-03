package com.commercetools.pspadapter.payone;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class IntegrationService {

    private final PaymentQueryExecutor paymentQueryExecutor;

    IntegrationService(final PaymentQueryExecutor paymentQueryExecutor) {
        this.paymentQueryExecutor = paymentQueryExecutor;
    }
}
