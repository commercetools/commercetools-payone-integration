package com.commercetools.pspadapter.tenant;

import com.commercetools.pspadapter.payone.config.PropertyProvider;

import java.util.Optional;

public class TenantPropertyProvider {

    public static final String PAYONE_SUBACC_ID = "PAYONE_SUBACC_ID";
    public static final String PAYONE_MERCHANT_ID = "PAYONE_MERCHANT_ID";
    public static final String PAYONE_PORTAL_ID = "PAYONE_PORTAL_ID";
    public static final String PAYONE_KEY = "PAYONE_KEY";
    public static final String PAYONE_MODE = "PAYONE_MODE";

    public static final String CT_PROJECT_KEY = "CT_PROJECT_KEY";
    public static final String CT_CLIENT_ID = "CT_CLIENT_ID";
    public static final String CT_CLIENT_SECRET = "CT_CLIENT_SECRET";

    public static final String CT_START_FROM_SCRATCH = "CT_START_FROM_SCRATCH";

    public static final String SECURE_KEY = "SECURE_KEY";
    public static final String UPDATE_ORDER_PAYMENT_STATE = "UPDATE_ORDER_PAYMENT_STATE";

    private final String tenantName;

    private final String tenantPropertyPrefix;

    private final PropertyProvider propertyProvider;

    public TenantPropertyProvider(String tenantName, PropertyProvider propertyProvider) {
        this.tenantName = tenantName;
        this.tenantPropertyPrefix = tenantName + "_";
        this.propertyProvider = propertyProvider;
    }

    public String getTenantName() {
        return tenantName;
    }

    /**
     * @return property provider which contains both common application and tenant specific (with tenant name prefix) properties.
     */
    public PropertyProvider getCommonPropertyProvider() {
        return propertyProvider;
    }

    /**
     * @param propertyName property name suffix, without tenant name.
     * @return Optional value of tenant specific property, fetched by {@link #tenantName} and {@code propertyName}
     */
    public Optional<String> getTenantProperty(String propertyName) {
        return propertyProvider.getProperty(getTenantPropertyName(propertyName));
    }

    /**
     * Fetch mandatory tenant specific property. If property doesn't exist or empty -
     * {@link IllegalStateException} is thrown.
     *
     * @param propertyName property name suffix, without tenant name.
     * @return non-null value of tenant specific property, fetched by {@link #tenantName} and {@code propertyName}
     *
     * @throws IllegalStateException if the property isn't defined or empty
     */
    public String getTenantMandatoryNonEmptyProperty(String propertyName) {
        return propertyProvider.getMandatoryNonEmptyProperty(getTenantPropertyName(propertyName));
    }

    private String getTenantPropertyName(String propertyName) {
        return tenantPropertyPrefix + propertyName;
    }
}
