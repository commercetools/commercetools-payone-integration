package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.exceptions.NoCartLikeFoundException;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.notification.NotificationDispatcher;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.NotFoundException;
import io.sphere.sdk.http.HttpStatusCode;
import io.sphere.sdk.json.SphereJsonUtils;
import org.apache.http.entity.ContentType;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import spark.Redirect;
import spark.Spark;

import javax.annotation.Nonnull;
import java.util.ConcurrentModificationException;
import java.util.concurrent.CompletionException;

import static spark.Spark.redirect;

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

        redirect.any("/", "/health", Redirect.Status.MOVED_PERMANENTLY);

        // This is a temporary jerry-rig for the load balancer to check connection with the service itself.
        // For now it just returns a JSON response {"status":200}
        // It should be expanded to a more real health-checker service, which really performs PAYONE status check.
        // But don't forget, a load balancer may call this URL very often (like 1 per sec),
        // so don't make this request processor heavy, or implement is as independent background service.
        Spark.get("/health", (req, res) -> {
            res.status(HttpStatusCode.OK_200);
            res.type(ContentType.APPLICATION_JSON.getMimeType());
            return ImmutableMap.of("status", HttpStatus.OK_200);
        }, SphereJsonUtils::toJsonString);

        Spark.get("/commercetools/handle/payments/:id", (req, res) -> {
            final PaymentHandleResult paymentHandleResult = handlePayment(req.params("id"));
            if (!paymentHandleResult.body().isEmpty()) {
                LOG.debug(String.format("--> Result body of handle/payments/%s: %s", req.params("id"), paymentHandleResult.body()));
            }
            res.status(paymentHandleResult.statusCode());
            return res;
        }, new HandlePaymentResponseTransformer());

        Spark.post("/payone/notification", (req, res) -> {
            LOG.debug("<- Received POST from Payone: " + req.body());
            try {
                final Notification notification = Notification.fromKeyValueString(req.body(), "\r?\n?&");
                notificationDispatcher.dispatchNotification(notification);
            } catch (Exception e) {
                // Potential issues for this exception are:
                // 1. req.body is mal-formed hence can't by parsed by Notification.fromKeyValueString
                // 2. Invalid access secret values in the request (account id, key, portal id etc)
                // 3. ConcurrentModificationException in case the respective payment could not be updated
                //    after two attempts due to concurrent modifications; a later retry might be successful
                // 4. Execution timeout, if sphere client has not responded in time
                // 5. unknown notification type
                // Any other unexpected error.
                LOG.error("Payone notification handling error. Request body: " + req.body(), e);
                res.status(400);
                return e.getMessage();
            }
            res.status(200);
            return "TSOK";
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
            // TODO jw: make configurable or use constants instead of magic numbers at least
            for (int i = 0; i < 20; i++) {
                try {
                    final PaymentWithCartLike payment = commercetoolsQueryExecutor.getPaymentWithCartLike(paymentId);
                    if (!"PAYONE".equals(payment.getPayment().getPaymentMethodInfo().getPaymentInterface())) {
                        return new PaymentHandleResult(HttpStatusCode.BAD_REQUEST_400);
                    }

                    final PaymentWithCartLike result = paymentDispatcher.dispatchPayment(payment);
                    return new PaymentHandleResult(HttpStatusCode.OK_200);
                } catch (final ConcurrentModificationException cme) {
                    Thread.sleep(100);
                }
            }
            return new PaymentHandleResult(HttpStatusCode.ACCEPTED_202);
        } catch (final InterruptedException e) {
            return logThrowableInResponse(e);
        } catch (final NotFoundException | NoCartLikeFoundException e) {
            // TODO clarify if we should use localized message
            final String body =
            String.format("Could not process payment with ID \"%s\", cause: %s", paymentId, e.getMessage());

            return new PaymentHandleResult(HttpStatusCode.NOT_FOUND_404, body);
        } catch (final ErrorResponseException e) {
            LOG.debug("An Error Response from commercetools platform", e);
            return new PaymentHandleResult(e.getStatusCode(), e.getMessage());
        } catch (final CompletionException e) {
            return logCauseMessageInResponse("An error occurred during communication with the commercetools platform: " + e.getMessage());
        } catch (final RuntimeException e) {
            // TODO clarify if we should use localized message
            return logThrowableInResponse(e);
        }
    }


    private PaymentHandleResult logThrowableInResponse(final Throwable throwable) {
        LOG.debug(throwable);
        // TODO jw: we probably need to differentiate depending on the exception type
        return logCauseMessageInResponse(String.format("Sorry, but you hit us between the eyes. Cause: %s", throwable.getMessage()));
    }

    private PaymentHandleResult logCauseMessageInResponse(final String message) {
        LOG.error(message);
        return new PaymentHandleResult(
                HttpStatusCode.INTERNAL_SERVER_ERROR_500,
                message);
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
