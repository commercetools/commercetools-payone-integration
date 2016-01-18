package com.commercetools.pspadapter.payone.mapping;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardAuthorizationRequest;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.models.Address;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

/**
 * @author fhaertig
 * @since 18.01.16
 */
@RunWith(MockitoJUnitRunner.class)
public class MappingUtilTest {

    private static final String dummyValue = "123";

    @Mock
    private PropertyProvider propertyProvider;

    @Before
    public void setUp() {
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
        SoftAssertions softly = new SoftAssertions();

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
        SoftAssertions softly = new SoftAssertions();

        MappingUtil.mapBillingAddressToRequest(authorizationRequestWithNameNumber, addressWithNameNumber);
        MappingUtil.mapShippingAddressToRequest(authorizationRequestWithNameNumber, addressWithNameNumber);

        softly.assertThat(authorizationRequestWithNameNumber.getStreet()).isEqualTo(addressWithNameNumber.getStreetName() + " " + addressWithNameNumber.getStreetNumber());
        softly.assertThat(authorizationRequestWithNameNumber.getShipping_street()).isEqualTo(addressWithNameNumber.getStreetName() + " " + addressWithNameNumber.getStreetNumber());

        softly.assertAll();
    }

    @Test
    public void streetJoiningNoNumber() {
        Address addressNoNumber = Address.of(CountryCode.DE)
                .withStreetName("Test Street");

        CreditCardAuthorizationRequest authorizationRequestNoNumber = new CreditCardAuthorizationRequest(new PayoneConfig(propertyProvider), "000123");
        SoftAssertions softly = new SoftAssertions();

        MappingUtil.mapBillingAddressToRequest(authorizationRequestNoNumber, addressNoNumber);
        MappingUtil.mapShippingAddressToRequest(authorizationRequestNoNumber, addressNoNumber);

        softly.assertThat(authorizationRequestNoNumber.getStreet()).isEqualTo(addressNoNumber.getStreetName());
        softly.assertThat(authorizationRequestNoNumber.getShipping_street()).isEqualTo(addressNoNumber.getStreetName());

        softly.assertAll();
    }

    @Test
    public void streetJoiningNoName() {
        Address addressNoName = Address.of(CountryCode.DE)
                .withStreetNumber("5");

        CreditCardAuthorizationRequest authorizationRequestNoName = new CreditCardAuthorizationRequest(new PayoneConfig(propertyProvider), "000123");
        SoftAssertions softly = new SoftAssertions();

        MappingUtil.mapBillingAddressToRequest(authorizationRequestNoName, addressNoName);
        MappingUtil.mapShippingAddressToRequest(authorizationRequestNoName, addressNoName);

        softly.assertThat(authorizationRequestNoName.getStreet()).isEmpty();
        softly.assertThat(authorizationRequestNoName.getShipping_street()).isEmpty();
    }
}