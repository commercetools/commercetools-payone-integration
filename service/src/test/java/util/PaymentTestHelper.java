package util;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import io.sphere.sdk.carts.Cart;
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

    public static final String KLARNA_DIR = "com/commercetools/pspadapter/payone/mapping/klarna/";

    protected static InputStream getJsonFromFile(String filePath) throws IOException {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);
    }

    private Payment getPaymentFromFile(String filePath) throws IOException {
        InputStream dummyPaymentJson = getJsonFromFile(filePath);
        return SphereJsonUtils.readObject(IOUtils.toString(dummyPaymentJson), Payment.typeReference());
    }

    private Order getOrderFromFile(String filePath) throws IOException {
        final InputStream dummyOrderJson = getJsonFromFile(filePath);
        return SphereJsonUtils.readObject(IOUtils.toString(dummyOrderJson), Order.typeReference());
    }

    private Cart getCartFromFile(String filePath) throws IOException {
        final InputStream dummyCartJson = getJsonFromFile(filePath);
        return SphereJsonUtils.readObject(IOUtils.toString(dummyCartJson), Cart.typeReference());
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

    public Payment dummyPaymentNoTransaction20EuroPlanned() throws Exception {
        return getPaymentFromFile("dummyPaymentNoTransaction20EuroPlanned.json");
    }

    public Payment dummyPaymentOneAuthFailure20EuroCC() throws Exception {
        return getPaymentFromFile("dummyPaymentOneAuthFailure20Euro_CC.json");
    }

    public Payment dummyPaymentOneAuthInitial20EuroCC() throws Exception {
        return getPaymentFromFile("dummyPaymentOneAuthInitial20Euro_CC.json");
    }

    public Payment dummyPaymentOneAuthPending20EuroCC() throws Exception {
        return getPaymentFromFile("dummyPaymentOneAuthPending20Euro_CC.json");
    }

    public Payment dummyPaymentForKlarnaCart_KLV() throws Exception {
        return getPaymentFromFile(KLARNA_DIR + "dummyPaymentForKlarnaCart_KLV.json");
    }

    public Payment dummyPaymentForKlarnaCartWithoutDiscounts_KLV() throws Exception {
        return getPaymentFromFile(KLARNA_DIR + "dummyPaymentForKlarnaCartWithoutDiscounts_KLV.json");
    }

    public Payment dummyPaymentOneAuthPending20EuroPPE() throws Exception {
        return getPaymentFromFile("dummyPaymentOneAuthPending20Euro_PPE.json");
    }

    public Payment dummyPaymentOneAuthPending20EuroPDT() throws Exception {
        return getPaymentFromFile("dummyPaymentOneAuthPending20Euro_PDT.json");
    }

    public Payment dummyPaymentOneAuthPending20EuroBCT() throws IOException {
        return getPaymentFromFile("dummyPaymentOneAuthPending20Euro_BCT.json");
    }

    public Payment dummyPaymentOneAuthPending20EuroPNT() throws IOException {
        return getPaymentFromFile("dummyPaymentOneAuthPending20Euro_PNT.json");
    }

    public Payment dummyPaymentOneAuthPending20EuroWithoutIbanPNT() throws IOException {
        return getPaymentFromFile("dummyPaymentOneAuthPending20EuroWithoutIban_PNT.json");
    }

    public Payment dummyPaymentOneAuthPending20EuroWithIbanEncrypted() throws IOException {
        return getPaymentFromFile("dummyPaymentOneAuthPending20EuroWithIbanEncrypted.json");
    }

    public Payment dummyPaymentOneAuthPending20EuroWithIbanPlain() throws IOException {
        return getPaymentFromFile("dummyPaymentOneAuthPending20EuroWithIbanPlain.json");
    }

    public Payment dummyPaymentOneAuthPending20EuroVOR() throws IOException {
        return getPaymentFromFile("dummyPaymentOneAuthPending20Euro_VOR.json");
    }

    public Payment dummyPaymentOneAuthPending20EuroPFF() throws IOException {
        return getPaymentFromFile("dummyPaymentOneAuthPending20Euro_PFF.json");
    }

    public Payment dummyPaymentOneAuthPending20EuroPFC() throws IOException {
        return getPaymentFromFile("dummyPaymentOneAuthPending20Euro_PFC.json");
    }

    public Payment dummyPaymentOneAuthPending20EuroIDEAL() throws IOException {
        return getPaymentFromFile("dummyPaymentOneAuthPending20Euro_IDEAL.json");
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

    public Payment dummyPaymentOneChargeSuccess20Euro() throws Exception {
        return getPaymentFromFile("dummyPaymentOneChargeSuccess20Euro.json");
    }

    public Payment dummyPaymentOneChargeFailure20Euro() throws Exception {
        return getPaymentFromFile("dummyPaymentOneChargeFailure20Euro.json");
    }

    public Payment dummyPaymentOneChargeInitial20Euro() throws Exception {
        return getPaymentFromFile("dummyPaymentOneChargeInitial20Euro.json");
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

    public Cart dummyCart() throws Exception {
        return getCartFromFile("dummyCart.json");
    }

    public Cart dummyKlarnaCart() throws Exception {
        return getCartFromFile(KLARNA_DIR + "dummyKlarnaCart.json");
    }

    public Cart dummyKlarnaCartWithoutDiscounts() throws Exception {
        return getCartFromFile(KLARNA_DIR + "dummyKlarnaCartWithoutDiscounts.json");
    }

    /**
     * Note, the transactions in this payment expected to be "updated" later using
     * {@link #dummyPaymentTwoTransactionsSuccessInitial()} mock, thus the payment an the transactions have respectively
     * same IDs.
     *
     * @see #dummyPaymentTwoTransactionsSuccessInitial()
     */
    public Payment dummyPaymentTwoTransactionsInitial() throws Exception {
        return getPaymentFromFile("dummyPaymentTwoTransactionsInitial.json");
    }

    /**
     * Note, this payment contains kind of "updated" transactions from the
     * {@link #dummyPaymentTwoTransactionsInitial()}, thus the payment an the transactions have respectively
     * same IDs.
     *
     * @see #dummyPaymentTwoTransactionsInitial()
     */
    public Payment dummyPaymentTwoTransactionsSuccessInitial() throws Exception {
        return getPaymentFromFile("dummyPaymentTwoTransactionsSuccessInitial.json");
    }

    /**
     * Note, the transactions in this payment expected to be "updated" later using
     * {@link #dummyPaymentTwoTransactionsSuccessPending()} mock, thus the payment an the transactions have respectively
     * same IDs.
     *
     * @see #dummyPaymentTwoTransactionsSuccessPending()
     */
    public Payment dummyPaymentTwoTransactionsPending() throws Exception {
        return getPaymentFromFile("dummyPaymentTwoTransactionsPending.json");
    }

    /**
     * Note, this payment contains kind of "updated" transactions from the
     * {@link #dummyPaymentTwoTransactionsPending()}, thus the payment an the transactions have respectively
     * same IDs.
     *
     * @see #dummyPaymentTwoTransactionsPending()
     */
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

    public PaymentWithCartLike createDummyPaymentWithCartLike(Payment payment, Cart cart) {
        return new PaymentWithCartLike(payment, cart);
    }

    public PaymentWithCartLike createKlarnaPaymentWithCartLike() throws Exception {
        return createDummyPaymentWithCartLike(dummyPaymentForKlarnaCart_KLV(), dummyKlarnaCart());
    }

    public PaymentWithCartLike createKlarnaPaymentWithCartLikeWithoutDiscount() throws Exception {
        return createDummyPaymentWithCartLike(dummyPaymentForKlarnaCartWithoutDiscounts_KLV(), dummyKlarnaCartWithoutDiscounts());
    }

    public PaymentWithCartLike createPaypalPaymentWithCartLike() throws Exception {
        return createDummyPaymentWithCartLike(dummyPaymentOneAuthPending20EuroPPE(), dummyCart());
    }

    public PaymentWithCartLike createPaydirektPaymentWithCartLike() throws Exception {
        return createDummyPaymentWithCartLike(dummyPaymentOneAuthPending20EuroPDT(), dummyCart());
    }

    public PaymentWithCartLike createBancontactPaymentWithCartLike() throws Exception {
        return createDummyPaymentWithCartLike(dummyPaymentOneAuthPending20EuroBCT(), dummyCart());
    }
    public PaymentWithCartLike createIDealPaymentWithCartLike() throws Exception {
        return createDummyPaymentWithCartLike(dummyPaymentOneAuthPending20EuroIDEAL(), dummyCart());
    }
}
