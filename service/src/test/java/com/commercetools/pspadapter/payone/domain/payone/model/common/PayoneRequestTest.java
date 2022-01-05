package com.commercetools.pspadapter.payone.domain.payone.model.common;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.util.ClearSecuredValuesSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.commercetools.pspadapter.payone.config.PropertyProvider.HIDE_CUSTOMER_PERSONAL_DATA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PayoneRequestTest {
    private static final String requestType = "some-request";
    private static final String clearingType = "some-clearing";
    private static final String merchantId = "merchant X";
    private static final String portalId = "portal 23";
    private static final String keyHash = "hashed key";
    private static final String mode = "unit test";
    private static final String apiVersion = "v.1.2.3";
    private static final String reference = "someReference";
    private static final int amount = 123;
    private static final String currency = "EUR";
    private static final String lastName = "customerLastName";
    private static final String country = "customerCountry";
    private static final String param = "someParam";
    private static final String narrativeText = "someNarrativeText";
    private static final String customerId = "customerId";
    private static final String userId = "userId";
    private static final String salutation = "Mr";
    private static final String title = "title";
    private static final String firstName = "customerFirstName";
    private static final String company = "customerCompany";
    private static final String street = "customerStreet";
    private static final String zip = "customerZip";
    private static final String city = "customerCity";
    private static final String addressAddition = "customerAddressAddition";
    private static final String state = "customerState";
    private static final String email = "customerEmail";
    private static final String telephoneNumber = "customerPhone";
    private static final String birthday = "customerBirthday";
    private static final String language = "customerLanguage";
    private static final String vatId = "customerVatId";
    private static final String gender = "customerGender";
    private static final String personalId = "personalId";
    private static final String ip = "customerIp";
    private static final String shippingFirstName = "shippingFirstName";
    private static final String shippingLastName = "shippingLastname";
    private static final String shippingCompany = "shippingCompany";
    private static final String shippingStreet = "shippingStreet";
    private static final String shippingZip = "shippingZip";
    private static final String shippingCity = "shippingCity";
    private static final String shippingState = "shippingState";
    private static final String shippingCountry = "shippingCountry";
    private static final String successUrl = "successUrl";
    private static final String errorUrl = "errorUrl";
    private static final String backUrl = "backUrl";

    @Mock
    private PayoneConfig payoneConfig;

    private PayoneRequest request;

    @Before
    public void setUp() {
        when(payoneConfig.getMerchantId()).thenReturn(merchantId);
        when(payoneConfig.getPortalId()).thenReturn(portalId);
        when(payoneConfig.getKeyAsHash()).thenReturn(keyHash);
        when(payoneConfig.getMode()).thenReturn(mode);
        when(payoneConfig.getApiVersion()).thenReturn(apiVersion);
        request = new PayoneRequest(payoneConfig, requestType, clearingType);
        request.setAddressaddition(addressAddition);
        request.setAmount(amount);
        request.setBackurl(backUrl);
        request.setBirthday(birthday);
        request.setCity(city);
        request.setCompany(company);
        request.setCountry(country);
        request.setCurrency(currency);
        request.setCustomerid(customerId);
        request.setEmail(email);
        request.setErrorurl(errorUrl);
        request.setFirstname(firstName);
        request.setGender(gender);
        request.setIp(ip);
        request.setLanguage(language);
        request.setLastname(lastName);
        request.setNarrative_text(narrativeText);
        request.setParam(param);
        request.setPersonalid(personalId);
        request.setReference(reference);
        request.setSalutation(salutation);
        request.setShipping_city(shippingCity);
        request.setShipping_country(shippingCountry);
        request.setShipping_company(shippingCompany);
        request.setShipping_firstname(shippingFirstName);
        request.setShipping_lastname(shippingLastName);
        request.setShipping_state(shippingState);
        request.setShipping_street(shippingStreet);
        request.setShipping_zip(shippingZip);
        request.setState(state);
        request.setStreet(street);
        request.setSuccessurl(successUrl);
        request.setTelephonenumber(telephoneNumber);
        request.setTitle(title);
        request.setUserid(userId);
        request.setVatid(vatId);
        request.setZip(zip);
    }

    @After
    public void tearDown() {
        System.clearProperty(HIDE_CUSTOMER_PERSONAL_DATA);
    }

    @Test
    public void createsFullMap() {
        assertThat(request.toStringMap(false)).containsOnly(
                entry("narrative_text", narrativeText),
                entry("param", param),
                entry("key", keyHash),
                entry("clearingtype", clearingType),
                entry("errorurl", errorUrl),
                entry("vatid", vatId),
                entry("request", requestType),
                entry("backurl", backUrl),
                entry("successurl", successUrl),
                entry("mode", mode),
                entry("amount", amount),
                entry("reference", reference),
                entry("currency", currency),
                entry("api_version", apiVersion),
                entry("portalid", portalId),
                entry("mid", merchantId),
                entry("userid", userId),
                entry("customerid", customerId),
                entry("personalid", personalId),
                entry("language", language),
                entry("birthday", birthday),
                entry("gender", gender),
                entry("salutation", salutation),
                entry("title", title),
                entry("lastname", lastName),
                entry("firstname", firstName),
                entry("country", country),
                entry("state", state),
                entry("city", city),
                entry("street", street),
                entry("company", company),
                entry("addressaddition", addressAddition),
                entry("zip", zip),
                entry("telephonenumber", telephoneNumber),
                entry("ip", ip),
                entry("shipping_firstname", shippingFirstName),
                entry("shipping_lastname", shippingLastName),
                entry("shipping_country", shippingCountry),
                entry("shipping_state", shippingState),
                entry("shipping_city", shippingCity),
                entry("shipping_street", shippingStreet),
                entry("shipping_company", shippingCompany),
                entry("shipping_zip", shippingZip),
                entry("email", email));

    }

    @Test
    public void createsMapWithHiddenSecrets() {
        assertThat(request.toStringMap(true)).containsOnly(
                entry("narrative_text", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("param", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("key", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("clearingtype", clearingType),
                entry("errorurl", errorUrl),
                entry("vatid", vatId),
                entry("request", requestType),
                entry("backurl", backUrl),
                entry("successurl", successUrl),
                entry("mode", mode),
                entry("amount", amount),
                entry("reference", reference),
                entry("currency", currency),
                entry("api_version", apiVersion),
                entry("portalid", portalId),
                entry("mid", merchantId),
                entry("userid", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("customerid", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("personalid", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("language", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("birthday", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("gender", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("salutation", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("title", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("lastname", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("firstname", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("country", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("state", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("city", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("street", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("company", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("zip", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("telephonenumber", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("ip", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("addressaddition", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("shipping_firstname", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("shipping_lastname", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("shipping_country", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("shipping_state", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("shipping_city", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("shipping_street", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("shipping_company", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("shipping_zip", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("email", ClearSecuredValuesSerializer.PLACEHOLDER));
    }

    @Test
    public void createsMapWithHiddenSecretsWithoutHidingPersonalData() {
        System.setProperty(HIDE_CUSTOMER_PERSONAL_DATA, "false");

        assertThat(request.toStringMap(true)).containsOnly(
                entry("narrative_text", narrativeText),
                entry("param", param),
                entry("key", ClearSecuredValuesSerializer.PLACEHOLDER),
                entry("clearingtype", clearingType),
                entry("errorurl", errorUrl),
                entry("vatid", vatId),
                entry("request", requestType),
                entry("backurl", backUrl),
                entry("successurl", successUrl),
                entry("mode", mode),
                entry("amount", amount),
                entry("reference", reference),
                entry("currency", currency),
                entry("api_version", apiVersion),
                entry("portalid", portalId),
                entry("mid", merchantId),
                entry("userid", userId),
                entry("customerid", customerId),
                entry("personalid", personalId),
                entry("language", language),
                entry("birthday", birthday),
                entry("gender", gender),
                entry("salutation", salutation),
                entry("title", title),
                entry("lastname", lastName),
                entry("firstname", firstName),
                entry("country", country),
                entry("state", state),
                entry("city", city),
                entry("street", street),
                entry("company", company),
                entry("addressaddition", addressAddition),
                entry("zip", zip),
                entry("telephonenumber", telephoneNumber),
                entry("ip", ip),
                entry("shipping_firstname", shippingFirstName),
                entry("shipping_lastname", shippingLastName),
                entry("shipping_country", shippingCountry),
                entry("shipping_state", shippingState),
                entry("shipping_city", shippingCity),
                entry("shipping_street", shippingStreet),
                entry("shipping_company", shippingCompany),
                entry("shipping_zip", shippingZip),
                entry("email", email));
    }
}
