package com.commercetools.pspadapter.payone.config;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 * Provides the configuration of the integration service.
 *
 * @author fhaertig
 * @author Jan Wolter
 * @since 02.12.15
 */
public class ServiceConfig {


    private final List<String> tenants;

    private final String scheduledJobCronShortTimeFrame;
    private final String scheduledJobCronLongTimeFrame;

    /**
     * Initializes the configuration.
     *
     * @param propertyProvider to get the parameters from
     * @throws IllegalStateException if a mandatory parameter is undefined or empty
     */
    public ServiceConfig(final PropertyProvider propertyProvider) {

        this.tenants = getMandatoryTenantNames(propertyProvider);

        this.scheduledJobCronShortTimeFrame =
                propertyProvider.getProperty(PropertyProvider.SHORT_TIME_FRAME_SCHEDULED_JOB_CRON)
                        .map(String::valueOf)
                        .orElse("0/30 * * * * ? *");
        this.scheduledJobCronLongTimeFrame =
                propertyProvider.getProperty(PropertyProvider.LONG_TIME_FRAME_SCHEDULED_JOB_CRON)
                        .map(String::valueOf)
                        .orElse("5 0 0/1 * * ? *");
    }

    /**
     * @return Non-empty list of tenant names, specified in {@code TENANTS} configuration property.
     */
    public List<String> getTenants() {
        return tenants;
    }

    /**
     * Gets the
     * <a href="http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger">QUARTZ cron expression</a>
     * for polling commercetools messages with a shorter time frame.
     *
     * @return the cron expression
     */
    public String getScheduledJobCronForShortTimeFramePoll() {
        return scheduledJobCronShortTimeFrame;
    }

    /**
     * Gets the
     * <a href="http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger">QUARTZ cron expression</a>
     * for polling commercetools messages with a longer time frame.
     *
     * @return the cron expression
     */
    public String getScheduledJobCronForLongTimeFramePoll() {
        return scheduledJobCronLongTimeFrame;
    }

    /**
     * Split comma or semicolon separated list of the tenants names.
     * <p>
     * All trailing/leading whitespaces are ignored.
     * @param propertyProvider property provider which supplies {@link PropertyProvider#TENANTS} value.
     * @return non-null non-empty list of tenants.
     * @throws IllegalStateException if the property can't be parsed or the parsed list is empty, or one of the values
     * is empty (e.g., list of "a, , b").
     */
    private List<String> getMandatoryTenantNames(PropertyProvider propertyProvider) {
        final String tenantsList = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.TENANTS);
        final List<String> result = asList(tenantsList.trim().split("\\s*(,|;)\\s*"));

        if (result.size() < 1 || result.stream().anyMatch(StringUtils::isBlank)) {
            throw new IllegalStateException(format("Tenants list is invalid, pls check \"%s\" variable",
                    PropertyProvider.TENANTS));
        }

        return result;
    }
}
