package com.commercetools.pspadapter.payone;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

/**
 * @author fhaertig
 * @date 11.12.15
 */
public class PayoneConfig {

    private final String poApiUrl;
    private final String subAccountId;
    private final String merchantId;
    private final String portalId;
    private final String key;
    private final String mode;
    private final String apiVersion = "3.9";


    public PayoneConfig(final String poApiUrl) {
        this.poApiUrl = poApiUrl;
        this.subAccountId = getProperty("PAYONE_SUBACC_ID");
        this.merchantId = getProperty("PAYONE_MERCHANT_ID");
        this.portalId = getProperty("PAYONE_PORTAL_ID");
        this.key = getProperty("PAYONE_KEY");
        this.mode = getProperty("PAYONE_MODE");
    }

    public String getPoApiUrl() {
        return poApiUrl != null ? poApiUrl : "";
    }

    public String getSubAccountId() {
        return subAccountId != null ? subAccountId : "";
    }

    public String getMerchantId() {
        return merchantId != null ? merchantId : "";
    }

    public String getKeyAsMD5Hash() {
        if (this.key == null) {
            return "";
        } else {
            return Hashing.md5().hashString(this.key, Charsets.UTF_8).toString();
        }
    }

    public String getPortalId() {
        return portalId != null ? portalId : "";
    }

    public String getMode() {
        return mode != null ? mode : "";
    }

    public String getApiVersion() { return apiVersion; }

    private static String getProperty(final String key) {
        return System.getProperty(key);
    }
}
