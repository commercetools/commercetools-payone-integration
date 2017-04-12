package specs.paymentmethods.creditcard;

import io.sphere.sdk.payments.Payment;
import org.apache.http.HttpResponse;
import org.concordion.api.MultiValueResult;
import specs.BaseFixture;
import specs.paymentmethods.BaseNotifiablePaymentFixture;

import javax.money.format.MonetaryFormats;
import java.util.Locale;

abstract public class BaseCreditCardChargeFixture extends BaseNotifiablePaymentFixture {

    public MultiValueResult handlePayment(final String paymentName,
                                          final String requestType) throws Exception {
        final HttpResponse response = requestToHandlePaymentByLegibleName(paymentName);
        final Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);
        final String amountAuthorized = (payment.getAmountAuthorized() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountAuthorized()) :
                BaseFixture.EMPTY_STRING;
        final String amountPaid = (payment.getAmountPaid() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountPaid()) :
                BaseFixture.EMPTY_STRING;

        return MultiValueResult.multiValueResult()
                .with("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .with("interactionCount", getInteractionRequestCount(payment, transactionId, requestType))
                .with("transactionState", getTransactionState(payment, transactionId))
                .with("amountAuthorized", amountAuthorized)
                .with("amountPaid", amountPaid)
                .with("version", payment.getVersion().toString());
    }
}
