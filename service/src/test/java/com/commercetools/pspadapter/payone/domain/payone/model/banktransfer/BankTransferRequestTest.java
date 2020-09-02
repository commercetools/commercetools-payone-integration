package com.commercetools.pspadapter.payone.domain.payone.model.banktransfer;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.util.ClearSecuredValuesSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType.PAYONE_PNT;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.when;

/**
 * @author fhaertig
 * @since 19.04.16
 */
@RunWith(MockitoJUnitRunner.class)
public class BankTransferRequestTest {
    private static final String onlinebanktransfertype = "test-type";
    private static final String merchantId = "merchant X";
    private static final String portalId = "portal 23";
    private static final String keyHash = "hashed key";
    private static final String mode = "unit test";
    private static final String apiVersion = "v.1.2.3";
    private static final String iban = "DE012345";
    private static final String bic = "NOLADE0";

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
        final BankTransferRequest request = new BankTransferRequest(AUTHORIZATION, payoneConfig, onlinebanktransfertype);
        request.setIban(iban);
        request.setBic(bic);

        assertThat(request.toStringMap(false)).containsOnly(
                entry("request", "authorization"),
                entry("onlinebanktransfertype", onlinebanktransfertype),
                entry("clearingtype", PAYONE_PNT.getPayoneCode()),
                entry("mid", merchantId),
                entry("portalid", portalId),
                entry("mode", mode),
                entry("api_version", apiVersion),
                entry("key", keyHash),
                entry("iban", iban),
                entry("bic", bic),
                entry("amount", 0));
    }

    @Test
    public void createsMapWithHiddenSecrets() {
        //create with required properties
        final BankTransferRequest request = new BankTransferRequest(AUTHORIZATION, payoneConfig, onlinebanktransfertype);
        request.setIban(iban);
        request.setBic(bic);

        assertThat(request.toStringMap(true)).containsOnly(
                entry("request", "authorization"),
                entry("onlinebanktransfertype", onlinebanktransfertype),
                entry("clearingtype", PAYONE_PNT.getPayoneCode()),
                entry("mid", merchantId),
                entry("portalid", portalId),
                entry("mode", mode),
                entry("api_version", apiVersion),
                entry("key", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("iban", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("bic", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("amount", 0));
    }
}