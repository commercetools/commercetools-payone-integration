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

    public ServiceConfig() {
        this(new PropertyProvider());
    }

    public ServiceConfig(final PropertyProvider propertyProvider) {
        ctProjectKey = propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.CT_PROJECT_KEY)
                .filter(s -> !s.isEmpty())
                .orElseThrow(() -> propertyProvider.createIllegalArgumentException(PropertyProvider.CT_PROJECT_KEY));

        ctClientId  = propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.CT_CLIENT_ID)
                .filter(s -> !s.isEmpty())
                .orElseThrow(() -> propertyProvider.createIllegalArgumentException(PropertyProvider.CT_CLIENT_ID));

        ctClientSecret = propertyProvider.getEnvironmentOrSystemValue(PropertyProvider.CT_CLIENT_SECRET)
                .filter(s -> !s.isEmpty())
                .orElseThrow(() -> propertyProvider.createIllegalArgumentException(PropertyProvider.CT_CLIENT_SECRET));

        payoneConfig = new PayoneConfig(propertyProvider);
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
        return "0/5 * * * * ?";
    }

    public PayoneConfig getPayoneConfig() {
        return payoneConfig;
    }

}
