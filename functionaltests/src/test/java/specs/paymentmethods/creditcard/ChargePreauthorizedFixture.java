package specs.paymentmethods.creditcard;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.CartDraft;
import io.sphere.sdk.carts.CartDraftBuilder;
import io.sphere.sdk.carts.LineItemDraft;
import io.sphere.sdk.carts.commands.CartCreateCommand;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.AddLineItem;
import io.sphere.sdk.carts.commands.updateactions.AddPayment;
import io.sphere.sdk.carts.commands.updateactions.SetBillingAddress;
import io.sphere.sdk.carts.commands.updateactions.SetShippingAddress;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.orders.OrderFromCartDraft;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.orders.commands.OrderFromCartCreateCommand;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.concordion.api.ExpectedToFail;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;
import specs.BaseFixture;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * @author fhaertig
 * @since 10.12.15
 */
@RunWith(ConcordionRunner.class)
@ExpectedToFail
public class ChargePreauthorizedFixture extends BaseFixture {

    public String createPayment(
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode) throws Exception {

        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);
        final String paymentId = preparePaymentWithPreauthorizedAmountAndOrder(monetaryAmount, paymentMethod);

        //get newest payment and add new charge transaction
        ctpClient().executeBlocking(PaymentUpdateCommand.of(fetchPaymentById(paymentId),
                        ImmutableList.of(
                                AddTransaction.of(TransactionDraftBuilder
                                        .of(TransactionType.valueOf(transactionType), monetaryAmount, ZonedDateTime.now())
                                        .state(TransactionState.PENDING)
                                        .build())
                        )
                )
        );

        return paymentId;
    }

    public boolean handlePayment(final String paymentId) throws IOException, ExecutionException, InterruptedException {

        final HttpResponse response = Request.Get(getHandlePaymentUrl(paymentId))
                .connectTimeout(REQUEST_TIMEOUT)
                .execute()
                .returnResponse();

        return response.getStatusLine().getStatusCode() == 200;
    }

    public String getInterfaceInteractionCount(
            final String paymentId,
            final String transactionId,
            final String interactionTypeName,
            final String requestType) throws ExecutionException {
        final String interactionTypeId = typeIdFromTypeName(interactionTypeName);
        final Payment payment = fetchPaymentById(paymentId);
        return Long.toString(payment.getInterfaceInteractions().stream()
                .filter(i -> i.getType().getId().equals(interactionTypeId))
                .filter(i -> transactionId.equals(i.getFieldAsString(CustomFieldKeys.TRANSACTION_ID_FIELD)))
                .filter(i -> {
                    final String requestField = i.getFieldAsString(CustomFieldKeys.REQUEST_FIELD);
                    return requestField != null && requestField.contains("request=" + requestType);
                })
                .count());
    }

     private String preparePaymentWithPreauthorizedAmountAndOrder(final MonetaryAmount monetaryAmount, final String paymentMethod) throws Exception {
         final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                 .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                         .method(paymentMethod)
                         .paymentInterface("PAYONE")
                         .build())
                 .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                         CustomTypeBuilder.PAYMENT_CREDIT_CARD,
                         ImmutableMap.of(
                                 CustomFieldKeys.CARD_DATA_PLACEHOLDER_FIELD, getUnconfirmedVisaPseudoCardPan(),
                                 CustomFieldKeys.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage(),
                                 CustomFieldKeys.REFERENCE_FIELD, "<placeholder>")))
                 .build();

         final BlockingSphereClient ctpClient = ctpClient();
         final Payment payment = ctpClient.executeBlocking(PaymentCreateCommand.of(paymentDraft));
         //create cart and order with product
         final Product product = ctpClient.executeBlocking(ProductQuery.of()).getResults().get(0);
         final CartDraft cardDraft = CartDraftBuilder.of(Monetary.getCurrency("EUR")).build();
         final Cart cart = ctpClient.executeBlocking(CartUpdateCommand.of(
                 ctpClient.executeBlocking(CartCreateCommand.of(cardDraft)),
                 ImmutableList.of(
                         AddPayment.of(payment),
                         AddLineItem.of(LineItemDraft.of(product.getId(), product.getMasterData().getCurrent().getMasterVariant().getId(), 1)),
                         SetShippingAddress.of(Address.of(CountryCode.DE)),
                         SetBillingAddress.of(Address.of(CountryCode.DE).withLastName("Test Buyer"))
                 )));

         ctpClient.executeBlocking(OrderFromCartCreateCommand.of(OrderFromCartDraft.of(cart, getRandomOrderNumber(), PaymentState.PENDING)));

         ctpClient.executeBlocking(PaymentUpdateCommand.of(payment, AddTransaction.of(TransactionDraftBuilder
                 .of(TransactionType.AUTHORIZATION, monetaryAmount, ZonedDateTime.now())
                 .state(TransactionState.PENDING)
                 .build())));

         HttpResponse response = sendGetRequestToUrl(getHandlePaymentUrl(payment.getId()));

         //retry processing of payment to assure that authorization was done
         int i = 0;
         while (response.getStatusLine().getStatusCode() != 200) {
             Thread.sleep(200);
             response = sendGetRequestToUrl(getHandlePaymentUrl(payment.getId()));
             i++;
             if (i > 100) {
                 throw new Exception("Expected the service to answer with \"200 OK\" " +
                         "at least after 100 retries, but received always a different status.");
             }
         }
         return payment.getId();
     }
}
