package com.commercetools.pspadapter.payone.domain.ctp;

import com.commercetools.pspadapter.payone.mapping.CreditCardNetwork;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.sphere.sdk.carts.commands.CartDeleteCommand;
import io.sphere.sdk.carts.queries.CartQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.orders.commands.OrderDeleteCommand;
import io.sphere.sdk.orders.queries.OrderQuery;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.commands.PaymentDeleteCommand;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.queries.PaymentQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.types.*;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import io.sphere.sdk.types.queries.TypeQuery;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Jan Wolter
 */
public class CustomTypeBuilder {
    // TODO jw: not that custom, general type for all PSPs, move somewhere else
    public static final String PAYMENT_CREDIT_CARD = "payment-CREDIT_CARD";
    public static final String PAYMENT_WALLET = "payment-WALLET";
    public static final String PAYMENT_BANK_TRANSFER = "payment-BANK_TRANSFER";
    public static final String PAYMENT_CASH_ADVANCE = "payment-CASH-ADVANCE";
    public static final String PAYMENT_INVOICE_KLARNA = "payment-INVOICE-KLARNA";

    public static final String PAYONE_INTERACTION_REQUEST = "PAYONE_INTERACTION_REQUEST";
    public static final String PAYONE_INTERACTION_RESPONSE = "PAYONE_INTERACTION_RESPONSE";
    public static final String PAYONE_INTERACTION_REDIRECT = "PAYONE_INTERACTION_REDIRECT";
    public static final String PAYONE_INTERACTION_NOTIFICATION = "PAYONE_INTERACTION_NOTIFICATION";
    public static final String PAYONE_INTERACTION_TEMPORARY_ERROR = "PAYONE_INTERACTION_TEMPORARY_ERROR";
    public static final String PAYONE_UNSUPPORTED_TRANSACTION = "PAYONE_UNSUPPORTED_TRANSACTION";

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
         *
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

    private final BlockingSphereClient ctpClient;
    private final PermissionToStartFromScratch permissionToStartFromScratch;

    public CustomTypeBuilder(final BlockingSphereClient ctpClient, final PermissionToStartFromScratch permissionToErase) {
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

    public PermissionToStartFromScratch getPermissionToStartFromScratch() {
        return permissionToStartFromScratch;
    }

    private void resetPlatform() {
        // TODO jw: use futures
        // delete all orders
        ctpClient.executeBlocking(OrderQuery.of().withLimit(500)).getResults()
                .forEach(order -> ctpClient.executeBlocking(OrderDeleteCommand.of(order)));

        // delete all carts
        ctpClient.executeBlocking(CartQuery.of().withLimit(500)).getResults()
                .forEach(cart -> ctpClient.executeBlocking(CartDeleteCommand.of(cart)));

        // delete all payments
        ctpClient.executeBlocking(PaymentQuery.of().withLimit(500)).getResults()
                .forEach(payment -> ctpClient.executeBlocking(PaymentDeleteCommand.of(payment)));

        ctpClient.executeBlocking(TypeQuery.of().withLimit(500)).getResults()
                .forEach(type -> ctpClient.executeBlocking(TypeDeleteCommand.of(type)));
    }

    private void createPaymentProviderAgnosticTypes() {
        createPaymentCustomType(PAYMENT_CREDIT_CARD, ImmutableList.of(
                createSingleLineStringFieldDefinition(CustomFieldKeys.LANGUAGE_CODE_FIELD, FieldClassifier.REQUIRED),
                createSingleLineStringFieldDefinition(CustomFieldKeys.REFERENCE_FIELD, FieldClassifier.REQUIRED),
                createMultiLineStringFieldDefinition(CustomFieldKeys.REFERENCE_TEXT_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.REDIRECT_URL_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.SUCCESS_URL_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.ERROR_URL_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.CANCEL_URL_FIELD, FieldClassifier.OPTIONAL),
                createFieldDefinition(BooleanFieldType.of(), CustomFieldKeys.FORCE_3DSECURE_FIELD, null, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.CARD_DATA_PLACEHOLDER_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.TRUNCATED_CARD_NUMBER_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.CARD_HOLDER_NAME_FIELD, FieldClassifier.OPTIONAL),
                createFieldDefinition(DateFieldType.of(), CustomFieldKeys.CARD_EXPIRY_DATE_FIELD, null, FieldClassifier.OPTIONAL),
                createFieldDefinition(EnumFieldType.of(CreditCardNetwork.getValuesAsListOfEnumValue()), CustomFieldKeys.CARD_NETWORK_FIELD, null, FieldClassifier.OPTIONAL)
        ));

        createPaymentCustomType(PAYMENT_WALLET, ImmutableList.of(
                createSingleLineStringFieldDefinition(CustomFieldKeys.LANGUAGE_CODE_FIELD, FieldClassifier.REQUIRED),
                createSingleLineStringFieldDefinition(CustomFieldKeys.REFERENCE_FIELD, FieldClassifier.REQUIRED),
                createMultiLineStringFieldDefinition(CustomFieldKeys.REFERENCE_TEXT_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.REDIRECT_URL_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.SUCCESS_URL_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.ERROR_URL_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.CANCEL_URL_FIELD, FieldClassifier.OPTIONAL)
        ));

        createPaymentCustomType(PAYMENT_BANK_TRANSFER, ImmutableList.of(
                createSingleLineStringFieldDefinition(CustomFieldKeys.LANGUAGE_CODE_FIELD, FieldClassifier.REQUIRED),
                createSingleLineStringFieldDefinition(CustomFieldKeys.REFERENCE_FIELD, FieldClassifier.REQUIRED),
                createMultiLineStringFieldDefinition(CustomFieldKeys.REFERENCE_TEXT_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.REDIRECT_URL_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.SUCCESS_URL_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.ERROR_URL_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.CANCEL_URL_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.IBAN_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.BIC_FIELD, FieldClassifier.OPTIONAL)
        ));

        createPaymentCustomType(PAYMENT_CASH_ADVANCE, ImmutableList.of(
                createSingleLineStringFieldDefinition(CustomFieldKeys.LANGUAGE_CODE_FIELD, FieldClassifier.REQUIRED),
                createSingleLineStringFieldDefinition(CustomFieldKeys.REFERENCE_FIELD, FieldClassifier.REQUIRED),
                createSingleLineStringFieldDefinition(CustomFieldKeys.PAID_FROM_NAME_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.PAID_FROM_IBAN_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.PAID_FROM_BIC_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.REFUND_TO_NAME_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.REFUND_TO_IBAN_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.REFUND_TO_BIC_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.PAY_TO_NAME_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.PAY_TO_IBAN_FIELD, FieldClassifier.OPTIONAL),
                createSingleLineStringFieldDefinition(CustomFieldKeys.PAY_TO_BIC_FIELD, FieldClassifier.OPTIONAL)
        ));

        createPaymentCustomType(PAYMENT_INVOICE_KLARNA, ImmutableList.of(
                // TODO: cleanup this comment when pretty tested, good specified and set-up on all depended projects
                createSingleLineStringFieldDefinition(CustomFieldKeys.LANGUAGE_CODE_FIELD, FieldClassifier.REQUIRED),
                createSingleLineStringFieldDefinition(CustomFieldKeys.REFERENCE_FIELD, FieldClassifier.REQUIRED),
                createSingleLineStringFieldDefinition(CustomFieldKeys.GENDER_FIELD, FieldClassifier.REQUIRED),
                createSingleLineStringFieldDefinition(CustomFieldKeys.IP_FIELD, FieldClassifier.REQUIRED),
                createDateFieldDefinition(CustomFieldKeys.BIRTHDAY_FIELD, FieldClassifier.REQUIRED),
                createSingleLineStringFieldDefinition(CustomFieldKeys.TELEPHONENUMBER_FIELD, FieldClassifier.REQUIRED)
        ));
    }

    private void createPayoneSpecificTypes() {
        final FieldDefinition timestampField = createSingleLineStringFieldDefinition(CustomFieldKeys.TIMESTAMP_FIELD, FieldClassifier.REQUIRED);
        final FieldDefinition transactionIdField = createSingleLineStringFieldDefinition(CustomFieldKeys.TRANSACTION_ID_FIELD, FieldClassifier.REQUIRED);

        createInteractionRequest(timestampField, transactionIdField);
        createInteractionResponse(timestampField, transactionIdField);
        createInteractionRedirect(timestampField, transactionIdField);
        createInteractionNotification(timestampField);
        createInteractionTemporaryError(timestampField, transactionIdField);
        createPayoneUnsupportedTransaction(timestampField, transactionIdField);
    }

    private Type createInteractionRequest(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        return createInterfaceInteractionType(PAYONE_INTERACTION_REQUEST, ImmutableList.of(timestampField, transactionIdField, createMultiLineStringFieldDefinition(CustomFieldKeys.REQUEST_FIELD,  FieldClassifier.REQUIRED)));
    }

    private Type createInteractionResponse(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        return createInterfaceInteractionType(PAYONE_INTERACTION_RESPONSE, ImmutableList.of(timestampField, transactionIdField, createMultiLineStringFieldDefinition(CustomFieldKeys.RESPONSE_FIELD, FieldClassifier.REQUIRED)));
    }

    private Type createInteractionRedirect(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        return createInterfaceInteractionType(PAYONE_INTERACTION_REDIRECT,
                ImmutableList.of(timestampField, transactionIdField, createSingleLineStringFieldDefinition(CustomFieldKeys.REDIRECT_URL_FIELD, FieldClassifier.REQUIRED), createMultiLineStringFieldDefinition(CustomFieldKeys.RESPONSE_FIELD, FieldClassifier.REQUIRED)));
    }

    private Type createInteractionTemporaryError(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        return createInterfaceInteractionType(PAYONE_INTERACTION_TEMPORARY_ERROR, ImmutableList.of(timestampField, transactionIdField, createMultiLineStringFieldDefinition(CustomFieldKeys.RESPONSE_FIELD, FieldClassifier.REQUIRED)));
    }

    private Type createInteractionNotification(final FieldDefinition timestampField) {
        final FieldDefinition sequenceNumberField = createSingleLineStringFieldDefinition(CustomFieldKeys.SEQUENCE_NUMBER_FIELD, FieldClassifier.REQUIRED);
        final FieldDefinition txActionField = createSingleLineStringFieldDefinition(CustomFieldKeys.TX_ACTION_FIELD, FieldClassifier.REQUIRED);
        final FieldDefinition notificationField = createMultiLineStringFieldDefinition(CustomFieldKeys.NOTIFICATION_FIELD, FieldClassifier.REQUIRED);

        return createInterfaceInteractionType(PAYONE_INTERACTION_NOTIFICATION, ImmutableList.of(timestampField, sequenceNumberField, txActionField, notificationField));
    }

    private Type createPayoneUnsupportedTransaction(final FieldDefinition timestampField, final FieldDefinition transactionIdField) {
        return createInterfaceInteractionType(PAYONE_UNSUPPORTED_TRANSACTION, ImmutableList.of(timestampField, transactionIdField, createSingleLineStringFieldDefinition(CustomFieldKeys.MESSAGE_FIELD, FieldClassifier.REQUIRED)));
    }

    private Type createInterfaceInteractionType(final String typeKey, final ImmutableList<FieldDefinition> fieldDefinitions) {
        return createType(typeKey, fieldDefinitions, AddInterfaceInteraction.resourceTypeId());
    }

    private Type createPaymentCustomType(final String typeKey, final ImmutableList<FieldDefinition> fieldDefinitions) {
        return createType(typeKey, fieldDefinitions, Payment.resourceTypeId());
    }

    private Type createType(final String typeKey, final ImmutableList<FieldDefinition> fieldDefinitions, final String resourceTypeId) {
        // TODO replace with cache
        final PagedQueryResult<Type> result = ctpClient.executeBlocking(
                TypeQuery.of()
                        .withPredicates(m -> m.key().is(typeKey))
                        .withLimit(1));

        if (result.getTotal() == 0) {
            return ctpClient.executeBlocking(TypeCreateCommand.of(TypeDraftBuilder.of(
                    typeKey,
                    LocalizedString.ofEnglish(typeKey),
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
        return createFieldDefinition(StringFieldType.of(), fieldName, TextInputHint.SINGLE_LINE, classifier);
    }

    private FieldDefinition createMultiLineStringFieldDefinition(final String fieldName, final FieldClassifier classifier) {
        return createFieldDefinition(StringFieldType.of(), fieldName, TextInputHint.MULTI_LINE, classifier);
    }

    private FieldDefinition createDateFieldDefinition(final String fieldName, final FieldClassifier classifier) {
        return createFieldDefinition(DateFieldType.of(), fieldName, null, classifier);
    }

    private FieldDefinition createFieldDefinition(@Nonnull final FieldType fieldType,
                                                  @Nonnull final String fieldName,
                                                  @Nullable final TextInputHint inputHint,
                                                  @Nonnull final FieldClassifier classifier) {
        return FieldDefinition.of(
                    fieldType,
                    fieldName,
                    LocalizedString.ofEnglish(fieldName),
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
