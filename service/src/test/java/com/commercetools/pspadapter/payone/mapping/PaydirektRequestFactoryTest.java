package com.commercetools.pspadapter.payone.mapping;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PaydirektRequestFactoryTest extends BaseWalletRequestFactoryTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void createFullPreauthorizationRequestFromValidPayment() throws Exception {
        super.createFullPreauthorizationRequestFromValidPayment(payments.dummyPaymentOneAuthPending20EuroPDT(), "wlt", "PDT");
    }

    @Test
    public void createFullAuthorizationRequestFromValidPayment() throws Exception {
        super.createFullAuthorizationRequestFromValidPayment(payments.dummyPaymentOneAuthPending20EuroPDT(), "wlt", "PDT");
    }
}