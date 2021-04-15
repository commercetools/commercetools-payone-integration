package com.commercetools.pspadapter.payone.mapping.order;

import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * Default implementation for {@link PaymentToOrderStateMapper}:<ul>
 *     <li><i>appointed</i> and <i>underpaid</i> are mapped to {@link PaymentState#PENDING}</li>
 *     <li><i>paid</i>, <i>capture</i> and <i>transfer</i> are mapped to {@link PaymentState#PAID}</li>
 *     <li><i>failed</i> and <i>cancelation</i> are mapped to {@link PaymentState#FAILED}</li>
 *     <li>the rest is mapped to <b>null</b></li>
 * </ul>
 */
public class DefaultPaymentToOrderStateMapper implements PaymentToOrderStateMapper {

    private static final Map<String, PaymentState> PAYMENT_STATE_MAP = initPaymentStateMap();


    @Override
    public PaymentState mapPaymentToOrderState(Payment payment) {
        return ofNullable(payment)
                .map(Payment::getPaymentStatus)
                .map(PaymentStatus::getInterfaceCode)
                .map(PAYMENT_STATE_MAP::get)
                .orElse(null);
    }

    private static Map<String, PaymentState> initPaymentStateMap() {
        Map<String, PaymentState> paymentStateHashMap = new HashMap<>();
        paymentStateHashMap.put(NotificationAction.APPOINTED.getTxActionCode(), PaymentState.PENDING);
        paymentStateHashMap.put(NotificationAction.UNDERPAID.getTxActionCode(), PaymentState.PENDING);

        paymentStateHashMap.put(NotificationAction.PAID.getTxActionCode(), PaymentState.PAID);
        paymentStateHashMap.put(NotificationAction.CAPTURE.getTxActionCode(), PaymentState.PAID);
        paymentStateHashMap.put(NotificationAction.TRANSFER.getTxActionCode(), PaymentState.PAID);

        paymentStateHashMap.put(NotificationAction.FAILED.getTxActionCode(), PaymentState.FAILED);
        paymentStateHashMap.put(NotificationAction.CANCELATION.getTxActionCode(), PaymentState.FAILED);
        return Collections.unmodifiableMap(paymentStateHashMap);
    }

}
