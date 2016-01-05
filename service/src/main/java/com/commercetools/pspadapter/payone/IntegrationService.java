package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.google.common.base.Strings;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import spark.Response;
import spark.Spark;

import java.util.ConcurrentModificationException;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class IntegrationService {

    public static final Logger LOG = LogManager.getLogger(IntegrationService.class);

    public static final String HEROKU_ASSIGNED_PORT = "PORT";

    private final CommercetoolsQueryExecutor commercetoolsQueryExecutor;
    private final PaymentDispatcher paymentDispatcher;
    private final CustomTypeBuilder typeBuilder;
    private final NotificationDispatcher notificationDispatcher;
    private final ResultProcessor resultProcessor;

    IntegrationService(
            final CustomTypeBuilder typeBuilder,
            final CommercetoolsQueryExecutor commercetoolsQueryExecutor,
            final PaymentDispatcher paymentDispatcher,
            final NotificationDispatcher notificationDispatcher,
            final ResultProcessor resultProcessor) {
        this.commercetoolsQueryExecutor = commercetoolsQueryExecutor;
        this.paymentDispatcher = paymentDispatcher;
        this.typeBuilder = typeBuilder;
        this.notificationDispatcher = notificationDispatcher;
        this.resultProcessor = resultProcessor;
    }

    public void start() {
        createCustomTypes();

        Spark.port(port());

        Spark.get("/commercetools/handle/payments/:id", (req, res) -> {
            try {
                final PaymentWithCartLike payment = commercetoolsQueryExecutor.getPaymentWithCartLike(req.params("id"));
                try {
                    final PaymentWithCartLike result = paymentDispatcher.dispatchPayment(payment);
                    resultProcessor.process(result, res);
                } catch (final ConcurrentModificationException cme) {
                    try {
                        // TODO Better
                        for (int i = 0; i < 10; i++) {
                            Thread.sleep(100);
                            final PaymentWithCartLike paymentWithCartLike = commercetoolsQueryExecutor.getPaymentWithCartLike(req.params("id"));
                            if (paymentWithCartLike.getPayment().getVersion() > payment.getPayment().getVersion()) {
                                resultProcessor.process(paymentWithCartLike, res);
                                break;
                            }
                        }
                    } catch (final Exception e) {
                        logExceptionInResponse(res, e);
                    }
                } catch (final Exception e) {
                    logExceptionInResponse(res, e);
                }
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

            notificationDispatcher.dispatchNotification(notification);
            res.status(501);
            return "Currently not implemented";
        });

        Spark.awaitInitialization();
    }

    private void logExceptionInResponse(Response res, Exception e) {
        // TODO jw: we probably need to differentiate depending on the exception type
        res.status(500);
        res.type("text/plain; charset=utf-8");
        res.body(String.format("Sorry, but you hit us between the eyes. Cause: %s", e.getMessage()));
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
