package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.google.common.base.Joiner;
import io.sphere.sdk.payments.Payment;
import spark.Spark;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class IntegrationService {

    final Joiner joiner = Joiner.on('\n');

    private final CommercetoolsQueryExecutor commercetoolsQueryExecutor;
    private final PaymentDispatcher dispatcher;

    IntegrationService(final CommercetoolsQueryExecutor commercetoolsQueryExecutor, final PaymentDispatcher dispatcher) {
        this.commercetoolsQueryExecutor = commercetoolsQueryExecutor;
        this.dispatcher = dispatcher;
    }

    public void start() {
        Spark.port(port());

        Spark.get("/commercetools/handle/payment/:id", (req, res) -> {
            final int status = 403;
            res.status(status);
            res.type("text/json");
            return joiner.join(
                    "{",
                    String.format("\"statusCode\": %d,", status),
                    "\"message\": \"Not implemented, yet.\"",
                    "}");
        });


        Spark.get("/handle/:id", (req, res) -> {
            final Payment payment = commercetoolsQueryExecutor.getPaymentById(req.params("id"));
            // TODO: Properly handle dispatch-result
            try {
                dispatcher.dispatchPayment(payment);
                return "";
            } catch (Exception e) {
                res.status(500);
                return "";
            }
        });

        Spark.awaitInitialization();
    }

    public void stop() {
        Spark.stop();
    }

    public int port() {
        return 8080;
    }

    public CommercetoolsQueryExecutor getCommercetoolsQueryExecutor() {
        return commercetoolsQueryExecutor;
    }
}
