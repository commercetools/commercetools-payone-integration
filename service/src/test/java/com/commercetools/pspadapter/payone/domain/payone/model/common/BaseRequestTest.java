package com.commercetools.pspadapter.payone.domain.payone.model.common;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;
import java.util.Optional;

/**
 * @author fhaertig
 * @date 11.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseRequestTest {

    @Mock
    private PropertyProvider propertyProvider;

    private PayoneConfig payoneConfig;

    @Before
    public void setUp() {
        Map<String, String> internalProperties = ImmutableMap.<String, String>builder()
                .put(PropertyProvider.PAYONE_API_VERSION, "3.9")
                .build();

        Mockito.when(propertyProvider.getEnvironmentOrSystemValue(Matchers.any())).thenReturn(Optional.of("dummyEnvironmentValue"));
        Mockito.when(propertyProvider.getInternalProperties()).thenReturn(internalProperties);
        Mockito.when(propertyProvider.createIllegalArgumentException(Matchers.any())).thenCallRealMethod();

        payoneConfig = new PayoneConfig(propertyProvider);
    }

    @Test
    public void serializeBaseRequestToFullStringMap() {
        //create with required properties
        BaseRequest request = new BaseRequest(payoneConfig, RequestType.PREAUTHORIZATION.getType());

        Map<String, Object> resultMap = request.toStringMap(false);

        Assertions.assertThat(resultMap).containsEntry("request", RequestType.PREAUTHORIZATION.getType());
        Assertions.assertThat(resultMap).containsEntry("mid", payoneConfig.getMerchantId());
        Assertions.assertThat(resultMap).containsEntry("portalid", payoneConfig.getPortalId());
        Assertions.assertThat(resultMap).containsEntry("key", payoneConfig.getKeyAsMd5Hash());
        Assertions.assertThat(resultMap).containsEntry("mode", payoneConfig.getMode());
        Assertions.assertThat(resultMap).containsEntry("api_version", payoneConfig.getApiVersion());

        //assure that no other properties are contained
        Assertions.assertThat(resultMap).hasSize(6);
    }

    @Test
    public void serializeBaseRequestToClearedStringMap() {
        //create with required properties
        BaseRequest request = new BaseRequest(payoneConfig, RequestType.PREAUTHORIZATION.getType());

        Map<String, Object> resultMap = request.toStringMap(true);

        Assertions.assertThat(resultMap).containsEntry("request", RequestType.PREAUTHORIZATION.getType());
        Assertions.assertThat(resultMap).containsEntry("mid", payoneConfig.getMerchantId());
        Assertions.assertThat(resultMap).containsEntry("portalid", payoneConfig.getPortalId());
        Assertions.assertThat(resultMap).doesNotContainEntry("key", payoneConfig.getKeyAsMd5Hash());
        Assertions.assertThat(resultMap).containsEntry("mode", payoneConfig.getMode());
        Assertions.assertThat(resultMap).containsEntry("api_version", payoneConfig.getApiVersion());

        //assure that no other properties are contained
        Assertions.assertThat(resultMap).hasSize(5);
    }
}
