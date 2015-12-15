package com.commercetools.pspadapter.payone.domain.payone.model.creditcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Currency;
import java.util.Map;
import java.util.Optional;

/**
 * @author fhaertig
 * @date 11.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class CreditCardPreauthorizationRequestTest {

    private static final String REFERENCE = "123";
    private static final int AMOUNT = 2000;
    private static final String LASTNAME = "Mustermann";
    private static final String COUNTRY = "DE";
    private static final String PSEUDOCARDPAN = "0000123";

    @Mock
    private PropertyProvider propertyProvider;

    private PayoneConfig payoneConfig;

    @Before
    public void setUp() {
        Map<String, String> internalProperties = ImmutableMap.<String, String>builder()
                .put(PropertyProvider.PAYONE_API_VERSION, "3.9")
                .build();

        when(propertyProvider.getEnvironmentOrSystemValue(any())).thenReturn(Optional.of("dummyValue"));
        when(propertyProvider.getInternalProperties()).thenReturn(internalProperties);
        when(propertyProvider.createIllegalArgumentException(any())).thenCallRealMethod();

        payoneConfig = new PayoneConfig(propertyProvider);
    }

    @Test
    public void serializeCreditCardPreAuthRequestToStringMap() {


        //create with required properties
        CreditCardPreauthorizationRequest request = new CreditCardPreauthorizationRequest(payoneConfig, PSEUDOCARDPAN);

        request.setReference(REFERENCE);
        request.setAmount(AMOUNT);
        request.setCurrency(Currency.getInstance("EUR").getCurrencyCode());
        request.setLastname(LASTNAME);
        request.setCountry(COUNTRY);
        request.setEcommercemode("internet");

        Map<String, Object> resultMap = request.toStringMap();

        assertThat(resultMap).containsEntry("request", RequestType.PREAUTHORIZATION.getType());
        assertThat(resultMap).containsEntry("aid", payoneConfig.getSubAccountId());
        assertThat(resultMap).containsEntry("mid", payoneConfig.getMerchantId());
        assertThat(resultMap).containsEntry("portalid", payoneConfig.getPortalId());
        assertThat(resultMap).containsEntry("key", payoneConfig.getKeyAsMd5Hash());
        assertThat(resultMap).containsEntry("mode", payoneConfig.getMode());
        assertThat(resultMap).containsEntry("api_version", payoneConfig.getApiVersion());
        assertThat(resultMap).containsEntry("clearingtype", ClearingType.PAYONE_CC.getPayoneCode());
        assertThat(resultMap).containsEntry("reference", REFERENCE);
        assertThat(resultMap).containsEntry("amount", AMOUNT);
        assertThat(resultMap).containsEntry("currency", Currency.getInstance("EUR").getCurrencyCode());
        assertThat(resultMap).containsEntry("lastname", LASTNAME);
        assertThat(resultMap).containsEntry("country", COUNTRY);
        assertThat(resultMap).containsEntry("pseudocardpan", PSEUDOCARDPAN);
        assertThat(resultMap).containsEntry("ecommercemode", "internet");

        //assure that no other properties are contained
        assertThat(resultMap).hasSize(15);
    }
}
