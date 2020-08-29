package com.commercetools.pspadapter.payone.domain.payone.model.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.when;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.util.ClearSecuredValuesSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author fhaertig
 * @since 11.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseRequestTest {
    private static final String requestType = "some-request";
    private static final String merchantId = "merchant X";
    private static final String portalId = "portal 23";
    private static final String keyHash = "hashed key";
    private static final String mode = "unit test";
    private static final String apiVersion = "v.1.2.3";

    @Mock
    private PayoneConfig payoneConfig;

    @Before
    public void setUp() {
        when(payoneConfig.getMerchantId()).thenReturn(merchantId);
        when(payoneConfig.getPortalId()).thenReturn(portalId);
        when(payoneConfig.getKeyAsHash()).thenReturn(keyHash);
        when(payoneConfig.getMode()).thenReturn(mode);
        when(payoneConfig.getApiVersion()).thenReturn(apiVersion);
    }

    @Test
    public void createsFullMap() {
        //create with required properties
        final BaseRequest request = new BaseRequest(payoneConfig, requestType);

        assertThat(request.toStringMap(false)).containsOnly(
                entry("request", requestType),
                entry("mid", merchantId),
                entry("portalid", portalId),
                entry("key", keyHash),
                entry("mode", mode),
                entry("api_version", apiVersion));
    }

    @Test
    public void createsMapWithHiddenSecrets() {
        //create with required properties
        final BaseRequest request = new BaseRequest(payoneConfig, requestType);

        assertThat(request.toStringMap(true)).containsOnly(
                entry("request", requestType),
                entry("mid", merchantId),
                entry("portalid", portalId),
                entry("mode", mode),
                entry("api_version", apiVersion),
                entry("key", ClearSecuredValuesSerializer.PLACEHOLDER));
    }
}
