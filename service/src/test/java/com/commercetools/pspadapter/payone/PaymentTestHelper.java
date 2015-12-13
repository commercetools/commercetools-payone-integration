package com.commercetools.pspadapter.payone;

import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.payments.Payment;
import spark.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class PaymentTestHelper {
    protected static InputStream getJsonFromFile(String filePath) throws IOException {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);
    }

    protected Payment dummyPayment1() throws Exception {
        InputStream dummyPaymentJson = getJsonFromFile("dummyPaymentTwoTransactionsPending.json");
        return SphereJsonUtils.readObject(IOUtils.toString(dummyPaymentJson), Payment.typeReference());
    }

    protected Payment dummyPayment2() throws Exception {
        InputStream dummyPaymentJson = getJsonFromFile("dummyPaymentTwoTransactionsSuccessPending.json");
        return SphereJsonUtils.readObject(IOUtils.toString(dummyPaymentJson), Payment.typeReference());
    }

    protected Payment dummyPaymentWrongInterface() throws Exception {
        InputStream dummyPaymentJson = getJsonFromFile("dummyPaymentWrongInterface.json");
        return SphereJsonUtils.readObject(IOUtils.toString(dummyPaymentJson), Payment.typeReference());
    }
}
