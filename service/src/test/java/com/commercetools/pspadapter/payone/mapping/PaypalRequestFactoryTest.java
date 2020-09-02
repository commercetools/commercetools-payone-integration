package com.commercetools.pspadapter.payone.mapping;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PaypalRequestFactoryTest extends BaseWalletRequestFactoryTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void createFullPreauthorizationRequestFromValidPayment() throws Exception {
        super.createFullPreauthorizationRequestFromValidPayment(payments.dummyPaymentOneAuthPending20EuroPPE(), "wlt", "PPE");
    }

    @Test
    public void createFullAuthorizationRequestFromValidPayment() throws Exception {
        super.createFullAuthorizationRequestFromValidPayment(payments.dummyPaymentOneAuthPending20EuroPPE(), "wlt", "PPE");
    }
}