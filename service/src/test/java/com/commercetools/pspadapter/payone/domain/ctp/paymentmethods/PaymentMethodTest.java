package com.commercetools.pspadapter.payone.domain.ctp.paymentmethods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

/**
 * @author Jan Wolter
 */
public class PaymentMethodTest {
    @Test
    public void getsPostfinanceEfinancePaymentMethodFromMethodKey() {
        assertThat(PaymentMethod.fromMethodKey(MethodKeys.BANK_TRANSFER_POSTFINANCE_EFINANCE)).isSameAs(PaymentMethod.BANK_TRANSFER_POSTFINANCE_EFINANCE);
    }

    @Test
    public void getsCreditCardPaymentMethodFromMethodKey() {
        assertThat(PaymentMethod.fromMethodKey(MethodKeys.CREDIT_CARD)).isSameAs(PaymentMethod.CREDIT_CARD);
    }

    @Test
    public void throwsIfInvalidMethodKeyIsRequested() {
        assertThatThrownBy(() -> PaymentMethod.fromMethodKey("Taschengeld"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid PaymentMethod: Taschengeld");
    }
}
