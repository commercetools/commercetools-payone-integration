package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.pspadapter.tenant.TenantFactory;
import com.commercetools.pspadapter.tenant.TenantPropertyProvider;

import java.util.List;

import static com.commercetools.pspadapter.payone.util.PayoneConstants.PAYONE;
import static java.util.stream.Collectors.toList;

public class ServiceFactory {

    public static IntegrationService createIntegrationService(PropertyProvider propertyProvider, ServiceConfig serviceConfig) {
        List<TenantFactory> tenantFactories = serviceConfig.getTenants().stream()
                .map(tenantName -> new TenantPropertyProvider(tenantName, propertyProvider))
                .map(tenantPropertyProvider -> new TenantConfig(tenantPropertyProvider, new PayoneConfig(tenantPropertyProvider)))
                .map(tenantConfig -> new TenantFactory(PAYONE, tenantConfig))
                .collect(toList());

        return new IntegrationService(tenantFactories);
    }

}
