package util;

import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.queries.PaymentQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.queries.TypeQuery;
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
        final InputStream dummyPaymentJson = getJsonFromFile(filePath);
        return SphereJsonUtils.readObject(IOUtils.toString(dummyPaymentJson), Order.typeReference());
    }

    public PagedQueryResult<Payment> getPaymentQueryResultFromFile(String filePath) throws IOException {
        final InputStream dummyPaymentJson = getJsonFromFile(filePath);
        return SphereJsonUtils.readObject(IOUtils.toString(dummyPaymentJson), PaymentQuery.resultTypeReference());
    }

    private PagedQueryResult<Type> getTypeQueryResultFromFile(String filePath) throws IOException {
        final InputStream dummyPaymentJson = getJsonFromFile(filePath);
        return SphereJsonUtils.readObject(IOUtils.toString(dummyPaymentJson), TypeQuery.resultTypeReference());
    }

    public PagedQueryResult<Type> getCustomTypes() throws Exception {
        return getTypeQueryResultFromFile("customTypes.json");
    }

    public Payment dummyPaymentOneAuthPending20Euro() throws Exception {
        return getPaymentFromFile("dummyPaymentOneAuthPending20Euro.json");
    }

    public Payment dummyPaymentOneAuthPending20EuroPendingResponse() throws Exception {
        return getPaymentFromFile("dummyPaymentOneAuthPending20EuroPendingResponse.json");
    }

    public Payment dummyPaymentOneAuthPending20EuroRedirectResponse() throws Exception {
        return getPaymentFromFile("dummyPaymentOneAuthPending20EuroRedirectResponse.json");
    }

    public Payment dummyPaymentOneChargePending20Euro() throws Exception {
        return getPaymentFromFile("dummyPaymentOneChargePending20Euro.json");
    }

    public Payment dummyPaymentAuthSuccess() throws Exception {
        return getPaymentFromFile("dummyPaymentAuthSuccess.json");
    }

    public Payment dummyPaymentNoCustomFields() throws Exception {
        return getPaymentFromFile("dummyPaymentNoCustomFields.json");
    }

    public Order dummyOrderMapToPayoneRequest() throws Exception {
        return getOrderFromFile("dummyOrderMapToPayoneRequest.json");
    }

    public Payment dummyPaymentTwoTransactionsPending() throws Exception {
        return getPaymentFromFile("dummyPaymentTwoTransactionsPending.json");
    }

    public Payment dummyPaymentTwoTransactionsSuccessPending() throws Exception {
        return getPaymentFromFile("dummyPaymentTwoTransactionsSuccessPending.json");
    }

    public Payment dummyPaymentCreatedByNotification() throws Exception {
        return getPaymentFromFile("dummyPaymentCreatedByNotification.json");
    }

    public Payment dummyPaymentNoInterface() throws Exception {
        return getPaymentFromFile("dummyPaymentNoInterface.json");
    }

    public Payment dummyPaymentWrongInterface() throws Exception {
        return getPaymentFromFile("dummyPaymentWrongInterface.json");
    }

    public Payment dummyPaymentUnknownMethod() throws Exception {
        return getPaymentFromFile("dummyPaymentUnknownMethod.json");
    }
}
