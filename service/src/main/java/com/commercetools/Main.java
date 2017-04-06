package com.commercetools;

import com.commercetools.pspadapter.payone.IntegrationService;
import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.pspadapter.tenant.TenantFactory;
import com.commercetools.pspadapter.tenant.TenantPropertyProvider;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.List;

import static com.commercetools.pspadapter.payone.util.PayoneConstants.PAYONE;
import static java.util.stream.Collectors.toList;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws SchedulerException, MalformedURLException {

        final PropertyProvider propertyProvider = new PropertyProvider();
        final ServiceConfig serviceConfig = new ServiceConfig(propertyProvider);
        //final ServiceFactory serviceFactory = new ServiceFactory(serviceConfig);

        List<TenantFactory> tenantFactories = serviceConfig.getTenants().stream()
                .map(tenantName -> new TenantPropertyProvider(tenantName, propertyProvider))
                .map(tenantPropertyProvider -> new TenantConfig(tenantPropertyProvider, new PayoneConfig(tenantPropertyProvider)))
                .map(tenantConfig -> new TenantFactory(PAYONE, tenantConfig))
                .collect(toList());


        final IntegrationService integrationService = new IntegrationService(tenantFactories);

        integrationService.start();

        LOG.error("SCHEDULERS ARE NOT STARTED");

        // TODO: finalize schedulers

//        ScheduledJobFactory scheduledJobFactory = new ScheduledJobFactory();
//
//        scheduledJobFactory.setAllScheduledItemsStartedListener(() ->
//                LOG.info(format("%n%1$s%nPayone Integration Service is STARTED%n%1$s",
//                "============================================================"))
//        );

//        scheduledJobFactory.createScheduledJob(
//                CronScheduleBuilder.cronSchedule(serviceConfig.getScheduledJobCronForShortTimeFramePoll()),
//                ScheduledJobShortTimeframe.class,
//                integrationService,
//                serviceFactory.getPaymentDispatcher());
//
//        scheduledJobFactory.createScheduledJob(
//                CronScheduleBuilder.cronSchedule(serviceConfig.getScheduledJobCronForLongTimeFramePoll()),
//                ScheduledJobLongTimeframe.class,
//                integrationService,
//                serviceFactory.getPaymentDispatcher());
    }
}
