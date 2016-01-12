package com.commercetools.pspadapter.payone.config;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

/**
 * @author fhaertig
 * @since 11.12.15
 */
public class PayoneConfig {

    public static final String DEFAULT_PAYONE_API_URL = "https://api.pay1.de/post-gateway/";
    public static final String DEFAULT_PAYONE_MODE = "test";

    //assure that properties don't change once service started
    private final String subAccountId;
    private final String merchantId;
    private final String portalId;
    private final String keyAsMd5Hash;
    private final String mode;
    private final String apiUrl;
    private final String apiVersion;


    public PayoneConfig(final PropertyProvider propertyProvider) {
        subAccountId = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_SUBACC_ID);
        merchantId = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_MERCHANT_ID);
        portalId = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_PORTAL_ID);
        mode = propertyProvider.getProperty(PropertyProvider.PAYONE_MODE).orElse(DEFAULT_PAYONE_MODE);
        apiUrl = propertyProvider.getProperty(PropertyProvider.PAYONE_API_URL).orElse(DEFAULT_PAYONE_API_URL);
        apiVersion = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_API_VERSION);
        final String plainKey = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_KEY);
        keyAsMd5Hash = Hashing.md5().hashString(plainKey, Charsets.UTF_8).toString();
    }

    public String getApiUrl() {
        return apiUrl;
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

}
