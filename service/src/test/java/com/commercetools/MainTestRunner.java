package com.commercetools;

import com.commercetools.pspadapter.payone.IntegrationService;
import com.commercetools.pspadapter.payone.ServiceFactory;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.config.ServiceConfig;

import java.net.URISyntaxException;

import static com.commercetools.Main.bridgeJULToSLF4J;
import static com.commercetools.Main.configureAccessLogs;
import static com.commercetools.Main.configureLogLevel;

public class MainTestRunner {
    static MainTestRunner mainTestRunner;
    PropertyProvider propertyProvider = new PropertyProvider();


    private MainTestRunner() {
    }

    public static MainTestRunner getInstance() {
        if (mainTestRunner == null) {
            mainTestRunner = new MainTestRunner();
        }
        return mainTestRunner;
    }

    public void startPayoneService() throws URISyntaxException {

        ServiceConfig serviceConfig = new ServiceConfig(propertyProvider);

        bridgeJULToSLF4J();
        configureAccessLogs();
        configureLogLevel(serviceConfig);

        final IntegrationService integrationService = ServiceFactory.createIntegrationService(propertyProvider, serviceConfig);
        integrationService.start();
    }

    public PropertyProvider getPropertyProvider() {
        return propertyProvider;
    }

}
