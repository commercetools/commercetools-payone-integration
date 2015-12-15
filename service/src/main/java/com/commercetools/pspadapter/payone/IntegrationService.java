package com.commercetools.pspadapter.payone;

import spark.Spark;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class IntegrationService {

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
            final PaymentWithCartLike payment = commercetoolsQueryExecutor.getPaymentWithCartLike(req.params("id"));
            // TODO: Properly handle dispatch-result
            try {
                dispatcher.dispatchPayment(payment);
                res.status(200);
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
