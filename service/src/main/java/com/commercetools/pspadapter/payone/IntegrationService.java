package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.notification.NotificationDispatcher;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.pspadapter.tenant.TenantFactory;
import com.commercetools.pspadapter.tenant.TenantPropertyProvider;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.payments.queries.PaymentQuery;
import org.apache.http.entity.ContentType;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;
import spark.utils.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static com.commercetools.pspadapter.payone.util.PayoneConstants.PAYONE;
import static io.sphere.sdk.json.SphereJsonUtils.toJsonString;
import static io.sphere.sdk.json.SphereJsonUtils.toPrettyJsonString;
import static io.sphere.sdk.utils.CompletableFutureUtils.listOfFuturesToFutureOfList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @author fhaertig
 * @author Jan Wolter
 */
public class IntegrationService {

    public static final Logger LOG = LoggerFactory.getLogger(IntegrationService.class);

    private static final String HEROKU_ASSIGNED_PORT = "PORT";
    private static final int ERROR_STATUS = HttpStatus.SERVICE_UNAVAILABLE_503;
    private final List<TenantFactory> tenantFactories;
    private ServiceConfig serviceConfig = null;


    public IntegrationService(@Nonnull final ServiceConfig config,
                              @Nonnull final PropertyProvider propertyProvider) {
        this.serviceConfig = config;
        this.tenantFactories = serviceConfig.getTenants().stream()
                .map(tenantName -> new TenantPropertyProvider(tenantName, propertyProvider))
                .map(tenantPropertyProvider -> new TenantConfig(tenantPropertyProvider,
                        new PayoneConfig(tenantPropertyProvider)))
                .map(tenantConfig -> new TenantFactory(PAYONE, tenantConfig))
                .collect(toList());

        if (CollectionUtils.isEmpty(this.tenantFactories)) {
            throw new IllegalArgumentException("Tenants list must be non-empty");
        }

        ImmutableMap<String, Object> healthResponse = createHealthResponse(serviceConfig);
    }

    private static void initTenantServiceResources(TenantFactory tenantFactory) {

        // create custom types
        tenantFactory.getCustomTypeBuilder().run();

        PaymentHandler paymentHandler = tenantFactory.getPaymentHandler();
        NotificationDispatcher notificationDispatcher = tenantFactory.getNotificationDispatcher();

        // register payment handler URL
        String paymentHandlerUrl = tenantFactory.getPaymentHandlerUrl();
        LOG.info("Register payment handler URL {}", paymentHandlerUrl);
        Spark.get(paymentHandlerUrl, (req, res) -> {
            final PaymentHandleResult paymentHandleResult = paymentHandler.handlePayment(req.params("id"));
            if (!paymentHandleResult.body().isEmpty()) {
                LOG.debug("--> Result body of ${getTenantName()}/commercetools/handle/payments/{}: {}", req.params(
                        "id"), paymentHandleResult.body());
            }
            res.status(paymentHandleResult.statusCode());
            return res;
        }, new HandlePaymentResponseTransformer());

        // register Payone notifications URL
        String payoneNotificationUrl = tenantFactory.getPayoneNotificationUrl();
        LOG.info("Register payone notification URL {}", payoneNotificationUrl);
        Spark.post(payoneNotificationUrl, (req, res) -> {
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
        // For now it just returns a JSON response with status code, tenants list and static application info.
        // It should be expanded to a more real health-checker service, which really performs PAYONE status check.
        // But don't forget, a load balancer may call this URL very often (like 1 per sec),
        // so don't make this request processor heavy, or implement is as independent background service.
        LOG.info("Register /health URL");
        LOG.info("Use /health?pretty to pretty-print output JSON");
        Spark.get("/health", (req, res) -> {
            ImmutableMap<String, Object> healthResponse = createHealthResponse(serviceConfig);
            res.status((Integer) healthResponse.getOrDefault("status", ERROR_STATUS));
            res.type(ContentType.APPLICATION_JSON.getMimeType());

            return req.queryParams("pretty") != null ? toPrettyJsonString(healthResponse) :
                    toJsonString(healthResponse);
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

    private ImmutableMap<String, Object> createHealthResponse(@Nonnull final ServiceConfig serviceConfig) {
        final ImmutableMap<String, String> applicationInfo = ImmutableMap.of(
                "version", serviceConfig.getApplicationVersion(),
                "title", serviceConfig.getApplicationName());


        Map<String, CompletionStage<Integer>> tenantMap = checkTenantStati(tenantFactories);
        //resolve all completable stages
        listOfFuturesToFutureOfList(new ArrayList<>(tenantMap.values()));
        //unpack completable features
        Map<String, Integer> statusMap = tenantMap.keySet().stream()
                .collect(toMap(v -> v, v -> tenantMap.get(v).toCompletableFuture().join()));


        return ImmutableMap.of(
                "status", !statusMap.containsKey(ERROR_STATUS) ? HttpStatus.OK_200 : ERROR_STATUS,
                "tenants", statusMap,
                "applicationInfo", applicationInfo);
    }

    private Map<String, CompletionStage<Integer>> checkTenantStati(List<TenantFactory> tenants) {
        return tenants.stream().collect(toMap(TenantFactory::getTenantName, tenantFactory -> {
                    return tenantFactory.getBlockingSphereClient().
                            execute(PaymentQuery.of().withLimit(0l))
                            .thenApply(result -> {
                                        int status = HttpStatus.OK_200;
                                        final String tenantName = tenantFactory.getTenantName();
                                        try {
                                            result.getCount();
                                        } catch (Exception e) {
                                            LOG.error("Cannot query payments for the tenant {}", tenantName, e);
                                            status = ERROR_STATUS;
                                        }
                                        return status;
                                    }
                            );
                }
        ));
    }
}