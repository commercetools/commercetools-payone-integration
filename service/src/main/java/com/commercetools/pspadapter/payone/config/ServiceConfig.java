package com.commercetools.pspadapter.payone.config;

/**
 * @author fhaertig
 * @author Jan Wolter
 * @date 02.12.15
 */
public class ServiceConfig {

    private final String ctProjectKey;
    private final String ctClientId;
    private final String ctClientSecret;
    private final PayoneConfig payoneConfig;
    private final boolean startFromScratch;

    public ServiceConfig(final PropertyProvider propertyProvider) {
        // FIXME jw: pass PayoneConfig into constructor
        payoneConfig = new PayoneConfig(propertyProvider);

        ctProjectKey = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_PROJECT_KEY);
        ctClientId  = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_CLIENT_ID);
        ctClientSecret = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_CLIENT_SECRET);
        startFromScratch = propertyProvider.getProperty(PropertyProvider.CT_START_FROM_SCRATCH)
                .map(Boolean::valueOf)
                .orElse(false);
    }

    public String getCtProjectKey() {
        return ctProjectKey;
    }

    public String getCtClientId() {
        return ctClientId;
    }

    public String getCtClientSecret() {
        return ctClientSecret;
    }

    public String getCronNotation() {
        return "0/10 * * * * ?";
    }

    public PayoneConfig getPayoneConfig() {
        return payoneConfig;
    }

    public boolean getStartFromScratch() {
        return startFromScratch;
    }
}
