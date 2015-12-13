package com.commercetools.pspadapter.payone;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class PaymentDispatcherTest extends PaymentTestHelper {

    @Test
    public void testRefusingWrongPaymentInterface() throws Exception {
        PaymentDispatcher dispatcher = new PaymentDispatcher(null);

        final Throwable noInterface = catchThrowable(() -> dispatcher.dispatchPayment(dummyPayment1()));
        assertThat(noInterface).isInstanceOf(IllegalArgumentException.class);

        final Throwable wrongInterface = catchThrowable(() -> dispatcher.dispatchPayment(dummyPaymentWrongInterface()));
        assertThat(wrongInterface).isInstanceOf(IllegalArgumentException.class);
    }

}