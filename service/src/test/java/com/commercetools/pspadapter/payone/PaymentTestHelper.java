package com.commercetools.pspadapter.payone;

import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.payments.Payment;
import spark.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class PaymentTestHelper {
    protected static InputStream getJsonFromFile(String filePath) throws IOException {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);
    }

    private Payment getPaymentFromFile(String filePath) throws IOException {
        InputStream dummyPaymentJson = getJsonFromFile(filePath);
        return SphereJsonUtils.readObject(IOUtils.toString(dummyPaymentJson), Payment.typeReference());
    }

    private Order getOrderFromFile(String filePath) throws IOException {
        InputStream dummyPaymentJson = getJsonFromFile(filePath);
        return SphereJsonUtils.readObject(IOUtils.toString(dummyPaymentJson), Order.typeReference());
    }

    protected Payment dummyPaymentMapToPayoneRequest() throws Exception {
        return getPaymentFromFile("dummyPaymentMapToPayoneRequest.json");
    }

    protected Payment dummyPaymentNoCustomFields() throws Exception {
        return getPaymentFromFile("dummyPaymentNoCustomFields.json");
    }

    protected Order dummyOrderMapToPayoneRequest() throws Exception {
        return getOrderFromFile("dummyOrderMapToPayoneRequest.json");
    }

    protected Payment dummyPaymentTwoTransactionsPending() throws Exception {
        return getPaymentFromFile("dummyPaymentTwoTransactionsPending.json");
    }

    protected Payment dummyPaymentTwoTransactionsSuccessPending() throws Exception {
        return getPaymentFromFile("dummyPaymentTwoTransactionsSuccessPending.json");
    }

    protected Payment dummyPaymentNoInterface() throws Exception {
        return getPaymentFromFile("dummyPaymentNoInterface.json");
    }

    protected Payment dummyPaymentWrongInterface() throws Exception {
        return getPaymentFromFile("dummyPaymentWrongInterface.json");
    }

    protected Payment dummyPaymentUnknownMethod() throws Exception {
        return getPaymentFromFile("dummyPaymentUnknownMethod.json");
    }
}
