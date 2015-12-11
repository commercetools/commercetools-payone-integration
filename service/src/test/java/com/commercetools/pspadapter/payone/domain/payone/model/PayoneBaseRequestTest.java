package com.commercetools.pspadapter.payone.domain.payone.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.pspadapter.payone.PayoneConfig;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Map;

/**
 * @author fhaertig
 * @date 11.12.15
 */
public class PayoneBaseRequestTest {

    private static final String PAYONE_SERVER_API_URL = "http://some.url.org/payone";

    private final PayoneConfig payoneConfig = new PayoneConfig(PAYONE_SERVER_API_URL);

    @Test
    public void serializeBasicRequestToStringMap() {
        PayoneBaseRequest request = new PayoneBaseRequest.PayoneRequestBuilder(
               payoneConfig, RequestType.AUTHORIZATION.getType()).build();

        ObjectMapper m = new ObjectMapper();
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Map<String, String> result = m.convertValue(request, Map.class);

        assertThat(result).containsEntry("request", RequestType.AUTHORIZATION.getType());
        assertThat(result).containsEntry("aid", payoneConfig.getSubAccountId());
        assertThat(result).containsEntry("mid", payoneConfig.getMerchantId());
        assertThat(result).containsEntry("portalid", payoneConfig.getPortalId());
        assertThat(result).containsEntry("key", payoneConfig.getKey());
        assertThat(result).containsEntry("mode", payoneConfig.getMode());
        assertThat(result).hasSize(6);
    }

}
