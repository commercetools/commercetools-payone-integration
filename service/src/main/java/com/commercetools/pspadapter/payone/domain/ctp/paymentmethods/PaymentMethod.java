package com.commercetools.pspadapter.payone.domain.ctp.paymentmethods;

import com.google.common.collect.ImmutableSet;
import io.sphere.sdk.payments.TransactionType;

import java.util.HashSet;
import java.util.Set;

public enum PaymentMethod {

    /**
     * Used e.g. for POS purchases
     */
    CASH("CASH"),

    /**
     * Cash received in advance before goods are shipped (this is actual Cash, please see BANK_TRANSFER-ADVANCE below for the typical case)
     */
    CASH_ADVANCE("CASH_ADVANCE"),

    /**
     * Delivery Service collects the Money from the buyer. The "PSP" can be directly the delivery service or a wrapper API
     */
    CASH_ON_DELIVERY("CASH_ON_DELIVERY"),

    /**
     * Goods are paid _after_ delivery based on an invoice. Can be done internally by the merchant or via a third party. Use this value if the specific way is encapsulated in the PSP or integration
     */
    INVOICE("INVOICE"),

    /**
     * Direct Invoice by the Merchant
     */
    INVOICE_DIRECT("INVOICE-DIRECT"),

    INVOICE_KLARNA("INVOICE-KLARNA"),

    /**
     * Money to be received by merchant in advance via Check / Money Order
     */
    CHECK_ADVANCE("CHECK-ADVANCE"),

    DIRECT_DEBIT("DIRECT_DEBIT"),

    /**
     * European Direct Debit System. Formerly called ELV in Germany
     */
    DIRECT_DEBIT_SEPA("DIRECT_DEBIT-SEPA",
            ImmutableSet.of(
                    TransactionType.AUTHORIZATION)
    ),

    /**
     * Can be realized by the merchant or via a 3rd party. Use this value if the selection of method is encapsulated by the PSP
     */
    INSTALLMENT("INSTALLMENT"),

    /**
     * Direct Credit Contract with the Merchant
     */
    INSTALLMENT_DIRECT("INSTALLMENT-DIRECT"),

    INSTALLMENT_BILLSAFE("INSTALLMENT-BILLSAFE"),

    INSTALLMENT_KLARNA("INSTALLMENT-KLARNA"),

    /**
     * Credit Card
     * Generic Key for any type of Credit Card (recommended)
     */
    CREDIT_CARD("CREDIT_CARD",
            ImmutableSet.of(
                    TransactionType.AUTHORIZATION,
                    TransactionType.CHARGE)
    ),

    /**
     * In case explicit restriction to cardNetwork is intended
     */
    CREDIT_CARD_MASTERCARD("CREDIT_CARD-MASTERCARD"),

    CREDIT_CARD_VISA("CREDIT_CARD-VISA"),

    CREDIT_CARD_AMERICAN_EXPRESS("CREDIT_CARD-AMERICAN_EXPRESS"),

    CREDIT_CARD_DINERS_CLUB("CREDIT_CARD-DINERS_CLUB"),

    CREDIT_CARD_DISCOVER("CREDIT_CARD-DISCOVER"),

    CREDIT_CARD_JCB("CREDIT_CARD-JCB"),

    /**
     * (probably never directly used)
     */
    DEBIT_CARD("DEBIT_CARD"),

    DEBIT_CARD_MAESTRO("DEBIT_CARD-MAESTRO"),

    DEBIT_CARD_CARTE_BLEUE("DEBIT_CARD-CARTE_BLEUE"),

    /**
     * (never directly used)
     */
    CREDIT("CREDIT"),

    /**
     * PayPal
     * The PayPal Wallet
     */
    WALLET_PAYPAL("WALLET-PAYPAL"),

    /**
     * Skrill Digital Wallet
     */
    WALLET_SKRILL("WALLET-SKRILL"),

    /**
     * moneta.ru
     */
    WALLET_MONETA("WALLET-MONETA"),

    /**
     * (probably not used directly)
     */
    BANK_TRANSFER("BANK_TRANSFER"),

    /**
     * Money received by merchant before delivery via Bank Transfer
     */
    BANK_TRANSFER_ADVANCE("BANK_TRANSFER-ADVANCE"),

    /**
     * Austrian eps system www.stuzza.at
     */
    BANK_TRANSFER_EPS("BANK_TRANSFER-EPS"),

    /**
     * A direct debit style system backed by some german banks
     */
    BANK_TRANSFER_GIROPAY("BANK_TRANSFER-GIROPAY"),

    /**
     * pay via you online banking system
     */
    BANK_TRANSFER_SOFORTUEBERWEISUNG("BANK_TRANSFER-SOFORTUEBERWEISUNG"),

    /**
     * Netherlands specific
     */
    BANK_TRANSFER_IDEAL("BANK_TRANSFER-IDEAL"),

    /**
     * Switzerland specific
     */
    BANK_TRANSFER_POSTFINANCE_EFINANCE("BANK_TRANSFER-POSTFINANCE_EFINANCE"),

    /**
     * Switzerland specific
     */
    BANK_TRANSFER_POSTFINANCE_CARD("BANK_TRANSFER-POSTFINANCE_CARD");


    private String key;
    private Set<TransactionType> supportedTransactionTypes;

    PaymentMethod(final String key) {
        this(key, new HashSet<>());
    }

    PaymentMethod(final String key, final Set<TransactionType> supportedTransactionTypes) {
        this.key = key;
        this.supportedTransactionTypes = supportedTransactionTypes;
    }

    public String getKey() {
        return key;
    }

    public Set<TransactionType> getSupportedTransactionTypes() {
        return supportedTransactionTypes;
    }

    public static PaymentMethod getMethodFromString(final String methodString) {
        try {
            return PaymentMethod.valueOf(methodString);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
