package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.google.common.base.Joiner;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.queries.PaymentByIdGet;
import spark.Spark;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class IntegrationService {

    final Joiner joiner = Joiner.on('\n');

    private final CommercetoolsQueryExecutor commercetoolsQueryExecutor;
    private final PaymentDispatcher dispatcher;
    private final CustomTypeBuilder typeBuilder;

    IntegrationService(final CommercetoolsQueryExecutor commercetoolsQueryExecutor, final PaymentDispatcher dispatcher, final CustomTypeBuilder typeBuilder) {
        this.commercetoolsQueryExecutor = commercetoolsQueryExecutor;
        this.dispatcher = dispatcher;
        this.typeBuilder = typeBuilder;
    }

    public void start() {
        createCustomTypes();

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
            final PaymentWithCartLike payment = commercetoolsQueryExecutor.getPaymentWithCartLike(req.params("id"));
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

    private void createCustomTypes() {
        typeBuilder.run();
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
