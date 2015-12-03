package com.commercetools.pspadapter.payone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Test;

/**
 * @author fhaertig
 * @date 03.12.15
 */
public class ServiceConfigTest {

    public static final String EMPTY_STRING = "";
    public static final String SYSTEM_PROPERTY = "fooValue";
    public static final String PO_API_URL_DEFAULT = "https://api.pay1.de/post-gateway/";

    @Test
    public void getCtProjectKey() {
        ServiceConfig config = new ServiceConfig();

        assertThat(config.getCtProjectKey(), is(EMPTY_STRING));

        System.setProperty("CT_PROJECT_KEY", SYSTEM_PROPERTY);

        assertThat(new ServiceConfig().getCtProjectKey(), is(SYSTEM_PROPERTY));
        assertThat("after system property was set", config.getCtProjectKey(), is(EMPTY_STRING));
    }

    @Test
    public void getCtClientId() {
        ServiceConfig config = new ServiceConfig();

        assertThat(config.getCtClientId(), is(EMPTY_STRING));

        System.setProperty("CT_CLIENT_ID", SYSTEM_PROPERTY);

        assertThat(new ServiceConfig().getCtClientId(), is(SYSTEM_PROPERTY));
        assertThat("after system property was set", config.getCtClientId(), is(EMPTY_STRING));
    }


    @Test
    public void getCtClientSecret() {
        ServiceConfig config = new ServiceConfig();

        assertThat(config.getCtClientSecret(), is(EMPTY_STRING));

        System.setProperty("CT_CLIENT_SECRET", SYSTEM_PROPERTY);

        assertThat(new ServiceConfig().getCtClientSecret(), is(SYSTEM_PROPERTY));
        assertThat("after system property was set", config.getCtClientSecret(), is(EMPTY_STRING));
    }


    @Test
    public void getPoApiUrl() {
        ServiceConfig config = new ServiceConfig();

        assertThat(config.getPoApiUrl(), is(PO_API_URL_DEFAULT));

        System.setProperty("PO_API_URL", SYSTEM_PROPERTY);

        assertThat(new ServiceConfig().getPoApiUrl(), is(SYSTEM_PROPERTY));
        assertThat("after system property was set", config.getPoApiUrl(), is(PO_API_URL_DEFAULT));
    }
}