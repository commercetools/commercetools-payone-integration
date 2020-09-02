package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.BaseTenantPropertyTest;
import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardAuthorizationRequest;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.types.CustomFields;
import org.assertj.core.api.SoftAssertions;
import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import util.PaymentTestHelper;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.GENDER_FIELD;
import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.LANGUAGE_CODE_FIELD;
import static com.commercetools.pspadapter.payone.mapping.MappingUtil.getPaymentLanguage;
import static com.neovisionaries.i18n.CountryCode.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author fhaertig
 * @since 18.01.16
 */
@RunWith(MockitoJUnitRunner.class)
public class MappingUtilTest extends BaseTenantPropertyTest {

    private SoftAssertions softly;

    @Mock(lenient = true)
    private Customer customer;

    @Mock
    private PaymentWithCartLike paymentWithCartLike;

    @Mock
    private Payment payment;

    @Mock
    private CustomFields customFields;

    @Mock
    private CartLike cartLike;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        final PaymentTestHelper payments = new PaymentTestHelper();
        when((CartLike) paymentWithCartLike.getCartLike()).thenReturn(payments.dummyOrderMapToPayoneRequest());
        softly = new SoftAssertions();
    }

    @Test
    public void testCountryStateMapping() {
        Address addressDE = Address.of(DE)
                .withState("AK");
        Address addressUS = Address.of(CountryCode.US)
                .withState("AK");

        CreditCardAuthorizationRequest authorizationRequestDE =
                new CreditCardAuthorizationRequest(new PayoneConfig(tenantPropertyProvider), "000123",
                        paymentWithCartLike);
        CreditCardAuthorizationRequest authorizationRequestUS =
                new CreditCardAuthorizationRequest(new PayoneConfig(tenantPropertyProvider), "000123",
                        paymentWithCartLike);

        MappingUtil.mapBillingAddressToRequest(authorizationRequestDE, addressDE);
        MappingUtil.mapShippingAddressToRequest(authorizationRequestDE, addressDE);
        MappingUtil.mapBillingAddressToRequest(authorizationRequestUS, addressUS);
        MappingUtil.mapShippingAddressToRequest(authorizationRequestUS, addressUS);

        softly.assertThat(authorizationRequestDE.getState()).as("DE billing address state").isNullOrEmpty();
        softly.assertThat(authorizationRequestDE.getShipping_state()).as("DE shipping address state").isNullOrEmpty();
        softly.assertThat(authorizationRequestUS.getState()).as("US billing address state").isEqualTo("AK");
        softly.assertThat(authorizationRequestUS.getShipping_state()).as("US shipping address state").isEqualTo("AK");

        softly.assertAll();
    }

    @Test
    public void streetJoiningFull() {
        Address addressWithNameNumber = Address.of(DE)
                .withStreetName("Test Street")
                .withStreetNumber("2");

        CreditCardAuthorizationRequest authorizationRequestWithNameNumber =
                new CreditCardAuthorizationRequest(new PayoneConfig(tenantPropertyProvider), "000123",
                        paymentWithCartLike);

        MappingUtil.mapBillingAddressToRequest(authorizationRequestWithNameNumber, addressWithNameNumber);
        MappingUtil.mapShippingAddressToRequest(authorizationRequestWithNameNumber, addressWithNameNumber);

        softly.assertThat(authorizationRequestWithNameNumber.getStreet()).as("billing address state").isEqualTo(addressWithNameNumber.getStreetName() + " " + addressWithNameNumber.getStreetNumber());
        softly.assertThat(authorizationRequestWithNameNumber.getShipping_street()).as("shipping address state").isEqualTo(addressWithNameNumber.getStreetName() + " " + addressWithNameNumber.getStreetNumber());

        softly.assertAll();
    }

    @Test
    public void streetJoiningNoNumber() {
        Address addressNoNumber = Address.of(DE)
                .withStreetName("Test Street");

        CreditCardAuthorizationRequest authorizationRequestNoNumber =
                new CreditCardAuthorizationRequest(new PayoneConfig(tenantPropertyProvider), "000123",
                        paymentWithCartLike);

        MappingUtil.mapBillingAddressToRequest(authorizationRequestNoNumber, addressNoNumber);
        MappingUtil.mapShippingAddressToRequest(authorizationRequestNoNumber, addressNoNumber);

        softly.assertThat(authorizationRequestNoNumber.getStreet()).as("billing address state").isEqualTo(addressNoNumber.getStreetName());
        softly.assertThat(authorizationRequestNoNumber.getShipping_street()).as("shipping address state").isEqualTo(addressNoNumber.getStreetName());

        softly.assertAll();
    }

    @Test
    public void streetJoiningNoName() {
        Address addressNoName = Address.of(DE)
                .withStreetNumber("5");

        CreditCardAuthorizationRequest authorizationRequestNoName =
                new CreditCardAuthorizationRequest(new PayoneConfig(tenantPropertyProvider), "000123",
                        paymentWithCartLike);

        MappingUtil.mapBillingAddressToRequest(authorizationRequestNoName, addressNoName);
        MappingUtil.mapShippingAddressToRequest(authorizationRequestNoName, addressNoName);

        softly.assertThat(authorizationRequestNoName.getStreet()).as("DE billing address state").isEqualTo("5");
        softly.assertThat(authorizationRequestNoName.getShipping_street()).as("DE shipping address state").isEqualTo(
                "5");

        softly.assertAll();
    }

    @Test
    public void customerNumberDefault() {
        when(customer.getCustomerNumber()).thenReturn("01234567890123456789");
        Reference<Customer> customerReference = Reference.of(Customer.referenceTypeId(), customer);

        CreditCardAuthorizationRequest authorizationRequest =
                new CreditCardAuthorizationRequest(new PayoneConfig(tenantPropertyProvider), "000123",
                        paymentWithCartLike);

        MappingUtil.mapCustomerToRequest(authorizationRequest, customerReference);

        assertThat(authorizationRequest.getCustomerid()).isEqualTo("01234567890123456789");
    }

    @Test
    public void customerNumberExceedsLength() {
        when(customer.getCustomerNumber()).thenReturn("01234567890123456789123456789");
        when(customer.getId()).thenReturn("276829bd-6fa3-450f-9e2a-9a8715a9a104");
        Reference<Customer> customerReference = Reference.of(Customer.referenceTypeId(), customer);

        CreditCardAuthorizationRequest authorizationRequest =
                new CreditCardAuthorizationRequest(new PayoneConfig(tenantPropertyProvider), "000123",
                        paymentWithCartLike);

        MappingUtil.mapCustomerToRequest(authorizationRequest, customerReference);

        assertThat(authorizationRequest.getCustomerid()).isEqualTo("276829bd6fa3450f9e2a");
    }

    @Test
    public void getPaymentLanguageTest() {
        when((CartLike) paymentWithCartLike.getCartLike()).thenReturn(mock(CartLike.class));

        // base cases: null arguments
        softly.assertThat(getPaymentLanguage(null).isPresent()).isFalse();
        softly.assertThat(getPaymentLanguage(paymentWithCartLike).isPresent()).isFalse();

        // add properties to payment object one-by-one till valid customFields.getFieldAsString(LANGUAGE_CODE_FIELD)
        when(paymentWithCartLike.getPayment()).thenReturn(payment);
        softly.assertThat(getPaymentLanguage(paymentWithCartLike)).isEmpty();

        when(payment.getCustom()).thenReturn(customFields);
        softly.assertThat(getPaymentLanguage(paymentWithCartLike)).isEmpty();

        when(customFields.getFieldAsString(LANGUAGE_CODE_FIELD)).thenReturn("nl");
        softly.assertThat(getPaymentLanguage(paymentWithCartLike)).hasValue("nl");

        // now set flush payment object to null - fetch language from cartLike
        // set properties one-by-one till locale.getLanguage()
        when(paymentWithCartLike.getPayment()).thenReturn(null);
        when((CartLike) paymentWithCartLike.getCartLike()).thenReturn(cartLike);
        softly.assertThat(getPaymentLanguage(paymentWithCartLike)).isEmpty();

        Locale locale = new Locale("xx");
        when(cartLike.getLocale()).thenReturn(locale);
        softly.assertThat(getPaymentLanguage(paymentWithCartLike)).hasValue("xx");

        // if both payment and cartLike set - payment value should privilege
        when(paymentWithCartLike.getPayment()).thenReturn(payment);
        softly.assertThat(getPaymentLanguage(paymentWithCartLike)).hasValue("nl");

        // but as soon as payment reference doesn't have proper language at the end -
        // cartLike language should be fetched
        when(customFields.getFieldAsString(LANGUAGE_CODE_FIELD)).thenReturn(null);
        Optional<String> paymentLanguage = getPaymentLanguage(paymentWithCartLike);
        softly.assertThat(paymentLanguage).hasValue("xx");

        softly.assertAll();
    }

    @Test
    public void getPaymentLanguageTagOrFallback() throws Exception {
        when((CartLike) paymentWithCartLike.getCartLike()).thenReturn(mock(CartLike.class));
        // base cases: null arguments
        softly.assertThat(MappingUtil.getPaymentLanguageTagOrFallback(null)).isEqualTo("en");
        softly.assertThat(MappingUtil.getPaymentLanguageTagOrFallback(paymentWithCartLike)).isEqualTo("en");

        // add properties to payment object one-by-one till valid customFields.getFieldAsString(LANGUAGE_CODE_FIELD)
        when(paymentWithCartLike.getPayment()).thenReturn(payment);
        softly.assertThat(MappingUtil.getPaymentLanguageTagOrFallback(paymentWithCartLike)).isEqualTo("en");

        when(payment.getCustom()).thenReturn(customFields);
        softly.assertThat(MappingUtil.getPaymentLanguageTagOrFallback(paymentWithCartLike)).isEqualTo("en");

        when(customFields.getFieldAsString(LANGUAGE_CODE_FIELD)).thenReturn("nl");
        softly.assertThat(MappingUtil.getPaymentLanguageTagOrFallback(paymentWithCartLike)).isEqualTo("nl");

        // now set flush payment object to null - fetch language from cartLike
        // set properties one-by-one till locale.getLanguage()
        when(paymentWithCartLike.getPayment()).thenReturn(null);
        when((CartLike) paymentWithCartLike.getCartLike()).thenReturn(cartLike);
        softly.assertThat(MappingUtil.getPaymentLanguageTagOrFallback(paymentWithCartLike)).isEqualTo("en");

        Locale locale = new Locale("xx");
        when(cartLike.getLocale()).thenReturn(locale);
        softly.assertThat(MappingUtil.getPaymentLanguageTagOrFallback(paymentWithCartLike)).isEqualTo("xx");

        // if both payment and cartLike set - payment value should privilege
        when(paymentWithCartLike.getPayment()).thenReturn(payment);
        softly.assertThat(MappingUtil.getPaymentLanguageTagOrFallback(paymentWithCartLike)).isEqualTo("nl");

        // but as soon as payment reference doesn't have proper language at the end -
        // cartLike language should be fetched
        when(customFields.getFieldAsString(LANGUAGE_CODE_FIELD)).thenReturn(null);
        String paymentLanguage = MappingUtil.getPaymentLanguageTagOrFallback(paymentWithCartLike);
        softly.assertThat(paymentLanguage).isEqualTo("xx");

        softly.assertAll();
    }

    @Test
    public void mapAmountPlannedFromPayment_withoutPrice() throws Exception {
        AuthorizationRequest request = new AuthorizationRequestImplTest(new PayoneConfig(tenantPropertyProvider),
                "testReqType", "testClearType");

        when(payment.getAmountPlanned()).thenReturn(null);

        MappingUtil.mapAmountPlannedFromPayment(request, payment);
        assertThat(request.getAmount()).isEqualTo(0);
        assertThat(request.getCurrency()).isNull();
    }

    @Test
    public void mapAmountPlannedFromPayment_withPrice() throws Exception {
        AuthorizationRequest request = new AuthorizationRequestImplTest(new PayoneConfig(tenantPropertyProvider),
                "testReqType", "testClearType");

        when(payment.getAmountPlanned()).thenReturn(Money.of(18, "EUR"));

        MappingUtil.mapAmountPlannedFromPayment(request, payment);
        assertThat(request.getAmount()).isEqualTo(1800);
        assertThat(request.getCurrency()).isEqualTo("EUR");
    }

    @Test
    public void getGenderFromPaymentCart() throws Exception {
        when((CartLike) paymentWithCartLike.getCartLike()).thenReturn(cartLike);
        when(paymentWithCartLike.getPayment()).thenReturn(payment);
        softly.assertThat(MappingUtil.getGenderFromPaymentCart(paymentWithCartLike)).isEmpty();

        CustomFields cartCustomFields = mock(CustomFields.class);
        when(cartCustomFields.getFieldAsString(GENDER_FIELD)).thenReturn("cartGender");
        CustomFields paymentCustomFields = mock(CustomFields.class);
        when(paymentCustomFields.getFieldAsString(GENDER_FIELD)).thenReturn("paymentGender");
        CustomFields customerCustomFields = mock(CustomFields.class);
        when(customerCustomFields.getFieldAsString(GENDER_FIELD)).thenReturn("USER_GENDER");
        cartLike = Mockito.mock(CartLike.class);

        when(cartLike.getCustom()).thenReturn(cartCustomFields);
        when(payment.getCustom()).thenReturn(paymentCustomFields);
        Reference<Customer> of = Reference.of("test-id", customer);
        when(payment.getCustomer()).thenReturn(of);
        when(customer.getCustom()).thenReturn(customerCustomFields);
        when((CartLike) paymentWithCartLike.getCartLike()).thenReturn(cartLike);
        // payment custom field in the result
        softly.assertThat(MappingUtil.getGenderFromPaymentCart(paymentWithCartLike)).hasValue("p");

        // cart custom field in the result
        when(payment.getCustom()).thenReturn(null);
        softly.assertThat(MappingUtil.getGenderFromPaymentCart(paymentWithCartLike)).hasValue("c");

        // customer custom field in the result
        when(cartCustomFields.getFieldAsString(GENDER_FIELD)).thenReturn(null);
        softly.assertThat(MappingUtil.getGenderFromPaymentCart(paymentWithCartLike)).hasValue("u");

        // don't fail is cartLike.getCustom() is not set
        when(cartLike.getCustom()).thenReturn(null);
        softly.assertThat(MappingUtil.getGenderFromPaymentCart(paymentWithCartLike)).hasValue("u");

        // empty if everything is empty
        when(customerCustomFields.getFieldAsString(GENDER_FIELD)).thenReturn(null);
        softly.assertThat(MappingUtil.getGenderFromPaymentCart(paymentWithCartLike)).isEmpty();

        // custom field is set, but customer#getCustom() is null
        Mockito.lenient().when(customerCustomFields.getFieldAsString(GENDER_FIELD)).thenReturn("XXX");
        Mockito.lenient().when(customer.getCustom()).thenReturn(null);
        softly.assertThat(MappingUtil.getGenderFromPaymentCart(paymentWithCartLike)).isEmpty();

        softly.assertAll();
    }

    @Test
    public void dateToBirthdayString_converting() throws Exception {
        assertThat(MappingUtil.dateToBirthdayString(null)).isNull();
        assertThat(MappingUtil.dateToBirthdayString(LocalDate.of(1986, 12, 15))).isEqualTo("19861215");
        assertThat(MappingUtil.dateToBirthdayString(LocalDate.of(1915, 1, 2))).isEqualTo("19150102");
    }

    @Test
    public void getFirstValueFromAddresses() throws Exception {
        assertThat(MappingUtil.getFirstValueFromAddresses(emptyList(), Address::getEmail))
                .isEmpty();

        assertThat(MappingUtil.getFirstValueFromAddresses(singletonList(Address.of(CH)), Address::getCountry))
                .contains(CH);

        assertThat(MappingUtil.getFirstValueFromAddresses(asList(Address.of(DE), Address.of(NL)), Address::getCountry))
                .contains(DE);

        assertThat(MappingUtil.getFirstValueFromAddresses(asList(Address.of(AL).withCompany("xxx"),
                Address.of(NL).withCompany("ggg")), Address::getCompany))
                .contains("xxx");

        assertThat(MappingUtil.getFirstValueFromAddresses(asList(Address.of(AL).withCompany("xxx").withEmail("TTT"),
                Address.of(NL).withCompany("ggg").withEmail("KKK")), Address::getPhone))
                .isEmpty();

        assertThat(MappingUtil.getFirstValueFromAddresses(asList(Address.of(AL).withCompany("xxx").withEmail("TTT"),
                Address.of(NL).withCompany("ggg").withEmail("KKK")), Address::getEmail))
                .contains("TTT");

        assertThat(MappingUtil.getFirstValueFromAddresses(asList(null,
                Address.of(NL).withCompany("ggg").withEmail("KKK")), Address::getEmail))
                .contains("KKK");
    }

    /**
     * Simple implementation for tests.
     */
    private class AuthorizationRequestImplTest extends AuthorizationRequest {
        protected AuthorizationRequestImplTest(PayoneConfig config, String requestType, String clearingtype) {
            super(config, requestType, clearingtype);
        }
    }
}
