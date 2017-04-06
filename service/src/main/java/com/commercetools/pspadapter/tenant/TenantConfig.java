package com.commercetools.pspadapter.tenant;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import io.sphere.sdk.client.SphereClientConfig;

import java.util.Objects;

import static com.commercetools.pspadapter.tenant.TenantPropertyProvider.*;

public class TenantConfig {

    private final String name;

    private final PayoneConfig payoneConfig;

    private final boolean startFromScratch;

    private final boolean updateOrderPaymentState;

    private final String secureKey;

    private final SphereClientConfig sphereClientConfig;

    public TenantConfig(TenantPropertyProvider tenantPropertyProvider, PayoneConfig payoneConfig) {
        this.name = tenantPropertyProvider.getTenantName();


        this.sphereClientConfig = SphereClientConfig.of(
                tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(CT_PROJECT_KEY),
                tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(CT_CLIENT_ID),
                tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(CT_CLIENT_SECRET));

        this.payoneConfig = payoneConfig;

        this.startFromScratch = tenantPropertyProvider.getTenantProperty(CT_START_FROM_SCRATCH)
                .map(Boolean::valueOf)
                .orElse(false);

        this.secureKey = tenantPropertyProvider.getTenantProperty(SECURE_KEY)
                .map(String::valueOf)
                .orElse("");

        updateOrderPaymentState = tenantPropertyProvider.getTenantProperty(UPDATE_ORDER_PAYMENT_STATE)
                .map(String::trim)
                .map(Boolean::valueOf)
                .orElse(false);
    }

    public String getName() {
        return name;
    }

    public PayoneConfig getPayoneConfig() {
        return payoneConfig;
    }

    /**
     * Gets the flag indicating whether the service shall reset the commerctools project at start up.
     *
     * @return whether the commercetools project shall be reset
     */
    public boolean getStartFromScratch() {
        return startFromScratch;
    }

    /**
     * If <b>true</b> - when processing Payone notification (update payment status) {@link io.sphere.sdk.orders.Order#getPaymentState()},
     * linked to the payment, should be update also. Otherwise leave the order payment state unchanged.
     * <p>
     * By default it is <b>false</b>
     *
     * @return <b>true</b> if environment the property {@link TenantPropertyProvider#UPDATE_ORDER_PAYMENT_STATE} is a string
     * <i>true</i> case insensitive, <b>false</b> otherwise.
     */
    public boolean isUpdateOrderPaymentState() {
        return updateOrderPaymentState;
    }

    /**
     * Gets the secure key which was used for encrypting data with Blowfish.
     *
     * @return non-null secure key as plain text. Empty string if not defined.
     */
    public String getSecureKey() {
        return secureKey;
    }

    public SphereClientConfig getSphereClientConfig() {
        return sphereClientConfig;
    }
}
