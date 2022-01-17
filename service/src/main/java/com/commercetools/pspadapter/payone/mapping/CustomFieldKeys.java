package com.commercetools.pspadapter.payone.mapping;

/**
 * @author fhaertig
 * @since 14.12.15
 */
public final class CustomFieldKeys {

    public static final String LANGUAGE_CODE_FIELD = "languageCode";
    public static final String REFERENCE_FIELD = "reference";
    public static final String REFERENCE_TEXT_FIELD = "referenceText";
    public static final String REDIRECT_URL_FIELD = "redirectUrl";
    public static final String SUCCESS_URL_FIELD = "successUrl";
    public static final String ERROR_URL_FIELD = "errorUrl";
    public static final String CANCEL_URL_FIELD = "cancelUrl";
    public static final String FORCE_3DSECURE_FIELD = "force3DSecure";
    public static final String CARD_DATA_PLACEHOLDER_FIELD = "cardDataPlaceholder";
    public static final String TRUNCATED_CARD_NUMBER_FIELD = "truncatedCardNumber";
    public static final String CARD_HOLDER_NAME_FIELD = "cardHolderName";
    public static final String CARD_EXPIRY_DATE_FIELD = "cardExpiryDate";
    public static final String CARD_NETWORK_FIELD = "cardNetwork";
    public static final String IBAN_FIELD = "IBAN";
    public static final String BIC_FIELD = "BIC";


    public static final String TIMESTAMP_FIELD = "timestamp";
    public static final String TRANSACTION_ID_FIELD = "transactionId";
    public static final String SEQUENCE_NUMBER_FIELD = "sequenceNumber";
    public static final String TX_ACTION_FIELD = "txAction";
    public static final String REQUEST_FIELD = "request";
    public static final String RESPONSE_FIELD = "response";

    public static final String NOTIFICATION_FIELD = "notification";
    public static final String MESSAGE_FIELD = "message";

    public static final String USER_ID_FIELD = "payoneUserId";

    public static final String PAID_FROM_NAME_FIELD = "paidFromAccountHolderName";
    public static final String PAID_FROM_IBAN_FIELD = "paidFromIBAN";
    public static final String PAID_FROM_BIC_FIELD = "paidFromBIC";
    public static final String REFUND_TO_NAME_FIELD = "refundToAccountHolderName";
    public static final String REFUND_TO_IBAN_FIELD = "refundToIBAN";
    public static final String REFUND_TO_BIC_FIELD = "refundToBIC";
    public static final String PAY_TO_NAME_FIELD = "paidToAccountHolderName";
    public static final String PAY_TO_IBAN_FIELD = "paidToIBAN";
    public static final String PAY_TO_BIC_FIELD = "paidToBIC";

    // Klarna

    // KlarnaConstants.Gender
    public static final String GENDER_FIELD = "gender";
    // IP_FIELD address, IPv4 or IPv6
    public static final String IP_FIELD = "ip";
    // Date of birth (YYYYMMDD), Mandatory for DE, NE and AT
    public static final String BIRTHDAY_FIELD = "birthday";
    // if the value is set - overrides optional value from address.
    public static final String TELEPHONENUMBER_FIELD = "telephonenumber";
    // Response from start_Session request to Klarna as JSON String
    public static final String START_SESSION_RESPONSE = "startSessionResponse";
    // start_Session request to Klarna as JSON String
    public static final String START_SESSION_REQUEST = "startSessionRequest";
    // this token is needed to load klarna widget
    public static final String CLIENT_TOKEN = "clientToken";
    // This token is needed to send authorization request from Klarna widget to Klarna
    public static final String AUTHORIZATION_TOKEN = "authorizationToken";
    //URL for "Back" or "Cancel"
    public static final String BACK_URL_FIELD = "backUrl";
    // Klarna responds with redirectUrl on pre-authorization request
    public static final String KLARNA_REDIRECT_URL_FIELD = "redirectUrl";
    //The workorderid is a technical id returned from the PAYONE platform to identify a workorder
    public static final String WORK_ORDER_ID_FIELD = "workorderid";
    // iDeal
    public static final String BANK_GROUP_TYPE = "bankGroupType";
    public static final String BANK_COUNTRY = "bankCountry";

    private CustomFieldKeys() {
    }
}
