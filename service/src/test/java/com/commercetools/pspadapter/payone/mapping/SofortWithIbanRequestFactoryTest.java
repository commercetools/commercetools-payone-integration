package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.BaseTenantPropertyTest;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BankTransferRequest;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.payments.Payment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SofortWithIbanRequestFactoryTest extends BaseTenantPropertyTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void createAuthorizationRequest_withPlainData() throws Exception {
        Payment payment = paymentsTestHelper.dummyPaymentOneAuthPending20EuroWithIbanPlain();
        Order order = paymentsTestHelper.dummyOrderMapToPayoneRequest();

        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, order);

        SofortBankTransferRequestFactory factory = new SofortBankTransferRequestFactory(tenantConfig);
        BankTransferRequest authorizationRequest = factory.createAuthorizationRequest(paymentWithCartLike);

        assertThat(authorizationRequest.getIban()).isEqualTo("DE66778899");
        assertThat(authorizationRequest.getBic()).isEqualTo("BIC_TEST_TEST");
    }

    @Test
    public void createAuthorizationRequest_withEncryptedData() throws Exception {
        Payment payment = paymentsTestHelper.dummyPaymentOneAuthPending20EuroWithIbanEncrypted();
        Order order = paymentsTestHelper.dummyOrderMapToPayoneRequest();
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, order);

        // mock key which has been used in dummyPaymentOneAuthPending20EuroWithIbanEncrypted IBAN/BIC encrypting
        when(tenantConfig.getSecureKey()).thenReturn("qwertyuiopasdfgh");

        SofortBankTransferRequestFactory factory = new SofortBankTransferRequestFactory(tenantConfig);
        BankTransferRequest authorizationRequest = factory.createAuthorizationRequest(paymentWithCartLike);

        assertThat(authorizationRequest.getIban()).isEqualTo("DE66778899");
        assertThat(authorizationRequest.getBic()).isEqualTo("BIC_TEST_TEST");
    }
}