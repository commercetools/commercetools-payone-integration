package com.commercetools.pspadapter.payone.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.TransactionDraft;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionInteractionId;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionState;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.Before;
import org.junit.Test;
import util.PaymentTestHelper;
import util.SphereClientDoubleCreator;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.util.List;

/**
 * @author fhaertig
 * @date 11.01.16
 */
public class AppointedNotificationProcessorTest {

    private CommercetoolsClient client;

    private final PaymentTestHelper testHelper = new PaymentTestHelper();


    @Before
    public void setUp() throws Exception {
        client = new CommercetoolsClient(SphereClientDoubleCreator
                .getSphereClientWithResponseFunction((intent) -> {
                    try {
                        if (intent.getPath().startsWith("/payments")) {
                            return testHelper.getPaymentQueryResultFromFile("dummyPaymentQueryResult.json");
                        }
                    } catch (final Exception e) {
                        throw new RuntimeException(String.format("Failed to load resource: %s", intent), e);
                    }
                    throw new UnsupportedOperationException(
                            String.format("I'm not prepared for this request: %s", intent));
                }));
    }

    @Test
    public void getUpdatesForNewTransaction() throws IOException {
        final Notification notification = new Notification();
        notification.setPrice("20.00");
        notification.setCurrency("EUR");
        notification.setTxaction(NotificationAction.APPOINTED);
        notification.setTransactionStatus(TransactionStatus.PENDING);

        Payment payment = testHelper.getPaymentQueryResultFromFile("dummyPaymentQueryResult.json").head().get();
        payment.getTransactions().clear();

        AppointedNotificationProcessor processor = new AppointedNotificationProcessor(client);

        List<UpdateAction<Payment>> updateActions = processor.getTransactionUpdates(payment, notification);

        MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());
        TransactionDraft comparable = TransactionDraftBuilder
                        .of(TransactionType.AUTHORIZATION, amount, null)
                        .state(TransactionState.PENDING)
                        .build();


        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isInstanceOf(AddTransaction.class);
        assertThat(((AddTransaction)updateActions.get(0)).getTransaction()).isEqualToComparingOnlyGivenFields(
                comparable, "type", "amount", "state");
    }

    @Test
    public void getUpdatesForExistingTransaction() throws IOException {
        final Notification notification = new Notification();
        notification.setTxaction(NotificationAction.APPOINTED);
        notification.setTransactionStatus(TransactionStatus.PENDING);

        Payment payment = testHelper.getPaymentQueryResultFromFile("dummyPaymentQueryResult.json").head().get();

        AppointedNotificationProcessor processor = new AppointedNotificationProcessor(client);

        List<UpdateAction<Payment>> updateActions = processor.getTransactionUpdates(payment, notification);

        assertThat(updateActions).contains(ChangeTransactionState.of(TransactionState.PENDING, payment.getTransactions().get(0).getId()));
        assertThat(updateActions).contains(ChangeTransactionInteractionId.of(notification.getSequencenumber(), payment.getTransactions().get(0).getId()));
    }
}