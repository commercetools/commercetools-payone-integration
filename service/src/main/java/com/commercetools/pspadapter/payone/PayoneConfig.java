package com.commercetools.pspadapter.payone;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import java.util.Optional;

/**
 * @author fhaertig
 * @date 11.12.15
 */
public class PayoneConfig {

    private final String poApiUrl;
    private final String subAccountId;
    private final String merchantId;
    private final String portalId;
    private final String keyAsMd5Hash;
    private final String mode;
    private final String apiVersion;


    public PayoneConfig(final String poApiUrl) {

        this.poApiUrl = poApiUrl;
        this.subAccountId = getPropertyValue("PAYONE_SUBACC_ID", null);
        this.merchantId = getPropertyValue("PAYONE_MERCHANT_ID", null);
        this.portalId = getPropertyValue("PAYONE_PORTAL_ID", null);
        this.mode = getPropertyValue("PAYONE_MODE", "test");
        this.apiVersion = "3.9";

        this.keyAsMd5Hash = Hashing.md5().hashString(getPropertyValue("PAYONE_KEY", null), Charsets.UTF_8).toString();
    }

    public String getPoApiUrl() {
        return poApiUrl != null ? poApiUrl : "";
    }

    public String getSubAccountId() {
        return subAccountId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getKeyAsMd5Hash() {
        return keyAsMd5Hash;
    }

    public String getPortalId() {
        return portalId;
    }

    public String getMode() {
        return mode;
    }

    public String getApiVersion() { return apiVersion; }

    static String getPropertyValue(final String key, final String defaultValue) {
        final String propertyValue = Optional
                                        .ofNullable(System.getProperty(key, defaultValue))
                                        .orElse(Optional.ofNullable(System.getenv(key)).orElse(defaultValue));
        if (propertyValue == null) {
            throw new IllegalArgumentException("Value of " + key + " is required and can not be empty!");
        }
        return propertyValue;
    }
}
