package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.exceptions.NoCartLikeFoundException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.NotFoundException;
import io.sphere.sdk.http.HttpStatusCode;
import net.logstash.logback.marker.LogstashMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ConcurrentModificationException;
import java.util.Random;

import static com.commercetools.pspadapter.tenant.TenantLoggerUtil.createTenantKeyValue;
import static io.sphere.sdk.http.HttpStatusCode.INTERNAL_SERVER_ERROR_500;
import static java.lang.String.format;

public class PaymentHandler {

    /**
     * How many times to retry if {@link ConcurrentModificationException} happens.
     */
    private static final int RETRIES_LIMIT = 5;
    private static final int RETRY_DELAY = 100; // msec

    private final String payoneInterfaceName;
    private final LogstashMarker tenantNameKeyValue;

    private final CommercetoolsQueryExecutor commercetoolsQueryExecutor;
    private final PaymentDispatcher paymentDispatcher;

    private final Logger logger;

    public PaymentHandler(String payoneInterfaceName, String tenantName,
                          CommercetoolsQueryExecutor commercetoolsQueryExecutor, PaymentDispatcher paymentDispatcher) {
        this.payoneInterfaceName = payoneInterfaceName;

        this.commercetoolsQueryExecutor = commercetoolsQueryExecutor;
        this.paymentDispatcher = paymentDispatcher;

        this.logger = LoggerFactory.getLogger(this.getClass());
        tenantNameKeyValue = createTenantKeyValue(tenantName);
    }

    /**
     * Tries to handle the payment with the provided ID.
     *
     * @param paymentId identifies the payment to be processed
     * @return the result of handling the payment
     */
    public PaymentHandleResult handlePayment(@Nonnull final String paymentId) {
        int retryCounter = 0;
        try {
            return processPayment(paymentId);
        } catch (final ConcurrentModificationException concurrentModificationException) {
            return handleConcurrentModificationException(paymentId, concurrentModificationException);
        } catch (final NotFoundException | NoCartLikeFoundException e) {
            return handleNotFoundException(paymentId, retryCounter, e);
        } catch (final ErrorResponseException e) {
            return errorResponseHandler(paymentId, retryCounter, e);
        } catch (final Exception e) {
            return handleException(paymentId, retryCounter, e);
        }
    }

    private PaymentHandleResult processPayment(@Nonnull final String paymentId)
        throws ConcurrentModificationException {

        final PaymentWithCartLike paymentWithCartLike =
            commercetoolsQueryExecutor.getPaymentWithCartLike(paymentId);

        final String paymentInterface = paymentWithCartLike
            .getPayment()
            .getPaymentMethodInfo()
            .getPaymentInterface();

        if (!payoneInterfaceName.equals(paymentInterface)) {
            final String errorMessage = format("Wrong payment interface name: expected '%s', found '%s' for the "
                + "commercetools Payment with id '%s'.", payoneInterfaceName, paymentInterface, paymentId);
            return new PaymentHandleResult(HttpStatusCode.BAD_REQUEST_400, errorMessage);
        }

        paymentDispatcher.dispatchPayment(paymentWithCartLike);
        return new PaymentHandleResult(HttpStatusCode.OK_200);
    }

    private PaymentHandleResult handleConcurrentModificationException(
        @Nonnull final String paymentId,
        @Nonnull final ConcurrentModificationException concurrentModificationException) {

        final String errorMessage = format("The payment with id '%s' couldn't be processed after %s retries. " +
                "One retry iteration here includes multiple payone/ctp service retries.", paymentId, RETRIES_LIMIT);
        logger.error(errorMessage, concurrentModificationException);
        return new PaymentHandleResult(HttpStatusCode.ACCEPTED_202, errorMessage);
    }

    private PaymentHandleResult handleNotFoundException(
            @Nonnull final String paymentId,
            int retriedCount,
            @Nonnull final Exception exception) {

        final String body = format("Failed to process the commercetools Payment with id [%s], as the payment or the cart could not be found after [%d] retries.",
                paymentId, retriedCount);
        logger.error(tenantNameKeyValue, body, exception);
        return new PaymentHandleResult(HttpStatusCode.NOT_FOUND_404, body);
    }

    private PaymentHandleResult errorResponseHandler(
        @Nonnull final String paymentId,
        int retriedCount,
        @Nonnull final ErrorResponseException e) {

        logger.error(tenantNameKeyValue,
            format("Failed to process the commercetools Payment with id [%s] due to an error response from the commercetools platform after [%d] retries.",
                    paymentId, retriedCount), e);
        return new PaymentHandleResult(e.getStatusCode(),
                format("Failed to process the commercetools Payment with id [%s], due to an error response from the "
                    + "commercetools platform. Try again later.", paymentId));
    }

    private PaymentHandleResult handleException(
        @Nonnull final String paymentId,
        int retriedCount,
        @Nonnull final Exception exception) {

        logger.error(tenantNameKeyValue,
            format("Unexpected error occurred when processing commercetools Payment with id [%s] after [%d] retries.",
                    paymentId, retriedCount), exception);
        return new PaymentHandleResult(INTERNAL_SERVER_ERROR_500,
                format("Unexpected error occurred when processing commercetools Payment with id [%s]. "
                    + "See the service logs", paymentId));
    }

    private static long calculateVariableDelay(final long triedAttempts) {
        final long randomNumberInRange = getRandomNumberInRange(50, RETRY_DELAY);
        final long timeoutMultipliedByTriedAttempts = RETRY_DELAY * (triedAttempts+1);
        return timeoutMultipliedByTriedAttempts + randomNumberInRange;
    }

    private static long getRandomNumberInRange(final long min, final long max) {
        return new Random().longs(min, (max + 1)).limit(1).findFirst().getAsLong();
    }
}
