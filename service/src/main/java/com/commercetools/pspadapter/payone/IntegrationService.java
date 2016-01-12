package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.notification.NotificationDispatcher;
import com.google.common.base.Strings;
import io.sphere.sdk.http.HttpStatusCode;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import spark.Spark;

import javax.annotation.Nonnull;
import java.util.ConcurrentModificationException;
import java.util.concurrent.CompletionException;

/**
 * @author fhaertig
 * @author Jan Wolter
 */
public class IntegrationService {

    public static final Logger LOG = LogManager.getLogger(IntegrationService.class);

    public static final String HEROKU_ASSIGNED_PORT = "PORT";

    private final CommercetoolsQueryExecutor commercetoolsQueryExecutor;
    private final PaymentDispatcher paymentDispatcher;
    private final CustomTypeBuilder typeBuilder;
    private final NotificationDispatcher notificationDispatcher;

    IntegrationService(
            final CustomTypeBuilder typeBuilder,
            final CommercetoolsQueryExecutor commercetoolsQueryExecutor,
            final PaymentDispatcher paymentDispatcher,
            final NotificationDispatcher notificationDispatcher) {
        this.commercetoolsQueryExecutor = commercetoolsQueryExecutor;
        this.paymentDispatcher = paymentDispatcher;
        this.typeBuilder = typeBuilder;
        this.notificationDispatcher = notificationDispatcher;
    }

    public void start() {
        createCustomTypes();

        Spark.port(port());

        Spark.get("/commercetools/handle/payments/:id", (req, res) -> {
            try {
                final PaymentHandleResult paymentHandleResult = handlePayment(req.params("id"));
                res.status(paymentHandleResult.statusCode());
                // TODO response body
            } catch (final Exception e) {
                // TODO jw: we probably need to differentiate depending on the exception type
                res.status(404);
                res.type("text/plain; charset=utf-8");
                res.body("The given payment could not be found.");
            }
            return res;
        });

        Spark.post("/payone/notification", (req, res) -> {
            LOG.info("<- Received POST from Payone: " + req.body());
            Notification notification = Notification.fromKeyValueString(req.body(), "\r?\n?&");

            try {
                notificationDispatcher.dispatchNotification(notification);
                res.status(501);
                return "Currently not implemented";
            } catch (IllegalArgumentException ex) {
                res.status(400);
                return ex.getMessage();
            }
        });

        Spark.awaitInitialization();
    }

    /**
     * Tries to handle the payment with the provided ID.
     *
     * @param paymentId identifies the payment to be processed
     * @return the result of handling the payment
     */
    public PaymentHandleResult handlePayment(@Nonnull final String paymentId) {
        try {
            final PaymentWithCartLike payment = commercetoolsQueryExecutor.getPaymentWithCartLike(paymentId);
            if (!"PAYONE".equals(payment.getPayment().getPaymentMethodInfo().getPaymentInterface()))
            {
                return new PaymentHandleResult(HttpStatusCode.BAD_REQUEST_400);
            }

            try {
                final PaymentWithCartLike result = paymentDispatcher.dispatchPayment(payment);
                return new PaymentHandleResult(HttpStatusCode.OK_200);
            } catch (final ConcurrentModificationException cme) {
                try {
                    // TODO Better
                    for (int i = 0; i < 10; i++) {
                        Thread.sleep(100);
                        final PaymentWithCartLike paymentWithCartLike =
                                commercetoolsQueryExecutor.getPaymentWithCartLike(paymentId);

                        if (paymentWithCartLike.getPayment().getVersion() > payment.getPayment().getVersion()) {
                            // TODO review this, it assumes that the concurrent modifier processes the same transaction
                            // but it could be a notification processor;
                            // maybe another dispatcher invocation would make more sense
                            return new PaymentHandleResult(HttpStatusCode.OK_200);
                        }
                    }
                    return new PaymentHandleResult(HttpStatusCode.ACCEPTED_202);
                } catch (final RuntimeException | InterruptedException e) {
                    return logThrowableInResponse(e);
                }
            } catch (final RuntimeException e) {
                return logThrowableInResponse(e);
            }
        } catch (final CompletionException e) {
            // TODO clarify if we should use localized message
            final String body =
                    String.format("Could not find payment with ID \"%s\", cause: %s", paymentId, e.getMessage());

            return new PaymentHandleResult(HttpStatusCode.NOT_FOUND_404, body);
        } catch (final RuntimeException e) {
            // TODO clarify if we should use localized message
            return new PaymentHandleResult(HttpStatusCode.INTERNAL_SERVER_ERROR_500, e.getMessage());
        }
    }

    private PaymentHandleResult logThrowableInResponse(final Throwable throwable) {
        // TODO jw: we probably need to differentiate depending on the exception type
        return new PaymentHandleResult(
                HttpStatusCode.INTERNAL_SERVER_ERROR_500,
                String.format("Sorry, but you hit us between the eyes. Cause: %s", throwable.getMessage()));
    }

    private void createCustomTypes() {
        typeBuilder.run();
    }

    public void stop() {
        Spark.stop();
    }

    public int port() {
        final String environmentVariable = System.getenv(HEROKU_ASSIGNED_PORT);
        if (!Strings.isNullOrEmpty(environmentVariable)) {
            return Integer.parseInt(environmentVariable);
        }

        final String systemProperty = System.getProperty(HEROKU_ASSIGNED_PORT, "8080");
        return Integer.parseInt(systemProperty);
    }

    public CommercetoolsQueryExecutor getCommercetoolsQueryExecutor() {
        return commercetoolsQueryExecutor;
    }
}
