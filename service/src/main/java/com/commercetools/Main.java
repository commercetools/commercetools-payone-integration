package com.commercetools;

import com.commercetools.pspadapter.payone.*;
import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import org.quartz.CronScheduleBuilder;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;

import static com.commercetools.pspadapter.payone.util.PayoneConstants.PAYONE;
import static java.lang.String.format;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws SchedulerException, MalformedURLException {

        final PropertyProvider propertyProvider = new PropertyProvider();
        final ServiceConfig serviceConfig = new ServiceConfig(propertyProvider, new PayoneConfig(propertyProvider));
        final ServiceFactory serviceFactory = new ServiceFactory(serviceConfig, PAYONE);

        final IntegrationService integrationService = new IntegrationService(
                serviceFactory.getCustomTypeBuilder(),
                serviceFactory.getCommercetoolsQueryExecutor(),
                serviceFactory.getPaymentDispatcher(),
                serviceFactory.getNotificationDispatcher(),
                serviceFactory.getPayoneInterfaceName());

        integrationService.start();

        ScheduledJobFactory scheduledJobFactory = new ScheduledJobFactory();

        scheduledJobFactory.setAllScheduledItemsStartedListener(() ->
                LOG.info(format("%n%1$s%nPayone Integration Service is STARTED%n%1$s",
                "============================================================"))
        );

        scheduledJobFactory.createScheduledJob(
                CronScheduleBuilder.cronSchedule(serviceConfig.getScheduledJobCronForShortTimeFramePoll()),
                ScheduledJobShortTimeframe.class,
                integrationService,
                serviceFactory.getPaymentDispatcher());

        scheduledJobFactory.createScheduledJob(
                CronScheduleBuilder.cronSchedule(serviceConfig.getScheduledJobCronForLongTimeFramePoll()),
                ScheduledJobLongTimeframe.class,
                integrationService,
                serviceFactory.getPaymentDispatcher());
    }
}
