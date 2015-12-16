package specs.paymentmethods.creditcard;

import com.commercetools.pspadapter.payone.IntegrationService;
import com.commercetools.pspadapter.payone.ServiceFactory;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.CartDraft;
import io.sphere.sdk.carts.CartDraftBuilder;
import io.sphere.sdk.carts.commands.CartCreateCommand;
import io.sphere.sdk.carts.commands.CartDeleteCommand;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.AddLineItem;
import io.sphere.sdk.carts.commands.updateactions.AddPayment;
import io.sphere.sdk.carts.commands.updateactions.SetBillingAddress;
import io.sphere.sdk.carts.commands.updateactions.SetShippingAddress;
import io.sphere.sdk.carts.queries.CartQuery;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.OrderFromCartDraft;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.orders.commands.OrderDeleteCommand;
import io.sphere.sdk.orders.commands.OrderFromCartCreateCommand;
import io.sphere.sdk.orders.commands.OrderUpdateCommand;
import io.sphere.sdk.orders.queries.OrderQuery;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentDraft;
import io.sphere.sdk.payments.PaymentDraftBuilder;
import io.sphere.sdk.payments.PaymentMethodInfoBuilder;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionDraft;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentDeleteCommand;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.queries.PaymentByIdGet;
import io.sphere.sdk.payments.queries.PaymentQuery;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import io.sphere.sdk.types.queries.TypeQuery;
import io.sphere.sdk.utils.MoneyImpl;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.concordion.api.ExpectedToFail;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import specs.BaseFixture;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * @author fhaertig
 * @date 10.12.15
 */
@ExpectedToFail
@RunWith(ConcordionRunner.class)
public class ChargePreauthorizedFixture extends BaseFixture {

    private BlockingClient ctpClient;
    private IntegrationService integrationService;
    private ServiceConfig serviceConfig;


    @Before
    public void initializeService() throws MalformedURLException {

        serviceConfig = new ServiceConfig(new PropertyProvider());

        //only for creation of test data
        ctpClient = new CommercetoolsClient(SphereClientFactory.of().createClient(
                serviceConfig.getCtProjectKey(),
                serviceConfig.getCtClientId(),
                serviceConfig.getCtClientSecret()));

        resetCommercetoolsPlatform();

        integrationService = ServiceFactory.createService(serviceConfig);
        integrationService.start();
    }

    @After
    public void tearDown() {
        integrationService.stop();
        resetCommercetoolsPlatform();
    }

    public String createPreAuthorizedPayment(
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode) throws ExecutionException, InterruptedException, PayoneException, IOException {

        final MonetaryAmount monetaryAmount = MoneyImpl.ofCents(Long.valueOf(centAmount), currencyCode);
        Payment payment = preparePaymentWithPreauthorizedAmountAndOrder(monetaryAmount, paymentMethod);

        //get newest payment and add new charge transaction
        payment = ctpClient.complete(PaymentByIdGet.of(payment.getId()));
        payment = ctpClient.complete(PaymentUpdateCommand.of(payment,
                    ImmutableList.of(
                            AddTransaction.of(TransactionDraftBuilder
                                    .of(TransactionType.valueOf(transactionType), monetaryAmount, ZonedDateTime.now())
                                    .state(TransactionState.PENDING)
                                    .build())
                    )
                )
        );

        return payment.getId();
    }

    public boolean handlePayment(final String paymentId) throws IOException, ExecutionException, InterruptedException {

        final HttpResponse response = Request.Get(getHandlePaymentUrl(paymentId))
                .connectTimeout(200)
                .execute()
                .returnResponse();

        //currently should return 400 because ChargeExecutor is not implemented yet!
        return response.getStatusLine().getStatusCode() == 400;
    }

    public String getIdOfLastTransaction(final String paymentId) {
        return Iterables.getLast(fetchPayment(paymentId).getTransactions()).getId();
    }

    public String getInterfaceInteractionCount(
            final String paymentId,
            final String transactionId,
            final String interactionType,
            final String requestType) {
        final Payment payment = fetchPayment(paymentId);
        return Long.toString(payment.getInterfaceInteractions().stream()
                .filter(i -> i.getType().getTypeId().equals(interactionType))
                .filter(i -> transactionId.equals(i.getFieldAsString(CustomTypeBuilder.TRANSACTION_ID_FIELD)))
                .filter(i -> {
                    final String requestField = i.getFieldAsString(CustomTypeBuilder.REQUEST_FIELD);
                    return requestField != null && requestField.contains("request=" + requestType);
                })
                .count());
    }

    public String getTransactionState(final String paymentId, final String transactionId) {
        final Optional<Transaction> transaction = fetchPayment(paymentId).getTransactions()
                .stream()
                .filter(i -> transactionId.equals(i.getId()))
                .findFirst();

        final TransactionState transactionState = transaction.isPresent()? transaction.get().getState() : null;
        return transactionState != null? transactionState.toSphereName() : "<UNKNOWN>";
    }

    private void resetCommercetoolsPlatform() {

        // TODO jw: use futures
        // delete all carts
        ctpClient.complete(CartQuery.of().withLimit(500)).getResults()
                .forEach(c -> ctpClient.complete(CartDeleteCommand.of(c)));

        //delete all orders
        ctpClient.complete(OrderQuery.of().withLimit(500)).getResults()
                .forEach(c -> ctpClient.complete(OrderDeleteCommand.of(c)));

        // delete all payments
        ctpClient.complete(PaymentQuery.of().withLimit(500)).getResults()
                .forEach(p -> ctpClient.complete(PaymentDeleteCommand.of(p)));

        ctpClient.complete(TypeQuery.of().withLimit(500)).getResults()
                .forEach(t -> ctpClient.complete(TypeDeleteCommand.of(t)));
    }

    private Payment preparePaymentWithPreauthorizedAmountAndOrder(final MonetaryAmount monetaryAmount, final String paymentMethod) throws ExecutionException, InterruptedException, IOException {
        final List<TransactionDraft> transactions = Collections.singletonList(TransactionDraftBuilder
                .of(TransactionType.AUTHORIZATION, monetaryAmount, ZonedDateTime.now())
                .state(TransactionState.PENDING)
                .build());

        final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                        .method(paymentMethod)
                        .paymentInterface("PAYONE")
                        .build())
                .transactions(transactions)
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYMENT_CREDIT_CARD,
                        ImmutableMap.of(
                                CustomTypeBuilder.CARD_DATA_PLACEHOLDER_FIELD, getUnconfirmedVisaPseudoCardPan(),
                                CustomTypeBuilder.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage(),
                                CustomTypeBuilder.REFERENCE_FIELD, "myGlobalKey")))
                .build();

        Payment payment = ctpClient.complete(PaymentCreateCommand.of(paymentDraft));

        final Address billingAddress = Address.of(CountryCode.DE).withLastName("Test Buyer");
        //create cart and order with product
        Product product = ctpClient.complete(ProductQuery.of()).getResults().get(0);
        CartDraft cardDraft = CartDraftBuilder.of(Monetary.getCurrency("EUR"))
                .build();
        Cart cart = ctpClient.execute(CartCreateCommand.of(cardDraft)).toCompletableFuture().get();
        List<UpdateAction<Cart>> updateActions = Arrays.asList(
                AddPayment.of(payment),
                AddLineItem.of(product.getId(), product.getMasterData().getCurrent().getMasterVariant().getId(), 1),
                SetShippingAddress.of(Address.of(CountryCode.DE)),
                SetBillingAddress.of(billingAddress)
        );
        cart = ctpClient.complete(CartUpdateCommand.of(cart, updateActions));
        final Order order = ctpClient.complete(OrderFromCartCreateCommand.of(OrderFromCartDraft.of(cart, getRandomOrderNumber(), PaymentState.PENDING)));
        ctpClient.complete(OrderUpdateCommand.of(order,
                Arrays.asList(
                        io.sphere.sdk.orders.commands.updateactions.AddPayment.of(payment)
                )));

        HttpResponse response = sendGetRequestToUrl(getHandlePaymentUrl(payment.getId()));

        //retry processing of payment to assure that authorization was done
        while (response.getStatusLine().getStatusCode() != 200) {
            Thread.sleep(200);
            response = sendGetRequestToUrl(getHandlePaymentUrl(payment.getId()));
        }
        return payment;
    }

    private Payment fetchPayment(final String paymentId) {
        return ctpClient.complete(PaymentByIdGet.of(paymentId));
    }
}
