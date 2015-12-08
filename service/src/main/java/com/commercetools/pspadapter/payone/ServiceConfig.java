package com.commercetools.pspadapter.payone;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

/**
 * @author fhaertig
 * @author Jan Wolter
 * @date 02.12.15
 */
public class ServiceConfig {

    private static final String DEFAULT_PAYONE_POST_GATEWAY = "https://api.pay1.de/post-gateway/";

    private final String ctProjectKey;
    private final String ctClientId;
    private final String ctClientSecret;
    private final String poApiUrl;

    public ServiceConfig() throws MalformedURLException {
        this(new URL(Optional.ofNullable(getProperty("PO_API_URL")).orElse(DEFAULT_PAYONE_POST_GATEWAY)));
    }

    public ServiceConfig(final URL payoneApiUrl) {
        ctProjectKey = getProperty("CT_PROJECT_KEY");
        ctClientId  = getProperty("CT_CLIENT_ID");
        ctClientSecret = getProperty("CT_CLIENT_SECRET");
        poApiUrl = payoneApiUrl.toExternalForm();
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
        return poApiUrl;
    }

    private static String getProperty(final String key) {
        return System.getProperty(key);
    }

}
