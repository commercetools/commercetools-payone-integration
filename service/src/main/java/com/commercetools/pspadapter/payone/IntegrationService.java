package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.google.common.base.Joiner;
import spark.Spark;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class IntegrationService {

    final Joiner joiner = Joiner.on('\n');

    private final CommercetoolsQueryExecutor commercetoolsQueryExecutor;

    IntegrationService(final CommercetoolsQueryExecutor commercetoolsQueryExecutor) {
        this.commercetoolsQueryExecutor = commercetoolsQueryExecutor;
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
