package com.commercetools.pspadapter.payone;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

/**
 * @author fhaertig
 * @date 03.12.15
 * TODO jw: The dependency on System.getProperty sucks, tests aren't isolated and might fail when executed in parallel.
 * @see #setUp()
 * @see #tearDown()
 */
public class ServiceConfigTest {

    public static final String EMPTY_STRING = "";
    public static final String SYSTEM_PROPERTY = "fooValue";
    public static final String URL_SYSTEM_PROPERTY = "https://www.an.url.com/a/path/";
    public static final String PO_API_URL_DEFAULT = "https://api.pay1.de/post-gateway/";

    private final Set<String> originalSystemProperties = System.getProperties().stringPropertyNames();

    /**
     * Ensure that none of the system propertie the SystemConfig depends is already existing.
     * TODO jw: Get rid of this brittle global state.
     */
    @Before
    public void setUp() {
        assertAbsenceOfProperty("CT_PROJECT_KEY");
        assertAbsenceOfProperty("CT_CLIENT_ID");
        assertAbsenceOfProperty("CT_CLIENT_SECRET");
        assertAbsenceOfProperty("PO_API_URL");
    }

    /**
     * Ensure that none of the system propertie the SystemConfig depends is left behind.
     * TODO jw: Get rid of this brittle global state.
     */
    @After
    public void tearDown() {
        System.clearProperty("CT_PROJECT_KEY");
        System.clearProperty("CT_CLIENT_ID");
        System.clearProperty("CT_CLIENT_SECRET");
        System.clearProperty("PO_API_URL");
    }

    @Test
    public void getCtProjectKey() throws MalformedURLException {
        ServiceConfig config = new ServiceConfig();

        assertThat(config.getCtProjectKey(), is(EMPTY_STRING));

        System.setProperty("CT_PROJECT_KEY", SYSTEM_PROPERTY);

        assertThat(new ServiceConfig().getCtProjectKey(), is(SYSTEM_PROPERTY));
        assertThat("after system property was set", config.getCtProjectKey(), is(EMPTY_STRING));
    }

    @Test
    public void getCtClientId() throws MalformedURLException {
        ServiceConfig config = new ServiceConfig();

        assertThat(config.getCtClientId(), is(EMPTY_STRING));

        System.setProperty("CT_CLIENT_ID", SYSTEM_PROPERTY);

        assertThat(new ServiceConfig().getCtClientId(), is(SYSTEM_PROPERTY));
        assertThat("after system property was set", config.getCtClientId(), is(EMPTY_STRING));
    }

    @Test
    public void getCtClientSecret() throws MalformedURLException {
        ServiceConfig config = new ServiceConfig();

        assertThat(config.getCtClientSecret(), is(EMPTY_STRING));

        System.setProperty("CT_CLIENT_SECRET", SYSTEM_PROPERTY);

        assertThat(new ServiceConfig().getCtClientSecret(), is(SYSTEM_PROPERTY));
        assertThat("after system property was set", config.getCtClientSecret(), is(EMPTY_STRING));
    }

    @Test
    public void getPoApiUrl() throws MalformedURLException {
        ServiceConfig config = new ServiceConfig();

        assertThat(config.getPoApiUrl(), is(PO_API_URL_DEFAULT));

        System.setProperty("PO_API_URL", URL_SYSTEM_PROPERTY);

        assertThat(new ServiceConfig().getPoApiUrl(), is(URL_SYSTEM_PROPERTY));
        assertThat("after system property was set", config.getPoApiUrl(), is(PO_API_URL_DEFAULT));
    }

    @Test
    public void getsPoApiUrlPassedToConstructor() throws MalformedURLException {
        final URL testUrl = new URL("http://www.atest.url.com/pathX");

        final ServiceConfig config = new ServiceConfig(testUrl);

        assertThat(config.getPoApiUrl(), is(testUrl.toExternalForm()));

        System.setProperty("PO_API_URL", URL_SYSTEM_PROPERTY);

        assertThat(new ServiceConfig(testUrl).getPoApiUrl(), is(testUrl.toExternalForm()));
        assertThat("after system property was set", config.getPoApiUrl(), is(testUrl.toExternalForm()));
    }

    private static void assertAbsenceOfProperty(final String propertyKey) {
        assertThat(propertyKey + " must not be set", System.getProperty(propertyKey), is(nullValue()));
    }
}
