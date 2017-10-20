package com.commercetools;

import com.commercetools.pspadapter.payone.*;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import org.quartz.CronScheduleBuilder;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;

import static java.lang.String.format;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws SchedulerException, MalformedURLException {

        final PropertyProvider propertyProvider = new PropertyProvider();
        final ServiceConfig serviceConfig = new ServiceConfig(propertyProvider);

        final IntegrationService integrationService = ServiceFactory.createIntegrationService(propertyProvider, serviceConfig);
        integrationService.start();

        ScheduledJobFactory scheduledJobFactory = new ScheduledJobFactory();

        scheduledJobFactory.setAllScheduledItemsStartedListener(() ->
                LOG.info(format("%n%s %n" +
                                "Payone Integration Service is STARTED %n" +
                                "%-10s %s %n" +
                                "%-10s %s %n" +
                                "%s",
                        "============================================================",
                        "Name:", serviceConfig.getApplicationName(),
                        "Version:", serviceConfig.getApplicationVersion(),
                        "============================================================"
                ))
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
}
