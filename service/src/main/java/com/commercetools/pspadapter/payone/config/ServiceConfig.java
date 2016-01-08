package com.commercetools.pspadapter.payone.config;

/**
 * Provides the configuration of the integration service.
 *
 * @author fhaertig
 * @author Jan Wolter
 * @date 02.12.15
 */
public class ServiceConfig {

    private final String ctProjectKey;
    private final String ctClientId;
    private final String ctClientSecret;
    private final boolean startFromScratch;

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
     * Gets the cron expression for polling commercetools messages.
     * @return the cron expression
     */
    public String getCronNotation() {
        // FIXME evaluate environment variable
        return "0/10 * * * * ?";
    }

    /**
     * Gets the flag indicating whether the service shall reset the commerctools project at start up.
     * @return whether the commercetools project shall be reset
     */
    public boolean getStartFromScratch() {
        return startFromScratch;
    }
}
