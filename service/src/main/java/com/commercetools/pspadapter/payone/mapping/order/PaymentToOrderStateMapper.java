package com.commercetools.pspadapter.payone.mapping.order;

import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.payments.Payment;

public interface PaymentToOrderStateMapper {

    /**
     * Map {@link Payment#getPaymentStatus()} to respective value for {@link Order#getPaymentState()}.
     * <p>
     * Since there is no any strict description of {@link Order#getPaymentState()} the implementation may be shop specific.
     *
     * @param payment <b>nullable</b> {@link Payment} from which payment status should be mapped
     * @return <b>nullable</b> {@link PaymentState} mapped to respective payment status value.
     */
    PaymentState mapPaymentToOrderState(Payment payment);
}
