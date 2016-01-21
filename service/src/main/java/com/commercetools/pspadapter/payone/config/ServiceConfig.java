package com.commercetools.pspadapter.payone.config;

/**
 * Provides the configuration of the integration service.
 *
 * @author fhaertig
 * @author Jan Wolter
 * @since 02.12.15
 */
public class ServiceConfig {

    private final String ctProjectKey;
    private final String ctClientId;
    private final String ctClientSecret;
    private final boolean startFromScratch;
    private final String scheduledJobCronShortTimeFrame;
    private final String scheduledJobCronLongTimeFrame;

    /**
     * Initializes the configuration.
     *
     * @param propertyProvider to get the parameters from
     *
     * @throws IllegalStateException if a mandatory parameter is undefined or empty
     */
    public ServiceConfig(final PropertyProvider propertyProvider) {
        ctProjectKey = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_PROJECT_KEY);
        ctClientId  = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_CLIENT_ID);
        ctClientSecret = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_CLIENT_SECRET);
        scheduledJobCronShortTimeFrame =
                propertyProvider.getProperty(PropertyProvider.SHORT_TIME_FRAME_SCHEDULED_JOB_CRON)
                        .map(String::valueOf)
                        .orElse("0/30 * * * * ? *");
        scheduledJobCronLongTimeFrame =
                propertyProvider.getProperty(PropertyProvider.LONG_TIME_FRAME_SCHEDULED_JOB_CRON)
                        .map(String::valueOf)
                        .orElse("5 0 0/1 * * ? *");
        startFromScratch = propertyProvider.getProperty(PropertyProvider.CT_START_FROM_SCRATCH)
                .map(Boolean::valueOf)
                .orElse(false);
    }

    /**
     * Gets the commercetools project key.
     * @return the key of the commercetools project to connect with
     */
    public String getCtProjectKey() {
        return ctProjectKey;
    }

    /**
     * Gets the commercetools client ID.
     * @return the client ID of the commercetools project to connect with
     */
    public String getCtClientId() {
        return ctClientId;
    }

    /**
     * Gets the commercetools client secret.
     * @return the client password for the commercetools project to connect with
     */
    public String getCtClientSecret() {
        return ctClientSecret;
    }

    /**
     * Gets the
     * <a href="http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger">QUARTZ cron expression</a>
     * for polling commercetools messages with a shorter time frame.
     * @return the cron expression
     */
    public String getScheduledJobCronForShortTimeFramePoll() {
        return scheduledJobCronShortTimeFrame;
    }

    /**
     * Gets the
     * <a href="http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger">QUARTZ cron expression</a>
     * for polling commercetools messages with a longer time frame.
     * @return the cron expression
     */
    public String getScheduledJobCronForLongTimeFramePoll() {
        return scheduledJobCronLongTimeFrame;
    }

    /**
     * Gets the flag indicating whether the service shall reset the commerctools project at start up.
     * @return whether the commercetools project shall be reset
     */
    public boolean getStartFromScratch() {
        return startFromScratch;
    }
}
