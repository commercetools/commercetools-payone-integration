package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.google.common.base.Strings;
import spark.Spark;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class IntegrationService {

    public static final String HEROKU_ASSIGNED_PORT = "PORT";

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
            try {
                DispatchResult result = dispatcher.dispatchPayment(payment);
                res.status(result.getStatusCode());
                return result.getMessage();
            } catch (Exception e) {
                res.status(500);
                return e.getMessage();
            }
        });

        Spark.post("/payone/notification", (req, res) -> {
            res.status(501);
            return "Currently not implemented";
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
