package com.commercetools.pspadapter.payone.domain.ctp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.types.DateTimeType;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.StringType;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.queries.TypeQuery;

/**
 * @author Jan Wolter
 */
public class CustomTypeBuilder {
    // TODO jw: not that custom, general type for all PSPs, move somewhere else
    public static final String PAYMENT_CREDIT_CARD = "payment-CREDIT_CARD";
    public static final String CARD_DATA_PLACEHOLDER_FIELD = "cardDataPlaceholder";

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
    public static final String REDIRECT_URL = "redirectUrl";
    public static final String NOTIFICATION_FIELD = "notification";
    public static final String MESSAGE_FIELD = "message";
    public static final boolean REQUIRED = true;

    final BlockingClient client;

    public CustomTypeBuilder(final BlockingClient client) {
        this.client = client;
    }

    public void run() {
        final FieldDefinition timestampField = FieldDefinition.of(
                DateTimeType.of(),
                TIMESTAMP_FIELD,
                LocalizedString.ofEnglishLocale(TIMESTAMP_FIELD),
                REQUIRED,
                TextInputHint.SINGLE_LINE);

        final FieldDefinition transactionIdField = FieldDefinition.of(
                StringType.of(),
                TRANSACTION_ID_FIELD,
                LocalizedString.ofEnglishLocale(TRANSACTION_ID_FIELD),
                REQUIRED,
                TextInputHint.SINGLE_LINE);

        createInteractionRequest(timestampField, transactionIdField);
        createInteractionResponse(timestampField, transactionIdField);
        createInteractionRedirect(timestampField, transactionIdField);
        createInteractionNotification(timestampField, transactionIdField);
        createInteractionTemporaryError(timestampField, transactionIdField);
    }

    private Type createInteractionRequest(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        return createType(PAYONE_INTERACTION_REQUEST, ImmutableList.of(timestampField, transactionIdField, createMultiLineStringFieldDefinition(REQUEST_FIELD)));
    }

    private Type createInteractionResponse(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        return createType(PAYONE_INTERACTION_RESPONSE, ImmutableList.of(timestampField, transactionIdField, createMultiLineStringFieldDefinition(RESPONSE_FIELD)));
    }

    private Type createInteractionRedirect(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        return createType(PAYONE_INTERACTION_REDIRECT,
            ImmutableList.of(timestampField, transactionIdField, createSingleLineStringFieldDefinition(REDIRECT_URL), createMultiLineStringFieldDefinition(RESPONSE_FIELD)));
    }

    private Type createInteractionTemporaryError(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        return createType(PAYONE_INTERACTION_TEMPORARY_ERROR, ImmutableList.of(timestampField, transactionIdField, createMultiLineStringFieldDefinition(RESPONSE_FIELD)));
    }

    private Type createInteractionNotification(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        return createType(PAYONE_INTERACTION_NOTIFICATION, ImmutableList.of(timestampField, transactionIdField, createMultiLineStringFieldDefinition(NOTIFICATION_FIELD)));
    }

    private Type createPayoneUnsupportedTransaction(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        final FieldDefinition messageField = FieldDefinition.of(
            StringType.of(),
            MESSAGE_FIELD,
            LocalizedString.ofEnglishLocale(MESSAGE_FIELD),
            REQUIRED,
            TextInputHint.SINGLE_LINE);

        return createType(PAYONE_UNSUPPORTED_TRANSACTION, ImmutableList.of(timestampField, transactionIdField, createSingleLineStringFieldDefinition(MESSAGE_FIELD)));
    }

    private Type createType(String typeKey, ImmutableList<FieldDefinition> fieldDefinitions) {
        // TODO replace with cache
        final PagedQueryResult<Type> result = client.complete(
                TypeQuery.of()
                        .withPredicates(m -> m.key().is(typeKey))
                        .withLimit(1));

        if (result.getTotal() == 0) {
            return client.complete(TypeCreateCommand.of(TypeDraftBuilder.of(
                typeKey,
                LocalizedString.ofEnglishLocale(typeKey),
                ImmutableSet.of(AddInterfaceInteraction.resourceTypeId()))
                .fieldDefinitions(
                    fieldDefinitions)
                .build()));
        }
        else {
            return result.getResults().get(0);
        }
    }

    private FieldDefinition createSingleLineStringFieldDefinition(final String fieldName) {
        return createFieldDefinition(fieldName, TextInputHint.SINGLE_LINE);
    }

    private FieldDefinition createMultiLineStringFieldDefinition(final String fieldName) {
        return createFieldDefinition(fieldName, TextInputHint.MULTI_LINE);
    }

    private FieldDefinition createFieldDefinition(final String fieldName, TextInputHint inputHint) {
        return FieldDefinition.of(
                    StringType.of(),
                    fieldName,
                    LocalizedString.ofEnglishLocale(fieldName),
                    REQUIRED,
                    inputHint);
    }
}
