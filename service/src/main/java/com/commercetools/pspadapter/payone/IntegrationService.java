package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.notification.NotificationDispatcher;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.pspadapter.tenant.TenantFactory;
import com.commercetools.pspadapter.tenant.TenantPropertyProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import spark.Spark;
import spark.utils.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.commercetools.pspadapter.payone.util.PayoneConstants.PAYONE;
import static com.commercetools.util.CorrelationIdUtil.attachFromRequestOrGenerateNew;
import static io.sphere.sdk.json.SphereJsonUtils.toJsonString;
import static io.sphere.sdk.json.SphereJsonUtils.toPrettyJsonString;
import static java.util.stream.Collectors.toList;

/**
 * @author fhaertig
 * @author Jan Wolter
 */
public class IntegrationService {

    public static final Logger LOG = LoggerFactory.getLogger(IntegrationService.class);
    static final int SUCCESS_STATUS = HttpStatus.OK_200;
    private static final String STATUS_KEY = "status";
    private static final String APPLICATION_INFO_KEY = "applicationInfo";


    private static final String HEROKU_ASSIGNED_PORT = "PORT";
    private List<TenantFactory> tenantFactories = null;
    private ServiceConfig serviceConfig = null;

    /**
     * This constructor is only used for testing proposes
     */
    public IntegrationService(@Nonnull final ServiceConfig config, List<TenantFactory> factories) {
        this.serviceConfig = config;
        this.tenantFactories = factories;
    }


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

    }

    private static void initTenantServiceResources(final TenantFactory tenantFactory) {

        // create custom types
        if (tenantFactory.getCustomTypeBuilder() != null) {
            tenantFactory.getCustomTypeBuilder().run();
        }
        PaymentHandler paymentHandler = tenantFactory.getPaymentHandler();
        NotificationDispatcher notificationDispatcher = tenantFactory.getNotificationDispatcher();

        // register payment handler URL
        String paymentHandlerUrl = tenantFactory.getPaymentHandlerUrl();
        if (StringUtils.isNotEmpty(paymentHandlerUrl)) {
            LOG.info("Register payment handler URL {}", paymentHandlerUrl);
            Spark.get(paymentHandlerUrl, (req, res) -> {
                    final PayoneResult payoneResult = paymentHandler.handlePayment(req.params("id"));
                    if (!payoneResult.body().isEmpty()) {
                        LOG.debug("--> Result body of ${getTenantName()}/commercetools/handle/payments/{}: {}",
                            req.params("id"), payoneResult.body());
                    }
                    res.status(payoneResult.statusCode());
                    return res;
                },
                new HandlePaymentResponseTransformer());
        }
        // register start Session URL
        String startSessionUrl = tenantFactory.getPayoneStartSessionUrl();
        if (StringUtils.isNotEmpty(startSessionUrl)) {
            LOG.info("Register start session URL {}", startSessionUrl);
            KlarnaStartSessionHandler klarnaStartSessionHandler = tenantFactory.getSessionHandler();
            Spark.get(startSessionUrl, (req, res) -> {
                        final PayoneResult payoneResult = klarnaStartSessionHandler.startSession(req.params("id"));
                        if (!payoneResult.body().isEmpty()) {
                            LOG.debug("--> Result body of ${getTenantName()}/commercetools/start/session/{}: {}",
                                    req.params("id"), payoneResult.body());
                        }
                        res.status(payoneResult.statusCode());
                        res.body(payoneResult.body());
                        return res;
                    },
                    new HandlePaymentResponseTransformer());
        }

        // register Payone notifications URL
        String payoneNotificationUrl = tenantFactory.getPayoneNotificationUrl();
        if (StringUtils.isNotEmpty(payoneNotificationUrl)) {
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
        injectCorrelationIdIntoContext();

        final Map<String, Object> healthResponse = createHealthResponse(serviceConfig);
        final String healthRequestContent = toJsonString(healthResponse);
        final String healthRequestPrettyContent = toPrettyJsonString(healthResponse);

        // This is a temporary jerry-rig for the load balancer to check connection with the service itself.
        // For now it just returns a JSON response with status code, tenants list and static application info.
        // It should be expanded to a more real health-checker service, which really performs PAYONE status check.
        // But don't forget, a load balancer may call this URL very often (like 1 per sec),
        // so don't make this request processor heavy, or implement is as independent background service.
        LOG.info("Register /health URL");
        LOG.info("Use /health?pretty to pretty-print output JSON");
        Spark.get("/health", (req, res) -> {
            res.status(SUCCESS_STATUS);
            res.type(ContentType.APPLICATION_JSON.getMimeType());
            return req.queryParams("pretty") != null ? healthRequestPrettyContent : healthRequestContent;
        });
    }

    private void injectCorrelationIdIntoContext() {
        Spark.before(((request, response) -> attachFromRequestOrGenerateNew(request)));
        Spark.after(((request, response) -> MDC.clear()));
    }

    public void stop() {
        Spark.stop();
    }

    public int port() {
        final String environmentVariable = System.getenv(HEROKU_ASSIGNED_PORT);
        if (!StringUtils.isBlank(environmentVariable)) {
            return Integer.parseInt(environmentVariable);
        }

        final String systemProperty = System.getProperty(HEROKU_ASSIGNED_PORT, "8080");
        return Integer.parseInt(systemProperty);
    }

    private Map<String, Object> createHealthResponse(@Nonnull final ServiceConfig serviceConfig) {
        final Map<String, String> applicationInfo = new LinkedHashMap<>();
        applicationInfo.put("version", serviceConfig.getApplicationVersion());
        applicationInfo.put("title", serviceConfig.getApplicationName());

        final Map<String, Object> healthResponse = new LinkedHashMap<>();
        healthResponse.put(STATUS_KEY, SUCCESS_STATUS);
        healthResponse.put(APPLICATION_INFO_KEY, applicationInfo);

        return healthResponse;
    }
}
