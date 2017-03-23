package specs.service;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.commercetools.service.OrderService;
import com.commercetools.service.OrderServiceImpl;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.SetCustomField;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.apache.http.HttpResponse;
import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import specs.BaseFixture;

import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;
import javax.money.NumberValue;
import javax.money.format.MonetaryFormats;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.commercetools.pspadapter.payone.util.CompletionUtil.executeBlocking;
import static java.lang.String.format;

@RunWith(ConcordionRunner.class)
public class OrderServiceImplFixture extends BaseFixture {

    private OrderService orderService;

    @Before
    public void setUp() throws Exception {
        super.initializeCommercetoolsClient();
        orderService = new OrderServiceImpl(this.ctpClient());
    }

    public MultiValueResult createPayment(
            final String paymentName,
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode) throws ExecutionException, InterruptedException {

        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);
        final String pseudocardpan = getUnconfirmedVisaPseudoCardPan();

        final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                        .method(paymentMethod)
                        .paymentInterface("PAYONE")
                        .build())
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYMENT_CREDIT_CARD,
                        ImmutableMap.of(
                                CustomFieldKeys.CARD_DATA_PLACEHOLDER_FIELD, pseudocardpan,
                                CustomFieldKeys.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage(),
                                CustomFieldKeys.REFERENCE_FIELD, "<placeholder>")))
                .build();

        final BlockingSphereClient ctpClient = ctpClient();
        final Payment payment = ctpClient.executeBlocking(PaymentCreateCommand.of(paymentDraft));
        registerPaymentWithLegibleName(paymentName, payment);

        final Order order = createAndGetOrder(payment, currencyCode);

        ctpClient.executeBlocking(PaymentUpdateCommand.of(
                payment,
                ImmutableList.<UpdateActionImpl<Payment>>builder()
                        .add(AddTransaction.of(TransactionDraftBuilder.of(
                                TransactionType.valueOf(transactionType),
                                monetaryAmount,
                                ZonedDateTime.now())
                                .state(TransactionState.PENDING)
                                .build()))
                        .add(SetCustomField.ofObject(CustomFieldKeys.REFERENCE_FIELD, order.getOrderNumber()))
                        .build()));

        return MultiValueResult.multiValueResult()
                .with("paymentId", payment.getId())
                .with("orderId", order.getId())
                .with("orderNumber", order.getOrderNumber());
    }

    public MultiValueResult handlePaymentAndOrder(final String paymentName,
                                                  final String requestType) throws Exception {
        final HttpResponse response = requestToHandlePaymentByLegibleName(paymentName);
        final Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);

        final String amountAuthorized = (payment.getAmountAuthorized() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountAuthorized()) :
                BaseFixture.EMPTY_STRING;

        Optional<Order> order = executeBlocking(orderService.getOrderByPaymentId(payment.getId()));

        return MultiValueResult.multiValueResult()
                .with("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .with("transactionState", getTransactionState(payment, transactionId))
                .with("amountAuthorized", amountAuthorized)
                .with("centAmount", order.map(Order::getTotalPrice).map(MonetaryAmount::getNumber).map(NumberValue::intValue).orElse(-666))
                .with("currencyCode", order.map(Order::getTotalPrice).map(MonetaryAmount::getCurrency).map(CurrencyUnit::getCurrencyCode).orElse("ERR"))
                .with("paymentState", order.map(Order::getPaymentState).map(PaymentState::toString).orElse(null));
    }

    public MultiValueResult updateOrderPaymentState(final String paymentName, final String paymentStateName) {
        final Payment payment = fetchPaymentByLegibleName(paymentName);

        Order updatedOrder = executeBlocking(
                orderService.getOrderByPaymentId(payment.getId())
                            .thenComposeAsync(order ->
                                   orderService.updateOrderPaymentState(
                                           order.orElseThrow(() -> new RuntimeException(format("Order nof found for payment [%s]", payment.getId()))),
                                           paymentStateName != null ? PaymentState.valueOf(paymentStateName) : null
                                   )));

        return MultiValueResult.multiValueResult()
                .with("paymentState", updatedOrder.getPaymentState())
                .with("orderNumber", updatedOrder.getOrderNumber());
    }

    public MultiValueResult updateOrderPaymentState_forNull(final String paymentName) {
        try {
            updateOrderPaymentState(paymentName, null);
        } catch (Throwable e) {
            return MultiValueResult.multiValueResult()
                    .with("exceptionClass", e.getClass().getName())
                    .with("exceptionCauseClass", e.getCause().getClass().getName());
        }

        return MultiValueResult.multiValueResult()
                .with("exceptionClass", "<missed>")
                .with("exceptionCauseClass", "<missed>");
    }


}