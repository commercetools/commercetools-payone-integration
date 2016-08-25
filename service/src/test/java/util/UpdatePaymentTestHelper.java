package util;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceCode;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceText;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author mht@dotsource.de
 */
public class UpdatePaymentTestHelper {
    public static AddInterfaceInteraction getAddInterfaceInteraction(final Notification notification, final ZonedDateTime time) {
        return AddInterfaceInteraction.ofTypeKeyAndObjects(
                CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                ImmutableMap.of(
                        CustomFieldKeys.TIMESTAMP_FIELD, time,
                        CustomFieldKeys.SEQUENCE_NUMBER_FIELD, notification.getSequencenumber(),
                        CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                        CustomFieldKeys.NOTIFICATION_FIELD, notification.toString()));
    }

    public static SetStatusInterfaceCode getSetStatusInterfaceCode(final Notification notification){
        return SetStatusInterfaceCode.of(notification.getTxaction().toString());
    }

    public static SetStatusInterfaceText getSetStatusInterfaceText(final Notification notification){
        return SetStatusInterfaceText.of(notification.getTxaction().toString());
    }

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
