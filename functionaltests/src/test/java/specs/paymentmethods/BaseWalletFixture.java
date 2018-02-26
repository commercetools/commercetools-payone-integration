package specs.paymentmethods;

import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.types.CustomFields;
import org.apache.http.HttpResponse;
import org.concordion.api.MultiValueResult;
import specs.response.BasePaymentFixture;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.*;

/**
 * Basic fixture Authorization and Charge test case of wallet payment types (PayPal, Paydirekt)
 */
public class BaseWalletFixture extends BasePaymentFixture {

    public MultiValueResult createPayment(
            final String paymentName,
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode,
            final String baseRedirectUrl) {

        Payment payment = createAndSaveWalletPayment(paymentName, paymentMethod, transactionType, centAmount, currencyCode, baseRedirectUrl);

        Optional<CustomFields> custom = Optional.ofNullable(payment.getCustom());

        return MultiValueResult.multiValueResult()
                .with("paymentId", payment.getId())
                .with("successUrl", custom.map(customFields -> customFields.getFieldAsString(SUCCESS_URL_FIELD)).orElse("<<UNDEFINED>>"))
                .with("errorUrl", custom.map(customFields -> customFields.getFieldAsString(ERROR_URL_FIELD)).orElse("<<UNDEFINED>>"))
                .with("cancelUrl", custom.map(customFields -> customFields.getFieldAsString(CANCEL_URL_FIELD)).orElse("<<UNDEFINED>>"));
    }

    public MultiValueResult handlePayment(final String paymentName,
                                          final String requestType) throws IOException, ExecutionException {
        final HttpResponse response = requestToHandlePaymentByLegibleName(paymentName);
        final Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);

        return MultiValueResult.multiValueResult()
                .with("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .with("interactionCount", getInteractionRequestCount(payment, transactionId, requestType))
                .with("transactionState", getTransactionState(payment, transactionId))
                .with("version", payment.getVersion().toString());
    }

    public MultiValueResult fetchPaymentDetails(final String paymentName) throws ExecutionException {
        Payment payment = fetchPaymentByLegibleName(paymentName);

        final String transactionId = getIdOfLastTransaction(payment);
        final String responseRedirectUrl = getInteractionRedirect(payment, transactionId)
                .map(i -> i.getFieldAsString(CustomFieldKeys.REDIRECT_URL_FIELD))
                .orElse(EMPTY_STRING);
        int urlTrimAt = responseRedirectUrl.contains("?") ? responseRedirectUrl.indexOf("?") : 0;

        final long appointedNotificationCount =
                getTotalNotificationCountOfAction(payment, "appointed");

        final long paidNotificationCount = getTotalNotificationCountOfAction(payment, "paid");

        return MultiValueResult.multiValueResult()
                .with("transactionState", getTransactionState(payment, transactionId))
                .with("responseRedirectUrlStart", responseRedirectUrl.substring(0, urlTrimAt))
                .with("responseRedirectUrlFull", responseRedirectUrl)
                .with("appointedNotificationCount", String.valueOf(appointedNotificationCount))
                .with("paidNotificationCount", String.valueOf(paidNotificationCount))
                .with("version", payment.getVersion().toString());
    }

    public boolean isInteractionRedirectPresent(final String paymentName) throws ExecutionException {
        Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);

        return getInteractionRedirect(payment, transactionId).isPresent();
    }
}
