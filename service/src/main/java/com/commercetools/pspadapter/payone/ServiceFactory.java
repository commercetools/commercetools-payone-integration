package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.config.ServiceConfig;

public class ServiceFactory {

    public static IntegrationService createIntegrationService(PropertyProvider propertyProvider, ServiceConfig serviceConfig) {
        return new IntegrationService(serviceConfig, propertyProvider);
    }

}
