package com.commercetools.pspadapter.payone.notification;

import com.commercetools.pspadapter.payone.ServiceFactory;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.service.OrderService;
import com.commercetools.service.PaymentService;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import util.PaymentTestHelper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BaseNotificationProcessorTest {
    @Mock
    protected PaymentService paymentService;

    @Mock
    protected OrderService orderService;

    @Mock
    protected ServiceFactory serviceFactory;

    @Captor
    protected ArgumentCaptor<List<UpdateAction<Payment>>> paymentRequestUpdatesCaptor;

    @Captor
    protected ArgumentCaptor<Payment> paymentRequestPayment;

    protected final PaymentTestHelper testHelper = new PaymentTestHelper();

    protected Notification notification;

    @Before
    public void setUp() throws Exception {
        when(serviceFactory.getPaymentService()).thenReturn(paymentService);
        when(paymentService.updatePayment(any(Payment.class), anyObject()))
                .then(answer -> CompletableFuture.completedFuture(answer.getArgumentAt(0, Payment.class)));

        when(serviceFactory.getOrderService()).thenReturn(orderService);
        when(orderService.getOrderByPaymentId(anyString()))
                .then(answer -> CompletableFuture.completedFuture(Optional.empty()));
    }

    protected List<? extends UpdateAction<Payment>> updatePaymentAndGetUpdateActions(Payment payment) {
        verify(paymentService).updatePayment(paymentRequestPayment.capture(), paymentRequestUpdatesCaptor.capture());

        final Payment updatePayment = paymentRequestPayment.getValue();
        assertThat(updatePayment).isEqualTo(payment);
        return paymentRequestUpdatesCaptor.getValue();
    }
}
