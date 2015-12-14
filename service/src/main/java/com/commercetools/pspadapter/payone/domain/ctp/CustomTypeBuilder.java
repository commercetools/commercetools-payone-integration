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
    public static final String PAYONE_INTERACTION_REQUEST = "PAYONE_INTERACTION_REQUEST";
    public static final String PAYONE_UNSUPPORTED_TRANSACTION = "PAYONE_UNSUPPORTED_TRANSACTION";
    public static final String TIMESTAMP_FIELD = "timestamp";
    public static final String TRANSACTION_ID_FIELD = "transactionId";
    public static final String MESSAGE_FIELD = "message";
    public static final boolean REQUIRED = true;

    final BlockingClient client;

    public CustomTypeBuilder(final BlockingClient client) {
        this.client = client;
    }

    public void run() {
        final PagedQueryResult<Type> result = client.complete(
                TypeQuery.of()
                        .withPredicates(m -> m.key().is(PAYONE_INTERACTION_REQUEST))
                        .withLimit(1));

        if (result.getTotal() == 0) {
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

            // TODO jw: add other fields and types

            // TODO jw: cache id <-> name
            final Type payoneInteractionRequestType = createPayoneInteractionRequestType(timestampField, transactionIdField);
            final Type payoneInteractionUnsupportedTransactionType = createPayoneUnsupportedTransactionType(timestampField, transactionIdField);
        }
    }

    private Type createPayoneInteractionRequestType(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        return client.complete(TypeCreateCommand.of(TypeDraftBuilder.of(
                PAYONE_INTERACTION_REQUEST,
                LocalizedString.ofEnglishLocale(PAYONE_INTERACTION_REQUEST),
                ImmutableSet.of(AddInterfaceInteraction.resourceTypeId()))
                .fieldDefinitions(ImmutableList.of(timestampField, transactionIdField))
                .build()));
    }

    private Type createPayoneUnsupportedTransactionType(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        final FieldDefinition messageField = FieldDefinition.of(
                StringType.of(),
                MESSAGE_FIELD,
                LocalizedString.ofEnglishLocale(MESSAGE_FIELD),
                REQUIRED,
                TextInputHint.SINGLE_LINE);

        return client.complete(TypeCreateCommand.of(TypeDraftBuilder.of(
                PAYONE_UNSUPPORTED_TRANSACTION,
                LocalizedString.ofEnglishLocale(PAYONE_UNSUPPORTED_TRANSACTION),
                ImmutableSet.of(AddInterfaceInteraction.resourceTypeId()))
                .fieldDefinitions(ImmutableList.of(timestampField, transactionIdField, messageField))
                .build()));
    }
}
