package com.commercetools.pspadapter.payone.domain.ctp;

import com.commercetools.pspadapter.payone.mapping.CreditCardNetwork;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import io.sphere.sdk.carts.commands.CartDeleteCommand;
import io.sphere.sdk.carts.queries.CartQuery;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.orders.commands.OrderDeleteCommand;
import io.sphere.sdk.orders.queries.OrderQuery;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.commands.PaymentDeleteCommand;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.queries.PaymentQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.types.BooleanType;
import io.sphere.sdk.types.DateType;
import io.sphere.sdk.types.EnumType;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.FieldType;
import io.sphere.sdk.types.StringType;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import io.sphere.sdk.types.queries.TypeQuery;

/**
 * @author Jan Wolter
 */
public class CustomTypeBuilder {
    // TODO jw: not that custom, general type for all PSPs, move somewhere else
    public static final String PAYMENT_CREDIT_CARD = "payment-CREDIT_CARD";

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

    public static final String PAYONE_INTERACTION_REQUEST = "PAYONE_INTERACTION_REQUEST";
    public static final String PAYONE_INTERACTION_RESPONSE = "PAYONE_INTERACTION_RESPONSE";
    public static final String PAYONE_INTERACTION_REDIRECT = "PAYONE_INTERACTION_REDIRECT";
    public static final String PAYONE_INTERACTION_NOTIFICATION = "PAYONE_INTERACTION_NOTIFICATION";
    public static final String PAYONE_INTERACTION_TEMPORARY_ERROR = "PAYONE_INTERACTION_TEMPORARY_ERROR";
    public static final String PAYONE_UNSUPPORTED_TRANSACTION = "PAYONE_UNSUPPORTED_TRANSACTION";

    public static final String TIMESTAMP_FIELD = "timestamp";
    public static final String TRANSACTION_ID_FIELD = "transactionId";
    public static final String REQUEST_FIELD = "request";
    public static final String RESPONSE_FIELD = "response";

    public static final String NOTIFICATION_FIELD = "notification";
    public static final String MESSAGE_FIELD = "message";

    public enum PermissionToStartFromScratch {
        /**
         * Permission to erase anything is denied.
         */
        DENIED,

        /**
         * Permission to erase something is granted.
         */
        GRANTED;

        /**
         * Gets the proper enum value.
         * @param permission whether or not permission to erase shall be granted
         * @return {@link #GRANTED} if permission is true, {@link #DENIED} otherwise
         */
        public static PermissionToStartFromScratch fromBoolean(final boolean permission) {
            if (permission) {
                return GRANTED;
            }

            return DENIED;
        }
    }

    private final BlockingClient ctpClient;
    private final PermissionToStartFromScratch permissionToStartFromScratch;

    public CustomTypeBuilder(final BlockingClient ctpClient, final PermissionToStartFromScratch permissionToErase) {
        this.ctpClient = ctpClient;
        this.permissionToStartFromScratch = permissionToErase;
    }

    public void run() {
        switch (permissionToStartFromScratch) {
            case GRANTED:
                resetPlatform();
                break;

            default:
                break;
        }

        createPaymentProviderAgnosticTypes();
        createPayoneSpecificTypes();
    }

    private void resetPlatform() {
        // TODO jw: use futures
        // delete all orders
        ctpClient.complete(OrderQuery.of().withLimit(500)).getResults()
                .forEach(order -> ctpClient.complete(OrderDeleteCommand.of(order)));

        // delete all carts
        ctpClient.complete(CartQuery.of().withLimit(500)).getResults()
                .forEach(cart -> ctpClient.complete(CartDeleteCommand.of(cart)));

        // delete all payments
        ctpClient.complete(PaymentQuery.of().withLimit(500)).getResults()
                .forEach(payment -> ctpClient.complete(PaymentDeleteCommand.of(payment)));

        ctpClient.complete(TypeQuery.of().withLimit(500)).getResults()
                .forEach(type -> ctpClient.complete(TypeDeleteCommand.of(type)));
    }

    private void createPaymentProviderAgnosticTypes() {
        createPaymentCustomType(PAYMENT_CREDIT_CARD, ImmutableList.of(
                createSingleLineStringFieldDefinition(LANGUAGE_CODE_FIELD, FieldClassifier.REQUIRED),
                createSingleLineStringFieldDefinition(REFERENCE_FIELD, FieldClassifier.REQUIRED),
                createMultiLineStringFieldDefinition(REFERENCE_TEXT_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(REDIRECT_URL_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(SUCCESS_URL_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(ERROR_URL_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CANCEL_URL_FIELD, FieldClassifier.OPTIONAL),
                createFieldDefinition(BooleanType.of(), FORCE_3DSECURE_FIELD, null, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CARD_DATA_PLACEHOLDER_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(TRUNCATED_CARD_NUMBER_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CARD_HOLDER_NAME_FIELD, FieldClassifier.OPTIONAL),
                createFieldDefinition(DateType.of(), CARD_EXPIRY_DATE_FIELD, null, FieldClassifier.OPTIONAL),
                createFieldDefinition(EnumType.of(CreditCardNetwork.getValuesAsListOfEnumValue()), CARD_NETWORK_FIELD, null, FieldClassifier.OPTIONAL)
        ));
    }

    private void createPayoneSpecificTypes() {
        final FieldDefinition timestampField = createSingleLineStringFieldDefinition(TIMESTAMP_FIELD, FieldClassifier.REQUIRED);
        final FieldDefinition transactionIdField = createSingleLineStringFieldDefinition(TRANSACTION_ID_FIELD, FieldClassifier.REQUIRED);

        createInteractionRequest(timestampField, transactionIdField);
        createInteractionResponse(timestampField, transactionIdField);
        createInteractionRedirect(timestampField, transactionIdField);
        createInteractionNotification(timestampField, transactionIdField);
        createInteractionTemporaryError(timestampField, transactionIdField);
        createPayoneUnsupportedTransaction(timestampField, transactionIdField);
    }

    private Type createInteractionRequest(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        return createInterfaceInteractionType(PAYONE_INTERACTION_REQUEST, ImmutableList.of(timestampField, transactionIdField, createMultiLineStringFieldDefinition(REQUEST_FIELD,  FieldClassifier.REQUIRED)));
    }

    private Type createInteractionResponse(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        return createInterfaceInteractionType(PAYONE_INTERACTION_RESPONSE, ImmutableList.of(timestampField, transactionIdField, createMultiLineStringFieldDefinition(RESPONSE_FIELD, FieldClassifier.REQUIRED)));
    }

    private Type createInteractionRedirect(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        return createInterfaceInteractionType(PAYONE_INTERACTION_REDIRECT,
                ImmutableList.of(timestampField, transactionIdField, createSingleLineStringFieldDefinition(REDIRECT_URL_FIELD, FieldClassifier.REQUIRED), createMultiLineStringFieldDefinition(RESPONSE_FIELD, FieldClassifier.REQUIRED)));
    }

    private Type createInteractionTemporaryError(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        return createInterfaceInteractionType(PAYONE_INTERACTION_TEMPORARY_ERROR, ImmutableList.of(timestampField, transactionIdField, createMultiLineStringFieldDefinition(RESPONSE_FIELD, FieldClassifier.REQUIRED)));
    }

    private Type createInteractionNotification(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        return createInterfaceInteractionType(PAYONE_INTERACTION_NOTIFICATION, ImmutableList.of(timestampField, transactionIdField, createMultiLineStringFieldDefinition(NOTIFICATION_FIELD, FieldClassifier.REQUIRED)));
    }

    private Type createPayoneUnsupportedTransaction(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        return createInterfaceInteractionType(PAYONE_UNSUPPORTED_TRANSACTION, ImmutableList.of(timestampField, transactionIdField, createSingleLineStringFieldDefinition(MESSAGE_FIELD, FieldClassifier.REQUIRED)));
    }

    private Type createInterfaceInteractionType(final String typeKey, final ImmutableList<FieldDefinition> fieldDefinitions) {
        return createType(typeKey, fieldDefinitions, AddInterfaceInteraction.resourceTypeId());
    }

    private Type createPaymentCustomType(final String typeKey, final ImmutableList<FieldDefinition> fieldDefinitions) {
        return createType(typeKey, fieldDefinitions, Payment.resourceTypeId());
    }

    private Type createType(final String typeKey, final ImmutableList<FieldDefinition> fieldDefinitions, final String resourceTypeId) {
        // TODO replace with cache
        final PagedQueryResult<Type> result = ctpClient.complete(
                TypeQuery.of()
                        .withPredicates(m -> m.key().is(typeKey))
                        .withLimit(1));

        if (result.getTotal() == 0) {
            return ctpClient.complete(TypeCreateCommand.of(TypeDraftBuilder.of(
                    typeKey,
                    LocalizedString.ofEnglishLocale(typeKey),
                    ImmutableSet.of(resourceTypeId))
                    .fieldDefinitions(
                            fieldDefinitions)
                    .build()));
        }
        else {
            return result.getResults().get(0);
        }
    }

    private FieldDefinition createSingleLineStringFieldDefinition(final String fieldName, final FieldClassifier classifier) {
        return createFieldDefinition(StringType.of(), fieldName, TextInputHint.SINGLE_LINE, classifier);
    }

    private FieldDefinition createMultiLineStringFieldDefinition(final String fieldName, final FieldClassifier classifier) {
        return createFieldDefinition(StringType.of(), fieldName, TextInputHint.MULTI_LINE, classifier);
    }

    private FieldDefinition createFieldDefinition(final FieldType fieldType, final String fieldName, final TextInputHint inputHint, final FieldClassifier classifier) {
        return FieldDefinition.of(
                    fieldType,
                    fieldName,
                    LocalizedString.ofEnglishLocale(fieldName),
                    classifier.toBoolean(),
                    inputHint);
    }

    private enum FieldClassifier {
        REQUIRED(true),
        OPTIONAL(false);

        private boolean value;

        FieldClassifier(final boolean value) {
            this.value = value;
        }

        public boolean toBoolean() {
            return value;
        }
    }
}
