package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.exceptions.NoCartLikeFoundException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.NotFoundException;
import io.sphere.sdk.http.HttpStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ConcurrentModificationException;
import java.util.concurrent.CompletionException;

import static com.commercetools.pspadapter.tenant.TenantLoggerUtil.createLoggerName;
import static io.sphere.sdk.http.HttpStatusCode.INTERNAL_SERVER_ERROR_500;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static net.logstash.logback.marker.Markers.append;

public class PaymentHandler {

    /**
     * How many times to retry if {@link ConcurrentModificationException} happens.
     */
    private static final int RETRIES_LIMIT = 20;
    private static final int RETRY_DELAY = 100; // msec

    private static final String TENANT_NAME_LOG_FIELD_KEY = "tenantName";
    private final String payoneInterfaceName;
    private final String tenantName;
    private final CommercetoolsQueryExecutor commercetoolsQueryExecutor;
    private final PaymentDispatcher paymentDispatcher;

    private final Logger logger;

    public PaymentHandler(String payoneInterfaceName, String tenantName,
                          CommercetoolsQueryExecutor commercetoolsQueryExecutor, PaymentDispatcher paymentDispatcher) {
        this.payoneInterfaceName = payoneInterfaceName;
        this.tenantName = tenantName;
        this.commercetoolsQueryExecutor = commercetoolsQueryExecutor;
        this.paymentDispatcher = paymentDispatcher;


        this.logger = LoggerFactory.getLogger(createLoggerName(this.getClass(), tenantName));
    }

    /**
     * Tries to handle the payment with the provided ID.
     *
     * @param paymentId identifies the payment to be processed
     * @return the result of handling the payment
     */
    public PaymentHandleResult handlePayment(@Nonnull final String paymentId) {
        try {
            for (int i = 0; i < RETRIES_LIMIT; i++) {
                try {
                    final PaymentWithCartLike paymentWithCartLike = commercetoolsQueryExecutor.getPaymentWithCartLike(paymentId);
                    String paymentInterface = paymentWithCartLike.getPayment().getPaymentMethodInfo().getPaymentInterface();
                    if (!payoneInterfaceName.equals(paymentInterface)) {
                        logger.warn(
                            append(TENANT_NAME_LOG_FIELD_KEY, tenantName),
                            "Wrong payment interface name: expected [{}], found [{}]", payoneInterfaceName, paymentInterface);
                        return new PaymentHandleResult(HttpStatusCode.BAD_REQUEST_400);
                    }

                    paymentDispatcher.dispatchPayment(paymentWithCartLike);
                    return new PaymentHandleResult(HttpStatusCode.OK_200);
                } catch (final ConcurrentModificationException cme) {
                    logger.warn(
                        append(TENANT_NAME_LOG_FIELD_KEY, tenantName),
                        "Exception on dispatchPayment with id [{}]. Retry {} of {}", paymentId, i + 1, RETRIES_LIMIT);
                    Thread.sleep(RETRY_DELAY);
                }
            }

            logger.error(
                append(TENANT_NAME_LOG_FIELD_KEY, tenantName),
                "The payment [{}] couldn't be processed after {} retries", paymentId, RETRIES_LIMIT);
            return new PaymentHandleResult(HttpStatusCode.ACCEPTED_202,
                    format("The payment couldn't be processed after %s retries", RETRIES_LIMIT));

        } catch (final NotFoundException | NoCartLikeFoundException e) {
            return handleNotFoundException(paymentId, e);
        } catch (final ErrorResponseException e) {
            return errorResponseHandler(e, paymentId);
        } catch (final CompletionException e) {
            return completionExceptionHandler(e, paymentId);
        } catch (final Exception e) {
            return handleThrowableInResponse(e, paymentId);
        }
    }

     private PaymentHandleResult handleNotFoundException(@Nonnull String paymentId, @Nonnull Exception exception ) {
        final String body = format("Failed to process the commercetools Payment with id [%s], as the payment or "
            + "the cart could not be found.", paymentId);
        // Temporary adding the exception to error log to simplify debugging.
        logger.error(
            append(TENANT_NAME_LOG_FIELD_KEY, tenantName), body, exception);
        return new PaymentHandleResult(HttpStatusCode.NOT_FOUND_404, body);
    }

    private PaymentHandleResult errorResponseHandler(@Nonnull ErrorResponseException e, @Nonnull String paymentId) {
        logger.warn(
            append(TENANT_NAME_LOG_FIELD_KEY, tenantName),
            format("Failed to process the commercetools Payment with id [%s] due to an error response from the "
            + "commercetools platform.", paymentId), e);
        return new PaymentHandleResult(e.getStatusCode(),
                format("Failed to process the commercetools payment with id [%s], due to an error response from the "
                    + "commercetools platform. Try again later.", paymentId));
    }

    /**
     * Completion exceptions used to tell nothing, thus we should try to report the exception cause.
     *
     * @param e Exception to report
     * @return {@link PaymentHandleResult} with 500 status and message which refers to the logs.
     */
    private PaymentHandleResult completionExceptionHandler(@Nonnull CompletionException e, @Nonnull String paymentId) {
        String causeMessage = ofNullable(e.getCause()).map(Throwable::toString).orElse("null");
        logger.error(
            append(TENANT_NAME_LOG_FIELD_KEY, tenantName),
            "Completion exception error: {}\nCause: {}", e.toString(), causeMessage);
        return new PaymentHandleResult(INTERNAL_SERVER_ERROR_500,
                format("An error occurred during communication with the commercetools platform when processing [%s] payment. See the service logs", paymentId));
    }

    private PaymentHandleResult handleThrowableInResponse(@Nonnull Throwable throwable, @Nonnull String paymentId) {
        logger.error(
            append(TENANT_NAME_LOG_FIELD_KEY, tenantName),
            "Error in response: ", throwable);
        return new PaymentHandleResult(INTERNAL_SERVER_ERROR_500,
                format("Unexpected error occurred when processing commercetools Payment with id [%s]. "
                    + "See the service logs", paymentId));
    }
}
