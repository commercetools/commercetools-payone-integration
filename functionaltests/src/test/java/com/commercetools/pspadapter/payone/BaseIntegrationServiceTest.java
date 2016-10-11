package com.commercetools.pspadapter.payone;

import org.junit.After;
import org.junit.Before;

public class BaseIntegrationServiceTest {

    public static final String BASE_URL = "http://localhost";

    protected IntegrationService service;

    @Before
    public void setUp() {
        service = ServiceFactory.create().createService();
        service.start();
    }

    @After
    public void tearDown() {
        service.stop();
    }

    /**
     * @return URL with protocol and local port.
     * @see IntegrationService#port()
     */
    protected final String BASE_URL_WITH_PORT() {
        return BASE_URL + ":" + service.port();
    }
}
