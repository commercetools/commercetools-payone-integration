package com.commercetools.util;

import java.util.Locale;

public final class ServiceConstants {

    /**
     * "en" without country code.
     */
    public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    public static final String DEFAULT_AUTH_URL = "https://auth.europe-west1.gcp.commercetools.com";
    public static final String DEFAULT_API_URL = "https://api.europe-west1.gcp.commercetools.com";

    private ServiceConstants() {
    }
}
