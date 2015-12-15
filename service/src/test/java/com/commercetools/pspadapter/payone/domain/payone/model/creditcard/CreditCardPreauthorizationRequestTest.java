package com.commercetools.pspadapter.payone.domain.payone.model.creditcard;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.pspadapter.payone.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import org.junit.Test;

import java.util.Currency;
import java.util.Map;

/**
 * @author fhaertig
 * @date 11.12.15
 */
public class CreditCardPreauthorizationRequestTest {

    private static final String PAYONE_SERVER_API_URL = "http://some.url.org/payone";
    private static final String REFERENCE = "123";
    private static final int AMOUNT = 2000;
    private static final String LASTNAME = "Mustermann";
    private static final String COUNTRY = "DE";
    private static final String PSEUDOCARDPAN = "0000123";

    private final PayoneConfig payoneConfig = new PayoneConfig(PAYONE_SERVER_API_URL);

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
        assertThat(resultMap).containsEntry("key", payoneConfig.getKeyAsMD5Hash());
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
