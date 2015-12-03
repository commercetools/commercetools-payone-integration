package com.commercetools.pspadapter.payone;

/**
 * @author fhaertig
 * @date 02.12.15
 */
public class ServiceConfig {

    private final String ctProjectKey;
    private final String ctClientId;
    private final String ctClientSecret;
    private final String poApiUrl;

    public ServiceConfig() {
        ctProjectKey = getProperty("CT_PROJECT_KEY");
        ctClientId  = getProperty("CT_CLIENT_ID");
        ctClientSecret = getProperty("CT_CLIENT_SECRET");
        poApiUrl = getProperty("PO_API_URL");
    }

    public String getCtProjectKey() {
        return ctProjectKey != null ? ctProjectKey : "";
    }

    public String getCtClientId() {
        return ctClientId != null ? ctClientId : "";
    }

    public String getCtClientSecret() {
        return ctClientSecret != null ? ctClientSecret : "";
    }

    public String getPoApiUrl() {
        return poApiUrl != null ? poApiUrl : "https://api.pay1.de/post-gateway/";
    }

    private String getProperty(final String key) {
        return System.getProperty(key);
    }
}
