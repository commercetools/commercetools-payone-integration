package com.commercetools;

import ch.qos.logback.access.jetty.RequestLogImpl;
import ch.qos.logback.classic.Level;
import com.commercetools.pspadapter.payone.IntegrationService;
import com.commercetools.pspadapter.payone.ServiceFactory;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.util.spark.JettyServerWithRequestLogFactory;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import spark.embeddedserver.EmbeddedServerFactory;
import spark.embeddedserver.EmbeddedServers;
import spark.embeddedserver.jetty.EmbeddedJettyFactory;

import static ch.qos.logback.classic.Level.toLevel;


public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    /**
     * It is recommended to run this service using {@code ./gradlew :service:run}
     * or {@code ./gradlew :service:runShadow} command - this will parse mandatory environment
     * variables from {@code gradle.properties} file.
     * Most of IDE should support Run/Debug configuration to run such gradle tasks.
     * <p>
     * See more in {@code Project-Lifecycle.md} documentation.
     *
     * @param args default command line args (ignored so far)
     */
    public static void main(String[] args)  {

        final PropertyProvider propertyProvider = new PropertyProvider();
        final ServiceConfig serviceConfig = new ServiceConfig(propertyProvider);

        bridgeJULToSLF4J();
        configureAccessLogs();
        configureLogLevel(serviceConfig);

        final IntegrationService integrationService = ServiceFactory.createIntegrationService(propertyProvider, serviceConfig);
        integrationService.start();
    }

    private static void configureLogLevel(ServiceConfig serviceConfig) {
        if (serviceConfig.getLoglevel().isPresent()) {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.setLevel(toLevel(serviceConfig.getLoglevel().get(), Level.INFO));
            LOG.info(String.format("The Service is running on %s mode.", serviceConfig.getLoglevel().get()));
        }
    }

    private static void configureAccessLogs() {
        final RequestLogImpl requestLog = new RequestLogImpl();
        requestLog.setFileName(Resources.getResource("logback-access.xml").getPath());
        requestLog.start();
        final JettyServerWithRequestLogFactory serverFactory = new JettyServerWithRequestLogFactory(requestLog);
        final EmbeddedServerFactory embeddedServerFactory = new EmbeddedJettyFactory(serverFactory);
        EmbeddedServers.add(EmbeddedServers.Identifiers.JETTY, embeddedServerFactory);
    }

    /**
     * Routes all incoming j.u.l. (java.util.logging.Logger) records to the SLF4j API. This is done by:
     * <ol>
     *     <li>Removing existing handlers attached to the j.u.l root logger.</li>
     *     <li>Adding SLF4JBridgeHandler to j.u.l's root logger.</li>
     * </ol>
     * <p>Why we do the routing?
     * <p>Some dependencies (e.g. org.javamoney.moneta's DefaultMonetaryContextFactory) log events using the
     * j.u.l. This causes such logs to ignore the logback.xml configuration which is only
     * applied to logs from the SLF4j implementation.
     *
     */
    private static void bridgeJULToSLF4J() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
}
