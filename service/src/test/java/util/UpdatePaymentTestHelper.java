package util;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.updateactions.*;

import javax.money.MonetaryAmount;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author mht@dotsource.de
 */
public class UpdatePaymentTestHelper {
    public static AddInterfaceInteraction getAddInterfaceInteraction(final Notification notification, final ZonedDateTime time) {
        final HashMap<String, Object> fieldsMap = new HashMap<>();
        fieldsMap.put(CustomFieldKeys.TIMESTAMP_FIELD, time);
        fieldsMap.put(CustomFieldKeys.SEQUENCE_NUMBER_FIELD, notification.getSequencenumber());
        fieldsMap.put(CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode());
        fieldsMap.put(CustomFieldKeys.NOTIFICATION_FIELD, notification.toString());
        return AddInterfaceInteraction.ofTypeKeyAndObjects(
                CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                fieldsMap);
    }

    public static SetStatusInterfaceCode getSetStatusInterfaceCode(final Notification notification){
        return SetStatusInterfaceCode.of(notification.getTxaction().toString());
    }

    public static SetStatusInterfaceText getSetStatusInterfaceText(final Notification notification){
        return SetStatusInterfaceText.of(notification.getTxaction().toString());
    }

    public static AddTransaction getAddTransaction(final TransactionType transactionType,
                                                   final TransactionState transactionState,
                                                   final MonetaryAmount amount,
                                                   final ZonedDateTime timestamp,
                                                   final String interactionId) {
        return AddTransaction.of(
                TransactionDraftBuilder.of(transactionType, amount, timestamp)
                        .state(transactionState)
                        .interactionId(interactionId)
                        .build());
    }

    public static ChangeTransactionInteractionId getChangeTransactionInteractionId(final String interactionId,
                                                                                   final String transactionId) {
        return ChangeTransactionInteractionId.of(interactionId, transactionId);
    }

    public static ChangeTransactionState getChangeTransactionState(final TransactionState state, final String id) {
        return ChangeTransactionState.of(state, id);
    }


    @SuppressWarnings("unchecked")
    public static void assertStandardUpdateActions(final List<? extends UpdateAction<Payment>> updateActions,
                                                   final AddInterfaceInteraction interfaceInteraction,
                                                   final SetStatusInterfaceCode statusInterfaceCode,
                                                   final SetStatusInterfaceText statusInterfaceText) {
        assertThat(updateActions).as("added interaction")
                .filteredOn(u -> u.getAction().equals("addInterfaceInteraction"))
                .usingElementComparatorOnFields("fields")
                .containsOnly(interfaceInteraction);

        assertThat(updateActions)
                .filteredOn(u -> u.getAction().equals("setStatusInterfaceCode"))
                .containsOnly(statusInterfaceCode);

        assertThat(updateActions)
                .filteredOn(u -> u.getAction().equals("setStatusInterfaceText"))
                .containsOnly(statusInterfaceText);
    }

}
