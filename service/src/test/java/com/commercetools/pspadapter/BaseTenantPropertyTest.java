package com.commercetools.pspadapter;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.pspadapter.tenant.TenantPropertyProvider;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.mockito.Mock;
import util.PaymentTestHelper;

import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class BaseTenantPropertyTest {

    protected final static PaymentTestHelper paymentsTestHelper = new PaymentTestHelper();

    protected static final String dummyPropertyValue = "123";
    protected static final String dummyTenantValue = "456";

    @Mock(lenient = true)
    protected TenantPropertyProvider tenantPropertyProvider;

    @Mock(lenient = true)
    protected PropertyProvider propertyProvider;

    @Mock(lenient = true)
    protected TenantConfig tenantConfig;

    protected PayoneConfig payoneConfig;

    @Before
    public void setUp() throws Exception {
        // 1) inject dummy values to tenant and common properties providers
        // 2) inject propertyProvider to tenantPropertyProvider
        // 3) inject payoneConfig to tenant config
        when(propertyProvider.getProperty(any())).thenReturn(Optional.of(dummyPropertyValue));
        when(propertyProvider.getMandatoryNonEmptyProperty(any())).thenReturn(dummyPropertyValue);

        when(tenantPropertyProvider.getCommonPropertyProvider()).thenReturn(propertyProvider);
        when(tenantPropertyProvider.getTenantProperty(anyString())).thenReturn(Optional.of(dummyTenantValue));
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(anyString())).thenReturn(dummyTenantValue);

        payoneConfig = new PayoneConfig(tenantPropertyProvider);

        when(tenantConfig.getPayoneConfig()).thenReturn(payoneConfig);
    }

    protected void validateBaseRequestValues(AuthorizationRequest request, String requestType) {

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(request.getRequest()).isEqualTo(requestType);
        softly.assertThat(request.getAid()).isEqualTo(payoneConfig.getSubAccountId());
        softly.assertThat(request.getMid()).isEqualTo(payoneConfig.getMerchantId());
        softly.assertThat(request.getPortalid()).isEqualTo(payoneConfig.getPortalId());
        softly.assertThat(request.getKey()).isEqualTo(payoneConfig.getKeyAsHash());
        softly.assertThat(request.getMode()).isEqualTo(payoneConfig.getMode());
        softly.assertThat(request.getApiVersion()).isEqualTo(payoneConfig.getApiVersion());
        softly.assertThat(request.getEncoding()).isEqualTo(payoneConfig.getEncoding());
        softly.assertThat(request.getSolutionName()).isEqualTo(payoneConfig.getSolutionName());
        softly.assertThat(request.getSolutionVersion()).isEqualTo(payoneConfig.getSolutionVersion());
        softly.assertThat(request.getIntegratorName()).isEqualTo(payoneConfig.getIntegratorName());
        softly.assertThat(request.getIntegratorVersion()).isEqualTo(payoneConfig.getIntegratorVersion());

        softly.assertAll();
    }
}
