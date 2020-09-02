package com.commercetools.pspadapter.payone.mapping.order;

import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultPaymentToOrderStateMapperTest {

    private PaymentToOrderStateMapper mapper;

    @Mock
    private Payment payment;

    @Mock
    private PaymentStatus paymentStatus;

    @Before
    public void setUp() throws Exception {
        mapper = new DefaultPaymentToOrderStateMapper();
    }

    @Test
    public void mapPaymentToOrderPaymentState_cornerCases() throws Exception {
        assertThat(mapper.mapPaymentToOrderState(null)).isNull();
        assertThat(mapper.mapPaymentToOrderState(payment)).isNull();

        when(payment.getPaymentStatus()).thenReturn(paymentStatus);
        assertThat(mapper.mapPaymentToOrderState(payment)).isNull();

        assertPaymentStatusCode(null, null);
        assertPaymentStatusCode("", null);
        assertPaymentStatusCode("any other string", null);
    }

    @Test
    public void mapPaymentsToOrderPaymentStatePending() throws Exception {
        assertPaymentStatusCode("appointed", PaymentState.PENDING);
        assertPaymentStatusCode("underpaid", PaymentState.PENDING);
    }

    @Test
    public void mapPaymentsToOrderPaymentStatePaid() throws Exception {
        assertPaymentStatusCode("capture", PaymentState.PAID);
        assertPaymentStatusCode("paid", PaymentState.PAID);
        assertPaymentStatusCode("transfer", PaymentState.PAID);
    }

    @Test
    public void mapPaymentsToOrderPaymentStateFailed() throws Exception {
        assertPaymentStatusCode("cancelation", PaymentState.FAILED);
        assertPaymentStatusCode("failed", PaymentState.FAILED);
    }

    @Test
    public void mapPaymentsToOrderPaymentStateNull() throws Exception {
        assertPaymentStatusCode("refund", null);
        assertPaymentStatusCode("debit", null);
        assertPaymentStatusCode("reminder", null);
        assertPaymentStatusCode("vauthorization", null);
        assertPaymentStatusCode("vsettlement", null);
        assertPaymentStatusCode("vsettlement", null);
    }

    private void assertPaymentStatusCode(String statusCodeToInject, PaymentState expected) {
        when(payment.getPaymentStatus()).thenReturn(paymentStatus);
        when(paymentStatus.getInterfaceCode()).thenReturn(statusCodeToInject);
        assertThat(mapper.mapPaymentToOrderState(payment)).isEqualTo(expected);
    }

}