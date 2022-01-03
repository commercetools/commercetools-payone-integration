package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.exceptions.NoCartLikeFoundException;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.commercetools.pspadapter.payone.domain.payone.model.common.StartSessionRequestWithCart;
import com.commercetools.pspadapter.payone.mapping.klarna.KlarnaRequestFactory;
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
import java.util.Random;

import static com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod.INVOICE_KLARNA;
import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.CLIENT_TOKEN;
import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.START_SESSION_REQUEST;
import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.START_SESSION_RESPONSE;
import static com.commercetools.pspadapter.tenant.TenantLoggerUtil.createTenantKeyValue;
import static io.sphere.sdk.http.HttpStatusCode.BAD_REQUEST_400;
import static io.sphere.sdk.http.HttpStatusCode.INTERNAL_SERVER_ERROR_500;
import static io.sphere.sdk.http.HttpStatusCode.OK_200;
import static java.lang.String.format;

public class SessionHandler {

    /**
     * How many times to retry if {@link ConcurrentModificationException} happens.
     */
    private static final int RETRIES_LIMIT = 5;
    private static final int RETRY_DELAY = 100; // msec
    public static final String ADD_PAYDATA_CLIENT_TOKEN = "add_paydata[client_token]";
    private final String payoneInterfaceName;
    private final LogstashMarker tenantNameKeyValue;
    private final PaymentService paymentService;
    private final KlarnaRequestFactory klarnaRequestFactory;
    private final CommercetoolsQueryExecutor commercetoolsQueryExecutor;

    private final Logger logger;
    private PayonePostService payonePostService;

    public SessionHandler(String payoneInterfaceName,
                          String tenantName,
                          CommercetoolsQueryExecutor commercetoolsQueryExecutor,
                          PayonePostService postService,
                          PaymentService paymentService,
                          KlarnaRequestFactory factory) {
        this.payoneInterfaceName = payoneInterfaceName;

        this.commercetoolsQueryExecutor = commercetoolsQueryExecutor;


        this.logger = LoggerFactory.getLogger(this.getClass());
        tenantNameKeyValue = createTenantKeyValue(tenantName);
        this.payonePostService = postService;
        this.paymentService = paymentService;
        this.klarnaRequestFactory = factory;
    }

    /**
     * Tries to start a payment session for the payment with the provided ID.
     *
     * @param paymentId identifies the payment to be processed
     * @return the result of handling the payment
     */
    public PayoneResult start(@Nonnull final String paymentId) {
        int retryCounter = 0;
        try {
            for (; retryCounter < RETRIES_LIMIT; retryCounter++) {
                try {
                    return startSession(paymentId);
                } catch (final ConcurrentModificationException concurrentModificationException) {
                    if (retryCounter == RETRIES_LIMIT - 1) {
                        throw concurrentModificationException;
                    } else {
                        Thread.sleep(calculateVariableDelay(retryCounter));
                    }
                }
            }
        } catch (final ConcurrentModificationException concurrentModificationException) {
            return handleConcurrentModificationException(paymentId, concurrentModificationException);
        } catch (final NotFoundException | NoCartLikeFoundException e) {
            return handleNotFoundException(paymentId, retryCounter, e);
        } catch (final ErrorResponseException e) {
            return errorResponseHandler(paymentId, retryCounter, e);
        } catch (final Exception e) {
            return handleException(paymentId, retryCounter, e);
        }
        return handleException(paymentId, retryCounter,
                new Exception("Unknown workflow error in PaymentHandler#handlePayment"));
    }

    private PayoneResult startSession(@Nonnull final String paymentId)
            throws ConcurrentModificationException {

        final PaymentWithCartLike paymentWithCartLike =
                commercetoolsQueryExecutor.getPaymentWithCartLike(paymentId);

        if (paymentWithCartLike == null) {
            final String errorMessage = format("The payment with id '%s' cannot be found.",
                    paymentId);
            return new PayoneResult(BAD_REQUEST_400, errorMessage);
        }

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

        StartSessionRequestWithCart startSessionRequest = klarnaRequestFactory.createStartSessionRequest(paymentWithCartLike);

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
        String requestBody = SphereJsonUtils.toJsonString(startSessionRequest);
        updateActions.add(SetCustomField.ofObject(START_SESSION_REQUEST, requestBody));
        String responseBody = SphereJsonUtils.toJsonString(response);
        updateActions.add(SetCustomField.ofObject(START_SESSION_RESPONSE, responseBody));

        String clientToken = null;
        if (response.containsKey(ADD_PAYDATA_CLIENT_TOKEN)) {
            clientToken = response.get(ADD_PAYDATA_CLIENT_TOKEN);
            updateActions.add(SetCustomField.ofObject(CLIENT_TOKEN, clientToken));
        }
        paymentService.updatePayment(paymentWithCartLike.getPayment(), updateActions).toCompletableFuture().join();
        return new PayoneResult(StringUtils.isNotBlank(clientToken) ? OK_200 : BAD_REQUEST_400, responseBody);
    }

    private PayoneResult handleConcurrentModificationException(
            @Nonnull final String paymentId,
            @Nonnull final ConcurrentModificationException concurrentModificationException) {

        final String errorMessage = format("The payment with id '%s' couldn't be processed after %s retries. " +
                "One retry iteration here includes multiple payone/ctp service retries.", paymentId, RETRIES_LIMIT);
        logger.error(errorMessage, concurrentModificationException);
        return new PayoneResult(HttpStatusCode.ACCEPTED_202, errorMessage);
    }

    private PayoneResult handleNotFoundException(
            @Nonnull final String paymentId,
            int retriedCount,
            @Nonnull final Exception exception) {

        final String body = format("Failed to process the commercetools Payment with id [%s], as the payment or the cart could not be found after [%d] retries.",
                paymentId, retriedCount);
        logger.error(tenantNameKeyValue, body, exception);
        return new PayoneResult(HttpStatusCode.NOT_FOUND_404, body);
    }

    private PayoneResult errorResponseHandler(
            @Nonnull final String paymentId,
            int retriedCount,
            @Nonnull final ErrorResponseException e) {

        logger.error(tenantNameKeyValue,
                format("Failed to process the commercetools Payment with id [%s] due to an error response from the commercetools platform after [%d] retries.",
                        paymentId, retriedCount), e);
        return new PayoneResult(e.getStatusCode(),
                format("Failed to process the commercetools Payment with id [%s], due to an error response from the "
                        + "commercetools platform. Try again later.", paymentId));
    }

    private PayoneResult handleException(
            @Nonnull final String paymentId,
            int retriedCount,
            @Nonnull final Exception exception) {

        logger.error(tenantNameKeyValue,
                format("Unexpected error occurred when processing commercetools Payment with id [%s] after [%d] retries.",
                        paymentId, retriedCount), exception);
        return new PayoneResult(INTERNAL_SERVER_ERROR_500,
                format("Unexpected error occurred when processing commercetools Payment with id [%s]. "
                        + "See the service logs", paymentId));
    }

    private static long calculateVariableDelay(final long triedAttempts) {
        final long randomNumberInRange = getRandomNumberInRange(50, RETRY_DELAY);
        final long timeoutMultipliedByTriedAttempts = RETRY_DELAY * (triedAttempts + 1);
        return timeoutMultipliedByTriedAttempts + randomNumberInRange;
    }

    private static long getRandomNumberInRange(final long min, final long max) {
        return new Random().longs(min, (max + 1)).limit(1).findFirst().getAsLong();
    }
}
