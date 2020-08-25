package com.commercetools.pspadapter.payone.domain.ctp.paymentmethods;

import com.google.common.collect.ImmutableSet;
import io.sphere.sdk.payments.TransactionType;

import javax.annotation.Nonnull;
import java.util.EnumSet;

import static io.sphere.sdk.payments.TransactionType.AUTHORIZATION;
import static io.sphere.sdk.payments.TransactionType.CHARGE;
import static java.util.Arrays.stream;

/**
 * Enumerates the payment methods available for Payone and <b>supported by current service</b>.
 *
 * @see MethodKeys
 */
public enum PaymentMethod {

//    /**
//     * <b>Neither implemented, nor tested yet!</b>
//     * @see MethodKeys#DIRECT_DEBIT_SEPA
//     */
//    DIRECT_DEBIT_SEPA(MethodKeys.DIRECT_DEBIT_SEPA),

    /**
     * @see MethodKeys#CREDIT_CARD
     */
    CREDIT_CARD(MethodKeys.CREDIT_CARD),

    /**
     * @see MethodKeys#WALLET_PAYPAL
     */
    WALLET_PAYPAL(MethodKeys.WALLET_PAYPAL),

    /**
     * @see MethodKeys#WALLET_PAYDIREKT
     */
    WALLET_PAYDIREKT(MethodKeys.WALLET_PAYDIREKT),

    /**
     * @see MethodKeys#BANK_TRANSFER_SOFORTUEBERWEISUNG
     */
    BANK_TRANSFER_SOFORTUEBERWEISUNG(MethodKeys.BANK_TRANSFER_SOFORTUEBERWEISUNG),

    /**
     * @see MethodKeys#BANK_TRANSFER_ADVANCE
     */
    BANK_TRANSFER_ADVANCE(MethodKeys.BANK_TRANSFER_ADVANCE),

    /**
     * @see MethodKeys#BANK_TRANSFER_SOFORTUEBERWEISUNG
     */
    BANK_TRANSFER_POSTFINANCE_EFINANCE(MethodKeys.BANK_TRANSFER_POSTFINANCE_EFINANCE),

    /**
     * @see MethodKeys#BANK_TRANSFER_SOFORTUEBERWEISUNG
     */
    BANK_TRANSFER_POSTFINANCE_CARD(MethodKeys.BANK_TRANSFER_POSTFINANCE_CARD),

    /**
     * @see MethodKeys#INVOICE_KLARNA
     */
    INVOICE_KLARNA(MethodKeys.INVOICE_KLARNA),

    /**
     * @see MethodKeys#BANK_TRANSFER_IDEAL
     */
    BANK_TRANSFER_IDEAL(MethodKeys.BANK_TRANSFER_IDEAL);

    /**
     * @see MethodKeys#BANK_TRANSFER_BANCONTACT
     */
    BANK_TRANSFER_BANCONTACT(MethodKeys.BANK_TRANSFER_BANCONTACT);

    /**
     * Set of payment methods supported by the integration service.
     * Other payment methods are not supported so far and will throw an exception.
     */
    public static final EnumSet<PaymentMethod> supportedPaymentMethods = EnumSet.allOf(PaymentMethod.class);

    /**
     * Set of supported CTP transaction types.
     * <p>
     * <b>Note:</b> solving issue
     * <i><a href="https://github.com/commercetools/commercetools-payone-integration/issues/217">
     * Make transaction type handling transparent</a></i>
     * we support any {@link TransactionType#AUTHORIZATION AUTHORIZATION} or {@link TransactionType#CHARGE CHARGE}
     * transaction for all payment methods: the Payone request will be sent without additional validation.
     * It is a responsibility of payment creator (shop developers).
     * Although, other transaction types (like {@code REFUND} or {@code CANCEL_AUTHORIZATION}) are not supported yet.
     */
    public static final ImmutableSet<TransactionType> supportedTransactionTypes = ImmutableSet.of(AUTHORIZATION, CHARGE);

    private String key;

    PaymentMethod(final String key) {
        this.key = key;
    }

    /**
     * Gets the method key.
     *
     * @return the method key, never null
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the payment method from the provided key.
     *
     * @param methodKey the key (aka name aka identifier) of the wanted method
     * @return the requested payment mwethod
     * @throws IllegalArgumentException in case of an invalid method key
     */
    @Nonnull
    public static PaymentMethod fromMethodKey(final String methodKey) {
        return stream(PaymentMethod.values())
                .filter(value -> value.key.equals(methodKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid PaymentMethod: %s", methodKey)));
    }
}
