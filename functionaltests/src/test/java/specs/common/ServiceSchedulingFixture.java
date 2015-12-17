package specs.common;

import com.commercetools.pspadapter.payone.IntegrationService;
import com.commercetools.pspadapter.payone.ServiceFactory;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.CartDraft;
import io.sphere.sdk.carts.CartDraftBuilder;
import io.sphere.sdk.carts.commands.CartCreateCommand;
import io.sphere.sdk.carts.commands.CartDeleteCommand;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.AddLineItem;
import io.sphere.sdk.carts.commands.updateactions.AddPayment;
import io.sphere.sdk.carts.commands.updateactions.SetShippingAddress;
import io.sphere.sdk.carts.queries.CartByIdGet;
import io.sphere.sdk.carts.queries.CartQuery;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.OrderFromCartDraft;
import io.sphere.sdk.orders.commands.OrderDeleteCommand;
import io.sphere.sdk.orders.commands.OrderFromCartCreateCommand;
import io.sphere.sdk.orders.queries.OrderQuery;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentDraft;
import io.sphere.sdk.payments.PaymentDraftBuilder;
import io.sphere.sdk.payments.TransactionDraft;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentDeleteCommand;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.utils.MoneyImpl;
import org.concordion.api.ExpectedToFail;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.quartz.SchedulerException;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.net.MalformedURLException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/**
 * @author fhaertig
 * @date 04.12.15
 */
@ExpectedToFail
@RunWith(ConcordionRunner.class)
public class ServiceSchedulingFixture {

    private static final String SCHEDULED_JOB_KEY = "commercetools-polljob-1";

    private SphereClient client;
    private Payment payment;
    private IntegrationService integrationService;

    @Before
    public void setUp() throws ExecutionException, InterruptedException, SchedulerException, MalformedURLException {
        String ctProjectKey = System.getProperty("CT_PROJECT_KEY");
        String ctClientId = System.getProperty( "CT_CLIENT_ID" );
        String ctClientSecret = System.getProperty( "CT_CLIENT_SECRET" );

        SphereClientFactory factory = SphereClientFactory.of();
        client = factory.createClient(ctProjectKey, ctClientId, ctClientSecret);

        MonetaryAmount monetaryAmount = MoneyImpl.ofCents(20000, "EUR");

        final List<TransactionDraft> transactions = Collections.singletonList(TransactionDraftBuilder
                .of(TransactionType.CHARGE, monetaryAmount, ZonedDateTime.now())
                .timestamp(ZonedDateTime.now())
                .interactionId("1")
                .state(TransactionState.PENDING)
                .build());

        PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .transactions(transactions).build();

        PaymentCreateCommand paymentCreateCommand = PaymentCreateCommand.of(paymentDraft);
        final CompletionStage<Payment> payment = client.execute(paymentCreateCommand);
        this.payment = payment.toCompletableFuture().get();

        //create cart and order with product
        Product product = client.execute(ProductQuery.of()).toCompletableFuture().get().getResults().get(0);
        CartDraft cardDraft = CartDraftBuilder.of(Monetary.getCurrency("EUR"))
                .build();
        Cart cart = client.execute(CartCreateCommand.of(cardDraft)).toCompletableFuture().get();
        List<UpdateAction<Cart>> updateActions = Arrays.asList(
                AddPayment.of(this.payment),
                AddLineItem.of(product.getId(), product.getMasterData().getCurrent().getMasterVariant().getId(), 1),
                SetShippingAddress.of(Address.of(CountryCode.DE))
        );
        client.execute(CartUpdateCommand.of(cart, updateActions)).toCompletableFuture().get();

        cart = client.execute(CartByIdGet.of(cart.getId())).toCompletableFuture().get();

        OrderFromCartDraft orderFromCartDraft = OrderFromCartDraft.of(cart);
        client.execute(OrderFromCartCreateCommand.of(orderFromCartDraft)).toCompletableFuture().get();


        integrationService = ServiceFactory.createService(new ServiceConfig());
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        //delete payment
        client.execute(PaymentDeleteCommand.of(payment));

        //delete carts
        PagedQueryResult<Cart> carts = client.execute(CartQuery.of()).toCompletableFuture().get();
        for (Cart cart : carts.getResults()) {
            client.execute(CartDeleteCommand.of(cart)).toCompletableFuture().get();
        }

        //delete orders
        PagedQueryResult<Order> orders = client.execute(OrderQuery.of()).toCompletableFuture().get();
        for (Order order : orders.getResults()) {
            client.execute(OrderDeleteCommand.of(order)).toCompletableFuture().get();
        }
    }

    public JobResult checkJobScheduling(String cronNotation) throws SchedulerException, InterruptedException {

        //TODO: Check if payment has a new interaction or transaction state changed to validate job run.
        return null;
    }

    class JobResult {
        private String triggerState;
        private String triggerCronExpression;
        private Date jobFirstExecutionTime;

        public JobResult( final String triggerState, final String triggerCronExpression, final Date jobFirstExecutionTime) {
            this.triggerState = triggerState;
            this.triggerCronExpression = triggerCronExpression;
            this.jobFirstExecutionTime = jobFirstExecutionTime;
        }

        public boolean validateCronNotation(final String cronNotation) {
            return this.triggerCronExpression.equals(cronNotation);
        }

        public boolean wasJobExecuted() {
            return this.triggerState.equals("NORMAL")
                    && jobFirstExecutionTime != null
                    && jobFirstExecutionTime.before(new Date());
        }
    }

    class HttpResult {
        public String header;
        public String body;

        public HttpResult(final String header, final String body) {
            this.header = header;
            this.body = body;
        }

        public boolean hasEmptyBody() {
            return this.body.isEmpty();
        }
    }
}
