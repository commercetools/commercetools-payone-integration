package com.commercetools.pspadapter.payone.notification.common;

import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import com.commercetools.pspadapter.payone.notification.BaseNotificationProcessorTest;
import com.commercetools.pspadapter.payone.notification.NotificationProcessorBase;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceCode;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceText;
import io.sphere.sdk.utils.MoneyImpl;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static util.UpdatePaymentTestHelper.*;

/**
 * Basic properties and methods for chargeable notifications tests (capture, paid etc)
 */
public class BaseChargedNotificationProcessorTest extends BaseNotificationProcessorTest {

    protected static final Integer millis = 1450365542;

    protected static final ZonedDateTime timestamp =
            ZonedDateTime.of(LocalDateTime.ofEpochSecond(millis, 0, ZoneOffset.UTC), ZoneId.of("UTC"));

    protected void processingPendingNotificationAboutUnknownTransactionAddsChargeTransactionWithStatePending(NotificationProcessorBase testee)
            throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentOneAuthPending20EuroCC();
        payment.getTransactions().clear();

        notification.setTransactionStatus(TransactionStatus.PENDING);

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());
        final AddTransaction transaction = AddTransaction.of(TransactionDraftBuilder
                .of(TransactionType.CHARGE, amount, timestamp)
                .state(TransactionState.PENDING)
                .interactionId(notification.getSequencenumber())
                .build());

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, timestamp);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions).as("# of payment update actions").hasSize(4);
        assertThat(updateActions).as("added transaction")
                .filteredOn(u -> u.getAction().equals("addTransaction"))
                .usingElementComparatorOnFields(
                        "transaction.type", "transaction.amount", "transaction.state", "transaction.timestamp")
                .containsOnlyOnce(transaction);

        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);
    }

    protected void processingPendingNotificationForPendingChargeTransactionDoesNotChangeState(NotificationProcessorBase testee)
            throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentOneChargePending20Euro();

        notification.setSequencenumber(payment.getTransactions().get(0).getInteractionId());
        notification.setTransactionStatus(TransactionStatus.PENDING);

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, timestamp);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);

        assertThat(updateActions).as("# of payment update actions").hasSize(3);
        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);
    }
}
