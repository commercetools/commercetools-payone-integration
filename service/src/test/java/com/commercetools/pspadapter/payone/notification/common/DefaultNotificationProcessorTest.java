package com.commercetools.pspadapter.payone.notification.common;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import com.commercetools.pspadapter.payone.notification.BaseNotificationProcessorTest;
import com.google.common.collect.Lists;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceCode;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceText;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.sphere.sdk.orders.PaymentState.CREDIT_OWED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static util.UpdatePaymentTestHelper.*;

/**
 * @author Jan Wolter
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultNotificationProcessorTest extends BaseNotificationProcessorTest {

    private static final Integer seconds = 1450365542;
    private static final ZonedDateTime TXTIME_ZONED_DATE_TIME =
            ZonedDateTime.of(LocalDateTime.ofEpochSecond(seconds, 0, ZoneOffset.UTC), ZoneId.of("UTC"));

    @InjectMocks
    private DefaultNotificationProcessor testee;


    @Before
    public void setUp() throws Exception {
        super.setUp();

        notification = new Notification();
        notification.setPrice("20.00");
        notification.setBalance("0.00");
        notification.setReceivable("0.00");
        notification.setCurrency("EUR");
        notification.setTxtime(seconds.toString());
        notification.setSequencenumber("12");
        notification.setTxaction(randomTxAction());
        notification.setTransactionStatus(TransactionStatus.COMPLETED);

        // just test any value
        when(paymentToOrderStateMapper.mapPaymentToOrderState(any(Payment.class)))
                .thenReturn(CREDIT_OWED);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void storesNotificationAsInterfaceInteractionOfTypePayoneInteractionNotification() throws Exception {
        // arrange
        final Payment payment = testHelper.dummyPaymentTwoTransactionsSuccessPending();

        // act
        testee.processTransactionStatusNotification(notification, payment);

        // assert
        final List<? extends UpdateAction<Payment>> updateActions = updatePaymentAndGetUpdateActions(payment);

        final AddInterfaceInteraction interfaceInteraction = getAddInterfaceInteraction(notification, TXTIME_ZONED_DATE_TIME);
        final SetStatusInterfaceCode statusInterfaceCode = getSetStatusInterfaceCode(notification);
        final SetStatusInterfaceText statusInterfaceText = getSetStatusInterfaceText(notification);


        assertStandardUpdateActions(updateActions, interfaceInteraction, statusInterfaceCode, statusInterfaceText);
        assertThat(updateActions).as("# of update actions").hasSize(3);

        // test value from #setUp() is injected
        verifyUpdateOrderActions(payment, CREDIT_OWED);
    }

    private static NotificationAction randomTxAction() {
        final ArrayList<NotificationAction> txActions = Lists.newArrayList(NotificationAction.values());
        Collections.shuffle(txActions);
        return txActions.get(0);
    }
}
