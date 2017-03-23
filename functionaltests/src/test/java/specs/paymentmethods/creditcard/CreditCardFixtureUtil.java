package specs.paymentmethods.creditcard;

import com.commercetools.service.OrderService;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.PaymentState;
import specs.paymentmethods.BaseNotifiablePaymentFixture;

import static com.commercetools.pspadapter.payone.util.CompletionUtil.executeBlocking;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

public class CreditCardFixtureUtil extends BaseNotifiablePaymentFixture {

    public static String fetchOrderPaymentState(OrderService orderService, String paymentId) {
        Order order = executeBlocking(orderService.getOrderByPaymentId(paymentId))
                .orElseThrow(() -> new RuntimeException(format("Order for payment [%s] not found", paymentId)));

        return ofNullable(order.getPaymentState()).map(PaymentState::toSphereName).orElse(null);
    }
}
