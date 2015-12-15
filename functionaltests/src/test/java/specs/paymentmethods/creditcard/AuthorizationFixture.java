package specs.paymentmethods.creditcard;

import com.commercetools.pspadapter.payone.IntegrationService;
import com.commercetools.pspadapter.payone.ServiceConfig;
import com.commercetools.pspadapter.payone.ServiceFactory;
import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.CartDraft;
import io.sphere.sdk.carts.CartDraftBuilder;
import io.sphere.sdk.carts.commands.CartCreateCommand;
import io.sphere.sdk.carts.commands.CartDeleteCommand;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.AddPayment;
import io.sphere.sdk.carts.queries.CartQuery;
import io.sphere.sdk.client.SphereClientFactory;
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
import io.sphere.sdk.payments.queries.PaymentByIdGet;
import io.sphere.sdk.payments.queries.PaymentQuery;
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
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * @author fhaertig
 * @date 10.12.15
 */
@ExpectedToFail
@RunWith(ConcordionRunner.class)
public class AuthorizationFixture extends BaseFixture {

    private BlockingClient ctpClient;
    private IntegrationService integrationService;

    @Before
    public void initializeService() throws MalformedURLException {

        final ServiceConfig serviceConfig = new ServiceConfig(new URL(getPayOneApiUrl()));

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

    public String createPayment(
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode) throws ExecutionException, InterruptedException {

        final MonetaryAmount monetaryAmount = MoneyImpl.of(centAmount, currencyCode);

        final List<TransactionDraft> transactions = Collections.singletonList(TransactionDraftBuilder
                .of(TransactionType.valueOf(transactionType), monetaryAmount, ZonedDateTime.now())
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
                                CustomTypeBuilder.CARD_DATA_PLACEHOLDER_FIELD, getUnconfirmedVisaPseudoCardPan())))
                .build();

        final Payment payment = ctpClient.complete(PaymentCreateCommand.of(paymentDraft));

        final CartDraft cardDraft = CartDraftBuilder.of(Monetary.getCurrency("EUR")).build();
        final Cart cart = ctpClient.complete(CartCreateCommand.of(cardDraft));
        ctpClient.complete(CartUpdateCommand.of(cart, AddPayment.of(payment)));

        return payment.getId();
    }

    public boolean handlePayment(final String paymentId) throws IOException, ExecutionException, InterruptedException {

        final HttpResponse response = Request.Get(getHandlePaymentUrl(paymentId))
                .connectTimeout(200)
                .execute()
                .returnResponse();

        return response.getStatusLine().getStatusCode() == 200;
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

        // delete all payments
        ctpClient.complete(PaymentQuery.of().withLimit(500)).getResults()
                .forEach(p -> ctpClient.complete(PaymentDeleteCommand.of(p)));

        ctpClient.complete(TypeQuery.of().withLimit(500)).getResults()
                .forEach(t -> ctpClient.complete(TypeDeleteCommand.of(t)));
    }

    private Payment fetchPayment(final String paymentId) {
        return ctpClient.complete(PaymentByIdGet.of(paymentId));
    }
}
