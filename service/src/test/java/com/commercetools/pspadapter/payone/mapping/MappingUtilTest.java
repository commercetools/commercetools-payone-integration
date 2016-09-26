package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardAuthorizationRequest;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.types.CustomFields;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Locale;
import java.util.Optional;

import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.LANGUAGE_CODE_FIELD;
import static com.commercetools.pspadapter.payone.mapping.MappingUtil.getPaymentLanguage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author fhaertig
 * @since 18.01.16
 */
@RunWith(MockitoJUnitRunner.class)
public class MappingUtilTest {

    private static final String dummyValue = "123";

    private SoftAssertions softly;

    @Mock
    private PropertyProvider propertyProvider;

    @Mock
    private Customer customer;

    @Mock
    private PaymentWithCartLike paymentWithCartLike;

    @Mock
    private Payment payment;

    @Mock
    private CustomFields customFields;

    @Mock
    private CartLike cardLike;

    @Before
    public void setUp() {
        softly = new SoftAssertions();

        when(propertyProvider.getProperty(anyString())).thenReturn(Optional.of(dummyValue));
        when(propertyProvider.getMandatoryNonEmptyProperty(anyString())).thenReturn(dummyValue);
    }

    @Test
    public void testCountryStateMapping() {
        Address addressDE = Address.of(CountryCode.DE)
                .withState("AK");
        Address addressUS = Address.of(CountryCode.US)
                .withState("AK");

        CreditCardAuthorizationRequest authorizationRequestDE = new CreditCardAuthorizationRequest(new PayoneConfig(propertyProvider), "000123");
        CreditCardAuthorizationRequest authorizationRequestUS = new CreditCardAuthorizationRequest(new PayoneConfig(propertyProvider), "000123");

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
        Address addressWithNameNumber = Address.of(CountryCode.DE)
                .withStreetName("Test Street")
                .withStreetNumber("2");

        CreditCardAuthorizationRequest authorizationRequestWithNameNumber = new CreditCardAuthorizationRequest(new PayoneConfig(propertyProvider), "000123");

        MappingUtil.mapBillingAddressToRequest(authorizationRequestWithNameNumber, addressWithNameNumber);
        MappingUtil.mapShippingAddressToRequest(authorizationRequestWithNameNumber, addressWithNameNumber);

        softly.assertThat(authorizationRequestWithNameNumber.getStreet()).as("billing address state").isEqualTo(addressWithNameNumber.getStreetName() + " " + addressWithNameNumber.getStreetNumber());
        softly.assertThat(authorizationRequestWithNameNumber.getShipping_street()).as("shipping address state").isEqualTo(addressWithNameNumber.getStreetName() + " " + addressWithNameNumber.getStreetNumber());

        softly.assertAll();
    }

    @Test
    public void streetJoiningNoNumber() {
        Address addressNoNumber = Address.of(CountryCode.DE)
                .withStreetName("Test Street");

        CreditCardAuthorizationRequest authorizationRequestNoNumber = new CreditCardAuthorizationRequest(new PayoneConfig(propertyProvider), "000123");

        MappingUtil.mapBillingAddressToRequest(authorizationRequestNoNumber, addressNoNumber);
        MappingUtil.mapShippingAddressToRequest(authorizationRequestNoNumber, addressNoNumber);

        softly.assertThat(authorizationRequestNoNumber.getStreet()).as("billing address state").isEqualTo(addressNoNumber.getStreetName());
        softly.assertThat(authorizationRequestNoNumber.getShipping_street()).as("shipping address state").isEqualTo(addressNoNumber.getStreetName());

        softly.assertAll();
    }

    @Test
    public void streetJoiningNoName() {
        Address addressNoName = Address.of(CountryCode.DE)
                .withStreetNumber("5");

        CreditCardAuthorizationRequest authorizationRequestNoName = new CreditCardAuthorizationRequest(new PayoneConfig(propertyProvider), "000123");

        MappingUtil.mapBillingAddressToRequest(authorizationRequestNoName, addressNoName);
        MappingUtil.mapShippingAddressToRequest(authorizationRequestNoName, addressNoName);

        softly.assertThat(authorizationRequestNoName.getStreet()).as("DE billing address state").isEqualTo("5");
        softly.assertThat(authorizationRequestNoName.getShipping_street()).as("DE shipping address state").isEqualTo("5");

        softly.assertAll();
    }

    @Test
    public void customerNumberDefault() {
        when(customer.getCustomerNumber()).thenReturn("01234567890123456789");
        Reference<Customer> customerReference = Reference.of(Customer.referenceTypeId(), customer);

        CreditCardAuthorizationRequest authorizationRequest = new CreditCardAuthorizationRequest(new PayoneConfig(propertyProvider), "000123");

        MappingUtil.mapCustomerToRequest(authorizationRequest, customerReference);

        assertThat(authorizationRequest.getCustomerid()).isEqualTo("01234567890123456789");
    }

    @Test
    public void customerNumberExceedsLength() {
        when(customer.getCustomerNumber()).thenReturn("01234567890123456789123456789");
        when(customer.getId()).thenReturn("276829bd-6fa3-450f-9e2a-9a8715a9a104");
        Reference<Customer> customerReference = Reference.of(Customer.referenceTypeId(), customer);

        CreditCardAuthorizationRequest authorizationRequest = new CreditCardAuthorizationRequest(new PayoneConfig(propertyProvider), "000123");

        MappingUtil.mapCustomerToRequest(authorizationRequest, customerReference);

        assertThat(authorizationRequest.getCustomerid()).isEqualTo("276829bd6fa3450f9e2a");
    }

    @Test
    public void getPaymentLanguageTest() {

        // base cases: null arguments
        softly.assertThat(getPaymentLanguage(null).isPresent()).isFalse();
        softly.assertThat(getPaymentLanguage(paymentWithCartLike).isPresent()).isFalse();

        // add properties to payment object one-by-one till valid customFields.getFieldAsString(LANGUAGE_CODE_FIELD)
        when(paymentWithCartLike.getPayment()).thenReturn(payment);
        softly.assertThat(getPaymentLanguage(paymentWithCartLike).isPresent()).isFalse();

        when(payment.getCustom()).thenReturn(customFields);
        softly.assertThat(getPaymentLanguage(paymentWithCartLike).isPresent()).isFalse();

        when(customFields.getFieldAsString(LANGUAGE_CODE_FIELD)).thenReturn("nl");
        softly.assertThat(getPaymentLanguage(paymentWithCartLike).get()).isEqualTo("nl");

        // now set flush payment object to null - fetch language from cartLike
        // set properties one-by-one till locale.getLanguage()
        when(paymentWithCartLike.getPayment()).thenReturn(null);
        when((CartLike)paymentWithCartLike.getCartLike()).thenReturn(cardLike);
        softly.assertThat(getPaymentLanguage(paymentWithCartLike).isPresent()).isFalse();

        Locale locale = new Locale("xx");
        when(cardLike.getLocale()).thenReturn(locale);
        softly.assertThat(getPaymentLanguage(paymentWithCartLike).get()).isEqualTo("xx");

        // if both payment and cartLike set - payment value should privilege
        when(paymentWithCartLike.getPayment()).thenReturn(payment);
        softly.assertThat(getPaymentLanguage(paymentWithCartLike).get()).isEqualTo("nl");

        // but as soon as payment reference doesn't have proper language at the end -
        // cartLike language should be fetched
        when(customFields.getFieldAsString(LANGUAGE_CODE_FIELD)).thenReturn(null);
        Optional<String> paymentLanguage = getPaymentLanguage(paymentWithCartLike);
        softly.assertThat(paymentLanguage.get()).isEqualTo("xx");

        softly.assertAll();
    }
}
