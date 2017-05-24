package com.commercetools.pspadapter.payone.domain.ctp.paymentmethods;

import com.google.common.collect.ImmutableSet;
import io.sphere.sdk.payments.TransactionType;

import java.util.Arrays;

/**
 * Enumerates the payment methods available for Payone.
 *
 * @see MethodKeys
 */
public enum PaymentMethod {

    /**
     * @see MethodKeys#DIRECT_DEBIT_SEPA
     */
    DIRECT_DEBIT_SEPA(MethodKeys.DIRECT_DEBIT_SEPA,
                    TransactionType.AUTHORIZATION,
                    TransactionType.CHARGE
    ),

    /**
     * @see MethodKeys#CREDIT_CARD
     */
    CREDIT_CARD(MethodKeys.CREDIT_CARD,
                    TransactionType.AUTHORIZATION,
                    TransactionType.CHARGE
    ),

    /**
     * @see MethodKeys#WALLET_PAYPAL
     */
    WALLET_PAYPAL(MethodKeys.WALLET_PAYPAL,
                    TransactionType.AUTHORIZATION,
                    TransactionType.CHARGE),

    /**
     * @see MethodKeys#BANK_TRANSFER_SOFORTUEBERWEISUNG
     */
    BANK_TRANSFER_SOFORTUEBERWEISUNG(MethodKeys.BANK_TRANSFER_SOFORTUEBERWEISUNG,
            TransactionType.AUTHORIZATION,
            TransactionType.CHARGE
    ),

    /**
     * @see MethodKeys#BANK_TRANSFER_ADVANCE
     */
    BANK_TRANSFER_ADVANCE(MethodKeys.BANK_TRANSFER_ADVANCE,
                    TransactionType.AUTHORIZATION,
                    TransactionType.CHARGE // not supported by the documentation, but works, hence left for the backward compatibility
    ),

    /**
     * @see MethodKeys#BANK_TRANSFER_SOFORTUEBERWEISUNG
     */
    BANK_TRANSFER_POSTFINANCE_EFINANCE(MethodKeys.BANK_TRANSFER_POSTFINANCE_EFINANCE,
            TransactionType.AUTHORIZATION,
            TransactionType.CHARGE
    ),

    /**
     * @see MethodKeys#BANK_TRANSFER_SOFORTUEBERWEISUNG
     */
    BANK_TRANSFER_POSTFINANCE_CARD(MethodKeys.BANK_TRANSFER_POSTFINANCE_CARD,
            TransactionType.AUTHORIZATION,
            TransactionType.CHARGE
    );

    private String key;
    private ImmutableSet<TransactionType> supportedTransactionTypes;

    PaymentMethod(final String key, final TransactionType... supportedTransactionTypes) {
        this.key = key;
        this.supportedTransactionTypes = ImmutableSet.copyOf(supportedTransactionTypes);
    }

    /**
     * Gets the method key.
     * @return the method key, never null
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the transaction types supported by this payment method.
     * @return the supported transaction types, never null
     */
    public ImmutableSet<TransactionType> getSupportedTransactionTypes() {
        return supportedTransactionTypes;
    }

    /**
     * Gets the payment method from the provided key.
     *
     * @param methodKey the key (aka name aka identifier) of the wanted method
     * @return the requested payment mwethod
     * @throws IllegalArgumentException in case of an invalid method key
     */
    public static PaymentMethod fromMethodKey(final String methodKey) {
        final PaymentMethod result = Arrays.stream(PaymentMethod.values())
                .filter(value -> value.key.equals(methodKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid PaymentMethod: %s", methodKey)));
        return result;
    }
}
