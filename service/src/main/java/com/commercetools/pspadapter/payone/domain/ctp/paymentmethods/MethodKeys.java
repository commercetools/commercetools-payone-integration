package com.commercetools.pspadapter.payone.domain.ctp.paymentmethods;

/**
 * This file is generated from Method-Keys.md. Do not edit!
 * See https://github.com/nkuehn/payment-integration-specifications/tree/scala-tools
 */
public class MethodKeys {

    /**
     * Used e.g. for POS purchases
     */
    public static final String CASH = "CASH";

    /**
     * Cash received in advance before goods are shipped (this is actual Cash, please see BANK_TRANSFER-ADVANCE below for the typical case)
     */
    public static final String CASH_ADVANCE = "CASH_ADVANCE";

    /**
     * Delivery Service collects the Money from the buyer. The "PSP" can be directly the delivery service or a wrapper API
     */
    public static final String CASH_ON_DELIVERY = "CASH_ON_DELIVERY";

    /**
     * Goods are paid _after_ delivery based on an invoice. Can be done internally by the merchant or via a third party. Use this value if the specific way is encapsulated in the PSP or integration
     */
    public static final String INVOICE = "INVOICE";

    /**
     * Direct Invoice by the Merchant
     */
    public static final String INVOICE_DIRECT = "INVOICE-DIRECT";

    public static final String INVOICE_KLARNA = "INVOICE-KLARNA";

    /**
     * Money to be received by merchant in advance via Check / Money Order
     */
    public static final String CHECK_ADVANCE = "CHECK-ADVANCE";

    public static final String DIRECT_DEBIT = "DIRECT_DEBIT";

    /**
     * European Direct Debit System. Formerly called ELV in Germany
     */
    public static final String DIRECT_DEBIT_SEPA = "DIRECT_DEBIT-SEPA";

    /**
     * Can be realized by the merchant or via a 3rd party. Use this value if the selection of method is encapsulated by the PSP
     */
    public static final String INSTALLMENT = "INSTALLMENT";

    /**
     * Direct Credit Contract with the Merchant
     */
    public static final String INSTALLMENT_DIRECT = "INSTALLMENT-DIRECT";

    public static final String INSTALLMENT_BILLSAFE = "INSTALLMENT-BILLSAFE";

    public static final String INSTALLMENT_KLARNA = "INSTALLMENT-KLARNA";

    /**
     * Credit Card
     * Generic Key for any type of Credit Card (recommended, implementation will autodetect specific variant)
     */
    public static final String CREDIT_CARD = "CREDIT_CARD";

    /**
     * (probably never directly used)
     */
    public static final String DEBIT_CARD = "DEBIT_CARD";

    public static final String DEBIT_CARD_MAESTRO = "DEBIT_CARD-MAESTRO";

    public static final String DEBIT_CARD_CARTE_BLEUE = "DEBIT_CARD-CARTE_BLEUE";

    /**
     * (never directly used)
     */
    public static final String CREDIT = "CREDIT";

    /**
     * PayPal
     * The PayPal Wallet
     */
    public static final String WALLET_PAYPAL = "WALLET-PAYPAL";

    /**
     * Paydirekt
     */
    public static final String WALLET_PAYDIREKT = "WALLET-PAYDIREKT";

    /**
     * Skrill Digital Wallet
     */
    public static final String WALLET_SKRILL = "WALLET-SKRILL";

    /**
     * moneta.ru
     */
    public static final String WALLET_MONETA = "WALLET-MONETA";

    /**
     * (probably not used directly)
     */
    public static final String BANK_TRANSFER = "BANK_TRANSFER";

    /**
     * Money received by merchant before delivery via Bank Transfer
     */
    public static final String BANK_TRANSFER_ADVANCE = "BANK_TRANSFER-ADVANCE";

    /**
     * Austrian eps system www.stuzza.at
     */
    public static final String BANK_TRANSFER_EPS = "BANK_TRANSFER-EPS";

    /**
     * A direct debit style system backed by some german banks
     */
    public static final String BANK_TRANSFER_GIROPAY = "BANK_TRANSFER-GIROPAY";

    /**
     * pay via you online banking system
     */
    public static final String BANK_TRANSFER_SOFORTUEBERWEISUNG = "BANK_TRANSFER-SOFORTUEBERWEISUNG";

    /**
     * Netherlands specific
     */
    public static final String BANK_TRANSFER_IDEAL = "BANK_TRANSFER-IDEAL";

    /**
     * Switzerland specific
     */
    public static final String BANK_TRANSFER_POSTFINANCE_EFINANCE = "BANK_TRANSFER-POSTFINANCE_EFINANCE";

    /**
     * Switzerland specific
     */
    public static final String BANK_TRANSFER_POSTFINANCE_CARD = "BANK_TRANSFER-POSTFINANCE_CARD";

    /**
     * (two dashes!) Credit Card with explicit whish for 3Dsecure check
     * Will autodetect the card network, but force 3Dsecure check redirect
     */
    public static final String CREDIT_CARD__3DSECURE = "CREDIT_CARD--3DSECURE";

    /**
     * In case explicit restriction to MasterCard is intended
     */
    public static final String CREDIT_CARD_MASTERCARD = "CREDIT_CARD-MASTERCARD";

    /**
     * 3Dsecure of MasterCard
     */
    public static final String CREDIT_CARD_MASTERCARD_SECURECODE = "CREDIT_CARD-MASTERCARD-SECURECODE";

    public static final String CREDIT_CARD_VISA = "CREDIT_CARD-VISA";

    /**
     * 3Dsecure of VISA
     */
    public static final String CREDIT_CARD_VISA_VERIFIED_BY_VISA = "CREDIT_CARD-VISA-VERIFIED_BY_VISA";

    public static final String CREDIT_CARD_AMERICAN_EXPRESS = "CREDIT_CARD-AMERICAN_EXPRESS";

    /**
     * 3Dsecure of Amex
     */
    public static final String CREDIT_CARD_AMERICAN_EXPRESS_SAFE_KEY = "CREDIT_CARD-AMERICAN_EXPRESS-SAFE_KEY";

    public static final String CREDIT_CARD_DINERS_CLUB = "CREDIT_CARD-DINERS_CLUB";

    public static final String CREDIT_CARD_DISCOVER = "CREDIT_CARD-DISCOVER";

    public static final String CREDIT_CARD_JCB = "CREDIT_CARD-JCB";
}
