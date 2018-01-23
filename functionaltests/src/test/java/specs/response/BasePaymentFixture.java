package specs.response;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.commercetools.service.OrderService;
import com.commercetools.service.OrderServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.CartDraft;
import io.sphere.sdk.carts.CartDraftBuilder;
import io.sphere.sdk.carts.CartDraftDsl;
import io.sphere.sdk.carts.commands.CartCreateCommand;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.AddDiscountCode;
import io.sphere.sdk.carts.commands.updateactions.AddPayment;
import io.sphere.sdk.carts.queries.CartByIdGet;
import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.queries.CustomerQueryBuilder;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.OrderFromCartDraft;
import io.sphere.sdk.orders.commands.OrderFromCartCreateCommand;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.SetCustomField;
import io.sphere.sdk.types.CustomFieldsDraft;
import model.HandlePaymentResult;
import org.apache.http.HttpResponse;
import org.junit.Before;
import specs.BaseFixture;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.MethodKeys.*;
import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.*;
import static com.commercetools.pspadapter.payone.util.CompletionUtil.executeBlocking;
import static io.sphere.sdk.payments.TransactionState.FAILURE;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;

/**
 * Base class to create and handle test payments for Payone.
 */
public class BasePaymentFixture extends BaseFixture {

    public static final String DISCOUNT_10_PERCENT = "test-10-percent-code";
    public static final String DISCOUNT_999_CENT = "test-999-cent-code";

    public static final String KLARNA_DEFAULT_CART_MOCK = "mocks/paymentmethods/klarna/KlarnaCartWithTestAccountAddress.json";

    protected OrderService orderService;

    @Before
    public void setUp() throws Exception {
        orderService = new OrderServiceImpl(ctpClient());
    }

    /**
     * Creates credit card payment (PAYMENT_CREDIT_CARD) and saves it to commercetools service. Also created payment is
     * saved in {@code #payments} map and may be reused in further sequental tests.
     * @param paymentName Unique payment name to use inside this test fixture
     * @param paymentMethod see {@link com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod}
     * @param transactionType  see {@link TransactionType}
     * @param centAmount amount to process (charge, authorize, refund etc)
     * @param currencyCode 3 letters currency code. The tested product should have this currency type, otherwise the
     *                     payment won't be created.
     * @param languageCode 2 letters ISO 639 code
     * @return String id of created payment.
     */
    public Payment createAndSaveCardPayment(String paymentName,
                                              String paymentMethod,
                                              String transactionType,
                                              String centAmount,
                                              String currencyCode,
                                              String languageCode) throws Exception {
        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);
        final String pseudocardpan = getUnconfirmedVisaPseudoCardPan();

        final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                        .method(paymentMethod)
                        .paymentInterface("PAYONE")
                        .build())
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYMENT_CREDIT_CARD,
                        ImmutableMap.<String, Object>builder()
                                .put(CustomFieldKeys.CARD_DATA_PLACEHOLDER_FIELD, pseudocardpan)
                                .put(CustomFieldKeys.LANGUAGE_CODE_FIELD, languageCode)
                                .put(REFERENCE_FIELD, "<placeholder>")
                                .build()))
                .build();

        return createPaymentCartOrderFromDraft(paymentName, paymentDraft, transactionType);
    }

    public Payment createAndSaveCardPayment(String paymentName,
                                               String paymentMethod,
                                               String transactionType,
                                               String centAmount,
                                               String currencyCode) throws Exception {
        return createAndSaveCardPayment(paymentName, paymentMethod, transactionType, centAmount, currencyCode, Locale.ENGLISH.getLanguage());
    }

    public Payment createAndSavePayment(String paymentName,
                                           String paymentMethod,
                                           String transactionType,
                                           String centAmount,
                                           String currencyCode) throws Exception {
        switch (paymentMethod) {
            case CREDIT_CARD: return createAndSaveCardPayment(paymentName, paymentMethod, transactionType, centAmount, currencyCode);
            case WALLET_PAYPAL: return createAndSavePaypalPayment(paymentName, paymentMethod, transactionType, centAmount, currencyCode);
            case BANK_TRANSFER_ADVANCE: return createAndSaveBankTransferAdvancedPayment(paymentName, paymentMethod, transactionType, centAmount, currencyCode);
            default: throw new IllegalArgumentException(format("Payment method [%s] is not implemented", paymentMethod));
        }
    }

    public Payment createAndSavePaypalPayment(
            final String paymentName,
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode) throws Exception {

        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);

        final String redirectUrl = "https://example.com/paypal_authorization/";

        final String successUrl = redirectUrl + URLEncoder.encode(paymentName + " Success", "UTF-8");
        final String errorUrl = redirectUrl + URLEncoder.encode(paymentName + " Error", "UTF-8");
        final String cancelUrl = redirectUrl + URLEncoder.encode(paymentName + " Cancel", "UTF-8");

        final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                        .method(paymentMethod)
                        .paymentInterface("PAYONE")
                        .build())
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYMENT_WALLET,
                        ImmutableMap.<String, Object>builder()
                                .put(CustomFieldKeys.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage())
                                .put(CustomFieldKeys.SUCCESS_URL_FIELD, successUrl)
                                .put(CustomFieldKeys.ERROR_URL_FIELD, errorUrl)
                                .put(CustomFieldKeys.CANCEL_URL_FIELD, cancelUrl)
                                .put(REFERENCE_FIELD, "<placeholder>")
                                .build()))
                .build();

        return createPaymentCartOrderFromDraft(paymentName, paymentDraft, transactionType);
    }

    public Payment createAndSaveBankTransferAdvancedPayment(
            final String paymentName,
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode) throws Exception {
        return createAndSaveBankTransferAdvancedPayment(paymentName, paymentMethod, transactionType, centAmount, currencyCode, BUYER_LAST_NAME);
    }

    public Payment createAndSaveBankTransferAdvancedPayment(
            final String paymentName,
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode,
            final String buyerLastName) throws Exception {

        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);

        final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                        .method(paymentMethod)
                        .paymentInterface("PAYONE")
                        .build())
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYMENT_CASH_ADVANCE,
                        ImmutableMap.<String, Object>builder()
                                .put(CustomFieldKeys.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage())
                                .put(REFERENCE_FIELD, "<placeholder>")
                                .build()))
                .build();

        return createPaymentCartOrderFromDraft(paymentName, paymentDraft, transactionType, buyerLastName);
    }

    /**
     * Create Klarna payment and attach to the {@code cart}.
     * The request is created form cart JSON mock + CTP user with email youremail@email.com
     * See <a href="https://developers.klarna.com/en/de/kpm/test-credentials">Klarna Test credentials</a>.
     *
     * So far the language ("de") and gender ("m") are hardcoded.
     * @return new payment reference with respective {@code transactionType}, and attached to cart.
     */
    public Payment createAndSaveKlarnaPayment(
            final Cart cart,
            final Order order,
            final String paymentName,
            final String transactionType,
            final String centAmount,
            final String currencyCode,
            final String ip,
            final String birthDay,
            final String telephonenumber) {
        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);

        // customer is required to fetch mandatory Klarna fields, like date of birth
        Customer customer = ctpClient().executeBlocking(CustomerQueryBuilder.of()
                .predicates(customerModel -> customerModel.lowercaseEmail().is("youremail@email.com"))
                .build())
                .head().orElseThrow(() -> new IllegalStateException("Customer youremail@email.com for klarna test payment has not found"));

        final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                        .method(PaymentMethod.INVOICE_KLARNA.getKey())
                        .paymentInterface("PAYONE")
                        .build())
                .transactions(singletonList(TransactionDraftBuilder.of(
                        TransactionType.valueOf(transactionType),
                        monetaryAmount,
                        ZonedDateTime.now())
                        .state(TransactionState.PENDING)
                        .build()))
                .customer(customer)
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYMENT_INVOICE_KLARNA,
                        ImmutableMap.<String, Object>builder()
                                .put(LANGUAGE_CODE_FIELD,
                                        ofNullable(cart.getLocale())
                                                .map(Locale::getLanguage)
                                                .orElseGet(Locale.GERMAN::getLanguage))
                                .put(REFERENCE_FIELD, ofNullable(order.getOrderNumber())
                                        .orElseThrow(() -> new IllegalStateException("Order must have a number")))
                                .put(GENDER_FIELD, "m")
                                .put(IP_FIELD, ip)
                                .put(BIRTHDAY_FIELD, birthDay)
                                .put(TELEPHONENUMBER_FIELD, telephonenumber)
                                .build()))
                .build();

        final Payment payment = ctpClient().executeBlocking(PaymentCreateCommand.of(paymentDraft));

        registerPaymentWithLegibleName(paymentName, payment);

        // set payment to the Cart#paymentInfo
        executeBlocking(ctpClient().execute(CartByIdGet.of(cart))
                .thenCompose(newCart -> ctpClient().execute(CartUpdateCommand.of(newCart, AddPayment.of(payment)))));

        return payment;

    }

    /**
     * Creates and saves payment from supplied draft. The method may be reused for different payment methods and types
     * as soon as they are already set in {@code paymentDraft}.
     * @param paymentName Unique payment name to use inside this test fixture
     * @param paymentDraft {@link PaymentDraft} from which to create and save payment
     * @param transactionType see {@link TransactionType}
     * @return Instance of created and saved payment object
     */
    protected Payment createPaymentCartOrderFromDraft(String paymentName, PaymentDraft paymentDraft, String transactionType, String buyerLastName) {
        final Payment payment = ctpClient().executeBlocking(PaymentCreateCommand.of(paymentDraft));

        registerPaymentWithLegibleName(paymentName, payment);

        final String orderNumber = createCartAndOrderForPayment(payment, paymentDraft.getAmountPlanned().getCurrency().getCurrencyCode(), buyerLastName);

        ctpClient().executeBlocking(PaymentUpdateCommand.of(
                payment,
                ImmutableList.<UpdateActionImpl<Payment>>builder()
                        .add(AddTransaction.of(TransactionDraftBuilder.of(
                                TransactionType.valueOf(transactionType),
                                paymentDraft.getAmountPlanned(),
                                ZonedDateTime.now())
                                .state(TransactionState.PENDING)
                                .build()))
                        .add(SetCustomField.ofObject(REFERENCE_FIELD, orderNumber))
                        .build()));

        return payment;
    }

    protected Payment createPaymentCartOrderFromDraft(String paymentName, PaymentDraft paymentDraft, String transactionType) {
        return createPaymentCartOrderFromDraft(paymentName, paymentDraft, transactionType, BUYER_LAST_NAME);
    }

    /**
     * Create a cart ready for Klarna payment. This cart expected to have multiple line items,
     * shipping/billing address is respective to Klarna test data requirements.
     * @param lastName "Approved" and "Denied" are 2 standard Klarna test names, see https://developers.klarna.com/en/de/kpm/test-credentials
     */
    public Cart createTemplateCartKlarna(String lastName) {
        CartDraft cartDraft = SphereJsonUtils.readObjectFromResource(KLARNA_DEFAULT_CART_MOCK, CartDraft.class);

        Address billingAddress = cartDraft.getBillingAddress();
        billingAddress = billingAddress != null ? billingAddress.withLastName(lastName) : null;
        CartDraftDsl cartDraftDsl = CartDraftBuilder.of(cartDraft)
                .billingAddress(billingAddress)
                .build();

        return ctpClient().executeBlocking(CartCreateCommand.of(cartDraftDsl));
    }

    public Order createOrderForCart(Cart cart) {
        String orderNumber = getRandomOrderNumber();
        return ctpClient().executeBlocking(OrderFromCartCreateCommand.of(
                OrderFromCartDraft.of(cart, orderNumber, null)));
    }

    /**
     * Apply {@code discountCodes} to the cart. The discounts are applied in the order they appear in the list.
     * @param cart cart to update
     * @param discountCodes  codes to apply to the {@code cart}
     * @return the same cart, but with updated prices.
     */
    public Cart applyDiscounts(Cart cart, String ...discountCodes) {
        return ctpClient().executeBlocking(CartUpdateCommand.of(cart,
                Arrays.stream(discountCodes)
                        .map(AddDiscountCode::of)
                        .collect(Collectors.toList())));
    }


    /**
     * Handle response by given name and parse "response" result from JSON string to {@link JsonNode} result.
     * @param paymentName previously created payment name from HTML template
     * @return JsonNode with response key-values if exists, or text node with error message otherwise
     */
    protected JsonNode handleJsonResponse(final String paymentName) throws ExecutionException, IOException {
        return handlePaymentByName(paymentName)
                .getPayment()
                .getInterfaceInteractions()
                .stream()
                .map(customFields -> customFields.getFieldAsString("response"))
                .filter(Objects::nonNull)
                .findFirst()
                .map(SphereJsonUtils::parse)
                .orElse(new TextNode("ERROR in payment transaction result: response JSON node not found"));
    }

    /**
     * Check all the payments from {@code paymentNamesList} have non-FAILURE status. If at least one is failed -
     * {@link IllegalStateException} is thrown.
     * @param paymentNamesList names of payments to check
     * @throws IllegalStateException if some payments are failed.
     */
    protected void validatePaymentsNotFailed(final List<String> paymentNamesList) throws IllegalStateException {
        // since fetchPaymentByLegibleName() is a slow blocking operation - make the stream parallel
        paymentNamesList.parallelStream().forEach(paymentName -> {
            Payment payment = fetchPaymentByLegibleName(paymentName);
            List<Transaction> transactions = payment.getTransactions();
            TransactionState lastTransactionState = transactions.size() > 0 ? transactions.get(transactions.size() - 1).getState() : null;
            if (FAILURE.equals(lastTransactionState)) {
                throw new IllegalStateException(format("Payment [%s] transaction is FAILURE, payment status is [%s]",
                        payment.getId(), payment.getPaymentStatus().getInterfaceCode()));
            }
        });
    }

    /**
     * Process payment on Payone service.
     * @param paymentName Previously used unique payment name
     * @return {@link HandlePaymentResult} with HTTP handle response and processed payment object
     */
    protected HandlePaymentResult handlePaymentByName(final String paymentName) throws ExecutionException, IOException {
        HttpResponse httpResponse = requestToHandlePaymentByLegibleName(paymentName);
        Payment payment = fetchPaymentByLegibleName(paymentName);
        return new HandlePaymentResult(httpResponse, payment);
    }

}
