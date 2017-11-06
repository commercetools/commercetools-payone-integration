package com.commercetools.pspadapter.payone.config;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import static com.commercetools.pspadapter.payone.config.PropertyProvider.*;
import static org.assertj.core.api.Assertions.assertThat;

public class PropertyProviderTest {

    private static final String HALLO_TEST_KEY = "hallo-test";
    private PropertyProvider propertyProvider;

    @Before
    public void setUp() throws Exception {
        propertyProvider = new PropertyProvider();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        System.clearProperty(HALLO_TEST_KEY); // in case exception happened in getFromSystemProperties test
    }

    @Test
    public void getInternalProperties() throws Exception {
        assertThat(propertyProvider.getMandatoryNonEmptyProperty(PAYONE_API_VERSION)).isEqualTo("3.9");
        assertThat(propertyProvider.getMandatoryNonEmptyProperty(PAYONE_REQUEST_ENCODING)).isEqualTo("UTF-8");
        assertThat(propertyProvider.getMandatoryNonEmptyProperty(PAYONE_SOLUTION_NAME)).isEqualTo("commercetools-platform");
        assertThat(propertyProvider.getMandatoryNonEmptyProperty(PAYONE_SOLUTION_VERSION)).isEqualTo("1");
        assertThat(propertyProvider.getMandatoryNonEmptyProperty(PAYONE_INTEGRATOR_NAME)).isEqualTo("DEBUG-TITLE");
        assertThat(propertyProvider.getMandatoryNonEmptyProperty(PAYONE_INTEGRATOR_VERSION)).isEqualTo("DEBUG-VERSION");
    }

    @Test
    public void getEmptyProperties() throws Exception {
        assertThat(propertyProvider.getProperty(null)).isEmpty();
        assertThat(propertyProvider.getProperty("")).isEmpty();
        assertThat(propertyProvider.getProperty("  ")).isEmpty();
        assertThat(propertyProvider.getProperty("woot-hack-woot")).isEmpty();
    }

    @Test
    public void getFromSystemProperties() throws Exception {
        assertThat(propertyProvider.getProperty("hallo-test")).isEmpty();
        System.setProperty("hallo-test", "world");
        assertThat(propertyProvider.getProperty("hallo-test")).contains("world");
        System.clearProperty(HALLO_TEST_KEY);
    }

}