package com.commercetools.pspadapter.payone.config;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.pspadapter.payone.config.PropertyProvider.*;
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
    private final String applicationName;
    private final String applicationVersion;
    private final List<String> personalDataToRemove;

    /**
     * Initializes the configuration.
     *
     * @param propertyProvider to get the parameters from
     * @throws IllegalStateException if a mandatory parameter is undefined or empty
     */
    public ServiceConfig(final PropertyProvider propertyProvider) {

        this.tenants = getMandatoryTenantNames(propertyProvider);

        this.applicationName = propertyProvider.getMandatoryNonEmptyProperty(PAYONE_INTEGRATOR_NAME);
        this.applicationVersion = propertyProvider.getMandatoryNonEmptyProperty(PAYONE_INTEGRATOR_VERSION);

        this.scheduledJobCronShortTimeFrame =
                propertyProvider.getProperty(SHORT_TIME_FRAME_SCHEDULED_JOB_CRON)
                        .map(String::valueOf)
                        .orElse("0/30 * * * * ? *");
        this.scheduledJobCronLongTimeFrame =
                propertyProvider.getProperty(LONG_TIME_FRAME_SCHEDULED_JOB_CRON)
                        .map(String::valueOf)
                        .orElse("5 0 0/1 * * ? *");

        this.personalDataToRemove = propertyProvider.getProperty(PERSONAL_DATA_TO_REMOVE)
                .map(dataString ->
                        Stream.of(dataString.trim().split("\\s*(,|;)\\s*"))
                        .distinct()
                        .collect(Collectors.toList())
                )
                .orElse(Collections.emptyList());
    }

    /**
     * @return Non-empty list of tenant names, specified in {@code TENANTS} configuration property.
     */
    public List<String> getTenants() {
        return tenants;
    }

    @Nonnull
    public String getApplicationName() {
        return applicationName;
    }

    @Nonnull
    public String getApplicationVersion() {
        return applicationVersion;
    }

    /**
     * Gets the
     * <a href="http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger">QUARTZ cron expression</a>
     * for polling commercetools messages with a shorter time frame.
     *
     * @return the cron expression
     */
    @Nonnull
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
    @Nonnull
    public String getScheduledJobCronForLongTimeFramePoll() {
        return scheduledJobCronLongTimeFrame;
    }

    /**
     * Gets the array of key names that should be removed from a map before logging this map.
     */
    @Nonnull
    public List<String> getPersonalDataToRemove() {
        return personalDataToRemove;
    }

    /**
     * Split comma or semicolon separated list of the tenants names.
     * <p>
     * All trailing/leading whitespaces are ignored.
     *
     * @param propertyProvider property provider which supplies {@link PropertyProvider#TENANTS} value.
     * @return non-null non-empty list of tenants.
     * @throws IllegalStateException if the property can't be parsed or the parsed list is empty, or one of the values
     *                               is empty (e.g., list of "a, , b").
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
