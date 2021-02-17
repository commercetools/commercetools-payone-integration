package com.commercetools.pspadapter.payone.transaction;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseErrorCode;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ResponseStatus;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionState;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionTimestamp;
import io.sphere.sdk.payments.commands.updateactions.SetCustomField;
import io.sphere.sdk.payments.commands.updateactions.SetInterfaceId;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceCode;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceText;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields.CUSTOMER_MESSAGE;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields.ERROR_CODE;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields.ERROR_MESSAGE;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields.STATUS;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields.TXID;
import static com.commercetools.pspadapter.payone.util.PaymentUtil.getTransactionById;

/**
 * @author mht@dotsource.de
 * Common base class responsible for default paymentupdateactions
 */
public abstract class TransactionBaseExecutor extends IdempotentTransactionExecutor {

    public static final String ERROR = "ERROR";

    protected final BlockingSphereClient client;

    public TransactionBaseExecutor(@Nonnull final LoadingCache<String, Type> typeCache,
                                   @Nonnull final BlockingSphereClient client) {
        super(typeCache);
        this.client = client;
    }

    @Override
    protected PaymentWithCartLike executeIdempotent(PaymentWithCartLike paymentWithCartLike, Transaction transaction) {
        return execute(paymentWithCartLike, transaction);
    }

    @Nonnull
    abstract protected PaymentWithCartLike execute(final PaymentWithCartLike paymentWithCartLike,
                                                   final Transaction transaction);

    /**
     * Creates the SetStatusInterfaceCode from the response
     * @param response contains all key that creates the
     * @return the UpdateAction that
     */
    protected SetStatusInterfaceCode setStatusInterfaceCode(final Map<String, String> response){
        final String status = response.get(STATUS);
        final StringBuilder stringBuilder = new StringBuilder(status);
        if(ERROR.equals(status)) {
            //Payone errorcode is required for error case
            stringBuilder.append(" ");
            stringBuilder.append(response.get(ERROR_CODE));
        }
        return SetStatusInterfaceCode.of(stringBuilder.toString());
    }

    /**
     * Creates the SetStatusInterfaceText from the response ErrorMessage in case of Error
     * @param response contains all key that creates the
     * @return the UpdateAction that
     */
    protected SetStatusInterfaceText setStatusInterfaceText(final Map<String, String> response){
        final String status = response.get(STATUS);
        final StringBuilder stringBuilder = new StringBuilder("");
        if(ERROR.equals(status)) {
            stringBuilder.append(response.get(ERROR_MESSAGE));
        }
        else {
            stringBuilder.append(status);
        }
        return SetStatusInterfaceText.of(stringBuilder.toString());
    }

    /**
     * Converts {@code Map<String, String>} response to a string with a JSON value.
     * @param response Response map to convert to a JSON String
     * @return JSON string with respective {@code response} key-value entries.
     * @see #exceptionToResponseJsonString(Exception)
     */
    protected static String responseToJsonString(Map<String, String> response) {
        return SphereJsonUtils.toJsonString(response);
    }

    /**
     * In case of communication errors with Payone payment URL/Server (timeout, authorisation, wrong server URL ...),
     * we simulate same response structure, as described in
     * <a href="https://sphere.atlassian.net/wiki/display/CDL/Payone?preview=%2F87326868%2F106102867%2FPAYONE_Platform_Server_API_EN_v2.77.pdf">
     *     TECHNICAL REFERENCE PAYONE Platform Channel Server API, Version 2.77</a>:<ul>
     *     <li><b>status</b>: ERROR</li>
     *     <li><b>errorcode</b>: {@link ResponseErrorCode#TRANSACTION_EXCEPTION}</li>
     *     <li><b>errormessage</b>: {@code exception.getMessage()} value</li>
     *     <li><b>customermessage</b>: Neutral English message (without technical info)
     *     to display for a customer on a page.</li>
     * </ul>
     * @param exception exception which occurred when transaction executed.
     * @return JSON string with key-value entries following Payone API.
     * @see #responseToJsonString(Map)
     */
    protected static String exceptionToResponseJsonString(@Nonnull Exception exception) {
        return responseToJsonString(ImmutableMap.of(
                STATUS, ResponseStatus.ERROR.getStateCode(),
                ERROR_CODE, ResponseErrorCode.TRANSACTION_EXCEPTION.getErrorCode(),
                ERROR_MESSAGE, "Integration Service Exception: " + exception.getMessage(),
                CUSTOMER_MESSAGE, "Error on payment transaction processing."));
    }

    /**
     * Additionally to {@link #getDefaultSuccessUpdateActions(TransactionState, Payment, String, Map, AddInterfaceInteraction)}
     * updates redirect specific custom fields.
     */
    protected List<UpdateActionImpl<Payment>> getRedirectUpdateActions(@Nonnull TransactionState newState,
                                                                       @Nonnull Payment updatedPayment,
                                                                       @Nonnull String transactionId,
                                                                       @Nonnull Map<String, String> response,
                                                                       @Nonnull AddInterfaceInteraction interfaceInteraction) {

        List<UpdateActionImpl<Payment>> updateActions =
                getDefaultSuccessUpdateActions(newState, updatedPayment, transactionId, response, interfaceInteraction);

        updateActions.add(SetCustomField.ofObject(CustomFieldKeys.REDIRECT_URL_FIELD, response.get(PayoneResponseFields.REDIRECT_URL)));

        return updateActions;
    }

    /**
     * Additionally to {@link #getDefaultUpdateActions(TransactionState, Payment, String, Map, AddInterfaceInteraction)}
     * adds payment interface id from the {@code response} ({@link PayoneResponseFields#TXID} field).
     * <p>
     * This update actions list is used for all success payment handling (including redirect, approved and pending)
     */
    protected List<UpdateActionImpl<Payment>> getDefaultSuccessUpdateActions(@Nonnull TransactionState newState,
                                                                             @Nonnull Payment updatedPayment,
                                                                             @Nonnull String transactionId,
                                                                             @Nonnull Map<String, String> response,
                                                                             @Nonnull AddInterfaceInteraction interfaceInteraction) {
        List<UpdateActionImpl<Payment>> updateActions = getDefaultUpdateActions(newState, updatedPayment, transactionId, response, interfaceInteraction);
        updateActions.add(SetInterfaceId.of(response.get(TXID)));

        return updateActions;
    }

    /**
     * Default payment update actions list, which is saved on any Payone response. The list is mutable and includes:<ul>
     * <li>{@code interfaceInteraction}</li>
     * <li>Payone status code and text from the {@code response}</li>
     * <li>change transaction timestamp to current time</li>
     * <li>(optionally) change transaction state if current transaction state is not equal to {@code newState}.
     * <br/>
     * <b>Note:</b> CTP platform throws exception if change transaction state action is called with the same value
     * the transaction has right now.</li>
     * </ul>
     */
    protected List<UpdateActionImpl<Payment>> getDefaultUpdateActions(@Nonnull TransactionState newState,
                                                                      @Nonnull Payment updatedPayment,
                                                                      @Nonnull String transactionId,
                                                                      @Nonnull Map<String, String> response,
                                                                      @Nonnull AddInterfaceInteraction interfaceInteraction) {
        ArrayList<UpdateActionImpl<Payment>> updateActions = Lists.newArrayList(
                interfaceInteraction,
                setStatusInterfaceCode(response),
                setStatusInterfaceText(response),
                ChangeTransactionTimestamp.of(ZonedDateTime.now(), transactionId));

        // avoid CTP exception if the transaction already has newState
        getTransactionById(updatedPayment, transactionId)
                .filter(tr -> tr.getState() != newState)
                .map(tr -> ChangeTransactionState.of(newState, transactionId))
                .ifPresent(updateActions::add);

        return updateActions;
    }

    protected PaymentWithCartLike update(PaymentWithCartLike paymentWithCartLike, Payment payment, List<? extends UpdateAction<Payment>> updateActions) {
        return paymentWithCartLike.withPayment(
                client.executeBlocking(PaymentUpdateCommand.of(payment, updateActions)));
    }
}
