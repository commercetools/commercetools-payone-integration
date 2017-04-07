package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.notification.NotificationDispatcher;
import com.commercetools.pspadapter.tenant.TenantFactory;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.http.HttpStatusCode;
import io.sphere.sdk.json.SphereJsonUtils;
import org.apache.http.entity.ContentType;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;
import spark.utils.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * @author fhaertig
 * @author Jan Wolter
 */
public class IntegrationService {

    public static final Logger LOG = LoggerFactory.getLogger(IntegrationService.class);

    public static final String HEROKU_ASSIGNED_PORT = "PORT";

    private final List<TenantFactory> tenantFactories;

    public IntegrationService(final List<TenantFactory> tenantFactories) {
        if (CollectionUtils.isEmpty(tenantFactories)) {
            throw new IllegalArgumentException("Tenants list must be non-empty");
        }

        this.tenantFactories = tenantFactories;
    }

    /**
     * @return Unmodifiable view of tenant factories list which are used for the service run.
     */
    public List<TenantFactory> getTenantFactories() {
        return Collections.unmodifiableList(tenantFactories);
    }

    public void start() {

        initSparkService();

        for (TenantFactory tenantFactory : tenantFactories) {
            initTenantServiceResources(tenantFactory);
        }

        Spark.awaitInitialization();
    }

    private void initSparkService() {
        Spark.port(port());

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
    }

    private static void initTenantServiceResources(TenantFactory tenantFactory) {

        // create custom types
        tenantFactory.getCustomTypeBuilder().run();

        PaymentHandler paymentHandler = tenantFactory.getPaymentHandler();
        NotificationDispatcher notificationDispatcher = tenantFactory.getNotificationDispatcher();

        // register payment handler URL
        Spark.get(tenantFactory.getPaymentHandlerUrl(), (req, res) -> {
            final PaymentHandleResult paymentHandleResult = paymentHandler.handlePayment(req.params("id"));
            if (!paymentHandleResult.body().isEmpty()) {
                LOG.debug("--> Result body of handle/payments/{}: {}", req.params("id"), paymentHandleResult.body());
            }
            res.status(paymentHandleResult.statusCode());
            return res;
        }, new HandlePaymentResponseTransformer());

        // register Payone notifications URL
        Spark.post(tenantFactory.getPayoneNotificationUrl(), (req, res) -> {
            LOG.debug("<- Received POST from Payone: {}", req.body());
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
                LOG.error("Payone notification handling error. Request body: {}", req.body(), e);
                res.status(400);
                return "Payone notification handling error. See the logs. Requested body: " + req.body();
            }
            res.status(200);
            return "TSOK";
        });
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
}
