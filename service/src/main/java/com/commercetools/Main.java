package com.commercetools;

import com.commercetools.pspadapter.payone.*;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import org.quartz.CronScheduleBuilder;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static net.logstash.logback.argument.StructuredArguments.value;

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
     * @throws SchedulerException if scheduled quartz jobs can't be started
     */
    public static void main(String[] args) throws SchedulerException {
        bridgeJULToSLF4J();

        final PropertyProvider propertyProvider = new PropertyProvider();
        final ServiceConfig serviceConfig = new ServiceConfig(propertyProvider);

        final IntegrationService integrationService = ServiceFactory.createIntegrationService(propertyProvider, serviceConfig);
        integrationService.start();

        ScheduledJobFactory scheduledJobFactory = new ScheduledJobFactory();

        scheduledJobFactory.setAllScheduledItemsStartedListener(() ->
                LOG.info("{} has started with version {}",
                    value("applicationName", serviceConfig.getApplicationName()),
                    value("version", serviceConfig.getApplicationVersion()))
        );

        scheduledJobFactory.createScheduledJob(
                CronScheduleBuilder.cronSchedule(serviceConfig.getScheduledJobCronForShortTimeFramePoll()),
                ScheduledJobShortTimeframe.class,
                integrationService);

        scheduledJobFactory.createScheduledJob(
                CronScheduleBuilder.cronSchedule(serviceConfig.getScheduledJobCronForLongTimeFramePoll()),
                ScheduledJobLongTimeframe.class,
                integrationService);
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
