package com.commercetools.pspadapter.payone.domain.ctp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.payments.Payment;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import util.PaymentTestHelper;

/**
 * @author fhaertig
 * @since 17.03.16
 */
public class PaymentWithCartLikeTest {

    private final PaymentTestHelper testHelper = new PaymentTestHelper();

    @Test
    public void createFromOrderAndPaymentWithReference() throws Exception {
        Payment payment = testHelper.dummyPaymentOneAuthPending20EuroCC();
        Order order = testHelper.dummyOrderMapToPayoneRequest();

        PaymentWithCartLike testee = new PaymentWithCartLike(payment, order);

        assertThat(testee.getOrderNumber()).isEqualTo(payment.getCustom().getFieldAsString(CustomFieldKeys.REFERENCE_FIELD));
        assertThat(testee.getCartLike()).isEqualTo(order);
    }

    @Test
    public void createFromCartAndPaymentWithReference() throws Exception {
        Payment payment = testHelper.dummyPaymentOneAuthPending20EuroCC();
        Cart cart = testHelper.dummyCart();


        PaymentWithCartLike testee = new PaymentWithCartLike(payment, cart);

        assertThat(testee.getOrderNumber()).isEqualTo(payment.getCustom().getFieldAsString(CustomFieldKeys.REFERENCE_FIELD));
        assertThat(testee.getCartLike()).isEqualTo(cart);
    }

    @Test
    public void createFromOrderAndPaymentWithoutReference() throws Exception {
        Payment payment = testHelper.dummyPaymentNoCustomFields();
        Order order = testHelper.dummyOrderMapToPayoneRequest();

        final Throwable throwable = catchThrowable(() -> new PaymentWithCartLike(payment, order));

        Assertions.assertThat(throwable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is missing the required custom field");
    }

    @Test
    public void createWithNewPaymentOrderNumberChanges() throws Exception {
        Payment payment = testHelper.dummyPaymentOneAuthPending20EuroPNT();
        Order order = testHelper.dummyOrderMapToPayoneRequest();

        PaymentWithCartLike testee = new PaymentWithCartLike(payment, order);

        assertThat(testee.getOrderNumber()).isEqualTo(payment.getCustom().getFieldAsString(CustomFieldKeys.REFERENCE_FIELD));
        assertThat(testee.getCartLike()).isEqualTo(order);

        Payment newPayment = testHelper.dummyPaymentOneAuthPending20EuroCC();

        testee = testee.withPayment(newPayment);

        assertThat(testee.getOrderNumber()).isEqualTo(newPayment.getCustom().getFieldAsString(CustomFieldKeys.REFERENCE_FIELD));
        assertThat(testee.getCartLike()).isEqualTo(order);
    }
}