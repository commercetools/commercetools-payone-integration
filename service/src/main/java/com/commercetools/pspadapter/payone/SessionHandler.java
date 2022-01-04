package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.exceptions.NoCartLikeFoundException;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.commercetools.pspadapter.payone.domain.payone.model.common.StartSessionRequestWithCart;
import com.commercetools.pspadapter.payone.mapping.klarna.KlarnaRequestFactory;
import com.commercetools.pspadapter.payone.mapping.klarna.PayoneKlarnaCountryToLanguageMapper;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.service.PaymentService;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.NotFoundException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.http.HttpStatusCode;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentMethodInfo;
import io.sphere.sdk.payments.commands.updateactions.SetCustomField;
import net.logstash.logback.marker.LogstashMarker;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;

import static com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod.INVOICE_KLARNA;
import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.CLIENT_TOKEN;
import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.START_SESSION_RESPONSE;
import static com.commercetools.pspadapter.tenant.TenantLoggerUtil.createTenantKeyValue;
import static io.sphere.sdk.http.HttpStatusCode.BAD_REQUEST_400;
import static io.sphere.sdk.http.HttpStatusCode.INTERNAL_SERVER_ERROR_500;
import static io.sphere.sdk.http.HttpStatusCode.OK_200;
import static java.lang.String.format;

public class SessionHandler {


    public static final String ADD_PAYDATA_CLIENT_TOKEN = "add_paydata[client_token]";
    private final TenantConfig tenantConfig;
    private final String payoneInterfaceName;
    private final LogstashMarker tenantNameKeyValue;
    private final PaymentService paymentService;

    private final CommercetoolsQueryExecutor commercetoolsQueryExecutor;

    private final Logger logger;
    private PayonePostService payonePostService;

    public SessionHandler(String payoneInterfaceName, String tenantName,
                          CommercetoolsQueryExecutor commercetoolsQueryExecutor, TenantConfig tenantConfig,
                          PayonePostService postService, PaymentService paymentService) {
        this.logger = LoggerFactory.getLogger(this.getClass());

        this.payoneInterfaceName = payoneInterfaceName;
        this.commercetoolsQueryExecutor = commercetoolsQueryExecutor;
        this.tenantNameKeyValue = createTenantKeyValue(tenantName);
        this.tenantConfig = tenantConfig;
        this.payonePostService = postService;
        this.paymentService = paymentService;
    }

    /**
     * Tries to start a payment session for the payment with the provided ID.
     *
     * @param paymentId identifies the payment to be processed
     * @return the result of handling the payment
     */
    public PayoneResult startSession(@Nonnull final String paymentId) {
        try {
            final PaymentWithCartLike paymentWithCartLike =
                    commercetoolsQueryExecutor.getPaymentWithCartLike(paymentId);
            if (paymentWithCartLike == null) {
                final String body = format("The payment for id [%s] cannot be found", paymentId);
                logger.error(tenantNameKeyValue, body);
                return new PayoneResult(HttpStatusCode.NOT_FOUND_404, body);
            }
            return startSession(paymentWithCartLike);
        } catch (final ConcurrentModificationException concurrentModificationException) {
            return handleConcurrentModificationException(paymentId, concurrentModificationException);
        } catch (final NotFoundException | NoCartLikeFoundException e) {
            return handleNotFoundException(paymentId, e);
        } catch (final ErrorResponseException e) {
            return errorResponseHandler(paymentId, e);
        } catch (final Exception e) {
            return handleException(paymentId, e);
        }
    }

    private PayoneResult startSession(@Nonnull final PaymentWithCartLike paymentWithCartLike)
            throws ConcurrentModificationException {
        String paymentId =paymentWithCartLike.getPayment().getId();
        final String paymentInterface = paymentWithCartLike
                .getPayment()
                .getPaymentMethodInfo()
                .getPaymentInterface();

        if (!payoneInterfaceName.equals(paymentInterface)) {
            final String errorMessage = format("Wrong payment interface name: expected '%s', found '%s' for the "
                    + "commercetools Payment with id '%s'.", payoneInterfaceName, paymentInterface, paymentId);
            return new PayoneResult(BAD_REQUEST_400, errorMessage);
        }
        final PaymentMethodInfo paymentMethodInfo = paymentWithCartLike.getPayment().getPaymentMethodInfo();
        if (!StringUtils.equals(paymentMethodInfo.getMethod(), INVOICE_KLARNA.getKey())) {
            final String errorMessage = format("The session can only be started for the payment method '%s'",
                    INVOICE_KLARNA.getKey());
            return new PayoneResult(BAD_REQUEST_400, errorMessage);
        }
        if (paymentWithCartLike.getPayment().getCustom() != null &&
                StringUtils.isNotBlank(paymentWithCartLike.getPayment().getCustom().getFieldAsString(CLIENT_TOKEN))) {
            return new PayoneResult(OK_200, paymentWithCartLike.getPayment().getCustom().getFieldAsString(START_SESSION_RESPONSE));
        }
        KlarnaRequestFactory requestFactory = new KlarnaRequestFactory(tenantConfig, new PayoneKlarnaCountryToLanguageMapper());
        StartSessionRequestWithCart startSessionRequest = requestFactory.createStartSessionRequest(paymentWithCartLike);

        List<UpdateAction<Payment>> updateActions = new ArrayList<UpdateAction<Payment>>();
        final Map<String, String> response;
        try {
            response = payonePostService.executePost(startSessionRequest);
        } catch (PayoneException paymentException) {
            final String errorMessage = format("The 'startSession' Request to Payone failed for commercetools Payment" +
                    " with id '%s'.", paymentWithCartLike.getPayment().getId());
            logger.error(errorMessage, paymentException);
            return new PayoneResult(BAD_REQUEST_400, errorMessage);
        }

        String responseBody = SphereJsonUtils.toJsonString(response);
        updateActions.add(SetCustomField.ofObject(START_SESSION_RESPONSE, responseBody));

        String clientToken = null;
        if (!response.containsKey(ADD_PAYDATA_CLIENT_TOKEN)) {
            final String errorMessage = format("The client token was not found in the payone response of the " +
                    "Startsession request for payment '%s'", paymentId);
            return new PayoneResult(BAD_REQUEST_400, errorMessage);
        }
        clientToken = response.get(ADD_PAYDATA_CLIENT_TOKEN);
        updateActions.add(SetCustomField.ofObject(CLIENT_TOKEN, clientToken));

        paymentService.updatePayment(paymentWithCartLike.getPayment(), updateActions);
        return new PayoneResult(StringUtils.isNotBlank(clientToken) ? OK_200 : BAD_REQUEST_400, responseBody);
    }

    private PayoneResult handleConcurrentModificationException(
            @Nonnull final String paymentId,
            @Nonnull final ConcurrentModificationException concurrentModificationException) {

        final String errorMessage = format("The payment with id '%s' couldn't be processed after %s retries. " +
                "One retry iteration here includes multiple payone/ctp service retries.", paymentId);
        logger.error(errorMessage, concurrentModificationException);
        return new PayoneResult(HttpStatusCode.ACCEPTED_202, errorMessage);
    }

    private PayoneResult handleNotFoundException(
            @Nonnull final String paymentId,
            @Nonnull final Exception exception) {

        final String body = format("Failed to process the commercetools Payment with id [%s]",
                paymentId);
        logger.error(tenantNameKeyValue, body, exception);
        return new PayoneResult(HttpStatusCode.NOT_FOUND_404, body);
    }

    private PayoneResult errorResponseHandler(
            @Nonnull final String paymentId,
            @Nonnull final ErrorResponseException e) {
        String message = format("Failed to process the commercetools payment with id [%s] due to an error response from the " +
                        "commercetools platform.",
                paymentId);
        logger.error(tenantNameKeyValue, message, e);
        return new PayoneResult(e.getStatusCode(),message);
    }

    private PayoneResult handleException(
            @Nonnull final String paymentId,
            @Nonnull final Exception exception) {
        String message = format("Unexpected error occurred when processing commercetools Payment with id [%s].",
                paymentId);
        logger.error(tenantNameKeyValue, message, exception);
        return new PayoneResult(INTERNAL_SERVER_ERROR_500, message);
    }
}
