package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BankTransferAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.payments.Payment;
import org.assertj.core.api.SoftAssertions;
import org.javamoney.moneta.function.MonetaryUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import util.PaymentTestHelper;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author fhaertig
 * @since 22.01.16
 */
@RunWith(MockitoJUnitRunner.class)
public class SofortWithoutIbanRequestFactoryTest {

    private final PaymentTestHelper payments = new PaymentTestHelper();
    private SofortBankTransferRequestFactory factory;

    @Mock
    private PropertyProvider propertyProvider;

    @Test
    public void createFullAuthorizationRequestFromValidPayment() throws Exception {

        when(propertyProvider.getProperty(any())).thenReturn(Optional.of("dummyVal"));
        when(propertyProvider.getMandatoryNonEmptyProperty(any())).thenReturn("dummyVal");
        //clear secure key to force unencrypted data
        when(propertyProvider.getProperty(PropertyProvider.SECURE_KEY)).thenReturn(Optional.of(""));

        PayoneConfig payoneConfig = new PayoneConfig(propertyProvider);
        ServiceConfig serviceConfig = new ServiceConfig(propertyProvider);
        factory = new SofortBankTransferRequestFactory(payoneConfig, serviceConfig);


        Payment payment = payments.dummyPaymentOneAuthPending20EuroWithoutIbanPNT();
        Order order = payments.dummyOrderMapToPayoneRequest();
        Customer customer = payment.getCustomer().getObj();

        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, order);
        BankTransferAuthorizationRequest result = factory.createAuthorizationRequest(paymentWithCartLike);
        SoftAssertions softly = new SoftAssertions();

        //base values
        softly.assertThat(result.getRequest()).isEqualTo(RequestType.AUTHORIZATION.getType());
        softly.assertThat(result.getAid()).isEqualTo(payoneConfig.getSubAccountId());
        softly.assertThat(result.getMid()).isEqualTo(payoneConfig.getMerchantId());
        softly.assertThat(result.getPortalid()).isEqualTo(payoneConfig.getPortalId());
        softly.assertThat(result.getKey()).isEqualTo(payoneConfig.getKeyAsMd5Hash());
        softly.assertThat(result.getMode()).isEqualTo(payoneConfig.getMode());
        softly.assertThat(result.getApiVersion()).isEqualTo(payoneConfig.getApiVersion());
        softly.assertThat(result.getSolutionName()).isEqualTo(payoneConfig.getSolutionName());
        softly.assertThat(result.getSolutionVersion()).isEqualTo(payoneConfig.getSolutionVersion());
        softly.assertThat(result.getIntegratorName()).isEqualTo(payoneConfig.getIntegratorName());
        softly.assertThat(result.getIntegratorVersion()).isEqualTo(payoneConfig.getIntegratorVersion());

        //clearing type
        ClearingType clearingType = ClearingType.getClearingTypeByKey("BANK_TRANSFER-SOFORTUEBERWEISUNG");
        softly.assertThat(result.getOnlinebanktransfertype()).isEqualTo(clearingType.getSubType());
        softly.assertThat(result.getClearingtype()).isEqualTo(clearingType.getPayoneCode());

        softly.assertThat(result.getIban()).isNull();
        softly.assertThat(result.getBic()).isNull();

        //references
        softly.assertThat(result.getReference()).isEqualTo(paymentWithCartLike.getReference());
        softly.assertThat(result.getCustomerid()).isEqualTo(customer.getCustomerNumber());
        // language is skipped in payment object, but set in order object
        softly.assertThat(result.getLanguage()).isEqualTo(order.getLocale().getLanguage());

        //monetary
        softly.assertThat(result.getAmount()).isEqualTo(MonetaryUtil.minorUnits().queryFrom(payment.getAmountPlanned()).intValue());
        softly.assertThat(result.getCurrency()).isEqualTo(payment.getAmountPlanned().getCurrency().getCurrencyCode());

        //urls
        softly.assertThat(result.getSuccessurl()).isEqualTo("www.test.de/success");
        softly.assertThat(result.getErrorurl()).isEqualTo("www.test.de/error");
        softly.assertThat(result.getBackurl()).isEqualTo("www.test.de/cancel");

        //billing address data
        Address billingAddress = order.getBillingAddress();
        softly.assertThat(result.getTitle()).isEqualTo(billingAddress.getTitle());
        softly.assertThat(result.getSalutation()).isEqualTo(billingAddress.getSalutation());
        softly.assertThat(result.getFirstname()).isEqualTo(billingAddress.getFirstName());
        softly.assertThat(result.getLastname()).isEqualTo(billingAddress.getLastName());
        softly.assertThat(result.getBirthday()).isEqualTo(payment.getCustomer().getObj().getDateOfBirth().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        softly.assertThat(result.getCompany()).isEqualTo(billingAddress.getCompany());
        softly.assertThat(result.getStreet()).isEqualTo(billingAddress.getStreetName() + " " + billingAddress.getStreetNumber());
        softly.assertThat(result.getAddressaddition()).isEqualTo(billingAddress.getAdditionalStreetInfo());
        softly.assertThat(result.getCity()).isEqualTo(billingAddress.getCity());
        softly.assertThat(result.getZip()).isEqualTo(order.getBillingAddress().getPostalCode());
        softly.assertThat(result.getCountry()).isEqualTo(order.getBillingAddress().getCountry().toLocale().getCountry());
        softly.assertThat(result.getEmail()).isEqualTo(order.getBillingAddress().getEmail());
        softly.assertThat(result.getTelephonenumber()).isEqualTo(Optional
                .ofNullable(billingAddress.getPhone())
                .orElse(billingAddress.getMobile()));

        //shipping address data
        Address shippingAddress = order.getShippingAddress();
        softly.assertThat(result.getShipping_firstname()).isEqualTo(shippingAddress.getFirstName());
        softly.assertThat(result.getShipping_lastname()).isEqualTo(shippingAddress.getLastName());
        softly.assertThat(result.getShipping_street()).isEqualTo(shippingAddress.getStreetName() + " " + shippingAddress.getStreetNumber());
        softly.assertThat(result.getShipping_city()).isEqualTo(shippingAddress.getCity());
        softly.assertThat(result.getShipping_zip()).isEqualTo(shippingAddress.getPostalCode());
        softly.assertThat(result.getShipping_country()).isEqualTo(shippingAddress.getCountry().toLocale().getCountry());
        softly.assertThat(result.getShipping_state()).isEqualTo(shippingAddress.getState());
        softly.assertThat(result.getShipping_company()).isEqualTo(shippingAddress.getCompany() + " " + shippingAddress.getDepartment());

        softly.assertAll();
    }

    @Test
    public void createFullAuthorizationRequestFromValidPaymentWithEncryptedBankData() throws Exception {

        when(propertyProvider.getProperty(any())).thenReturn(Optional.of("dummyValue"));
        when(propertyProvider.getMandatoryNonEmptyProperty(any())).thenReturn("dummyValue");

        PayoneConfig payoneConfig = new PayoneConfig(propertyProvider);
        ServiceConfig serviceConfig = new ServiceConfig(propertyProvider);
        factory = new SofortBankTransferRequestFactory(payoneConfig, serviceConfig);

        Payment payment = payments.dummyPaymentOneAuthPending20EuroWithoutIbanPNT();
        Order order = payments.dummyOrderMapToPayoneRequest();
        Customer customer = payment.getCustomer().getObj();

        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, order);
        BankTransferAuthorizationRequest result = factory.createAuthorizationRequest(paymentWithCartLike);
        SoftAssertions softly = new SoftAssertions();

        //base values
        softly.assertThat(result.getRequest()).isEqualTo(RequestType.AUTHORIZATION.getType());
        softly.assertThat(result.getAid()).isEqualTo(payoneConfig.getSubAccountId());
        softly.assertThat(result.getMid()).isEqualTo(payoneConfig.getMerchantId());
        softly.assertThat(result.getPortalid()).isEqualTo(payoneConfig.getPortalId());
        softly.assertThat(result.getKey()).isEqualTo(payoneConfig.getKeyAsMd5Hash());
        softly.assertThat(result.getMode()).isEqualTo(payoneConfig.getMode());
        softly.assertThat(result.getApiVersion()).isEqualTo(payoneConfig.getApiVersion());
        softly.assertThat(result.getSolutionName()).isEqualTo(payoneConfig.getSolutionName());
        softly.assertThat(result.getSolutionVersion()).isEqualTo(payoneConfig.getSolutionVersion());
        softly.assertThat(result.getIntegratorName()).isEqualTo(payoneConfig.getIntegratorName());
        softly.assertThat(result.getIntegratorVersion()).isEqualTo(payoneConfig.getIntegratorVersion());

        //clearing type
        ClearingType clearingType = ClearingType.getClearingTypeByKey("BANK_TRANSFER-SOFORTUEBERWEISUNG");
        softly.assertThat(result.getOnlinebanktransfertype()).isEqualTo(clearingType.getSubType());
        softly.assertThat(result.getClearingtype()).isEqualTo(clearingType.getPayoneCode());

        softly.assertThat(result.getIban()).isNull();
        softly.assertThat(result.getBic()).isNull();

        //references
        softly.assertThat(result.getReference()).isEqualTo(paymentWithCartLike.getReference());
        softly.assertThat(result.getCustomerid()).isEqualTo(customer.getCustomerNumber());
        // language is skipped in payment object, but set in order object
        softly.assertThat(result.getLanguage()).isEqualTo(order.getLocale().getLanguage());

        //monetary
        softly.assertThat(result.getAmount()).isEqualTo(MonetaryUtil.minorUnits().queryFrom(payment.getAmountPlanned()).intValue());
        softly.assertThat(result.getCurrency()).isEqualTo(payment.getAmountPlanned().getCurrency().getCurrencyCode());

        //urls
        softly.assertThat(result.getSuccessurl()).isEqualTo("www.test.de/success");
        softly.assertThat(result.getErrorurl()).isEqualTo("www.test.de/error");
        softly.assertThat(result.getBackurl()).isEqualTo("www.test.de/cancel");

        //billing address data
        Address billingAddress = order.getBillingAddress();
        softly.assertThat(result.getTitle()).isEqualTo(billingAddress.getTitle());
        softly.assertThat(result.getSalutation()).isEqualTo(billingAddress.getSalutation());
        softly.assertThat(result.getFirstname()).isEqualTo(billingAddress.getFirstName());
        softly.assertThat(result.getLastname()).isEqualTo(billingAddress.getLastName());
        softly.assertThat(result.getBirthday()).isEqualTo(payment.getCustomer().getObj().getDateOfBirth().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        softly.assertThat(result.getCompany()).isEqualTo(billingAddress.getCompany());
        softly.assertThat(result.getStreet()).isEqualTo(billingAddress.getStreetName() + " " + billingAddress.getStreetNumber());
        softly.assertThat(result.getAddressaddition()).isEqualTo(billingAddress.getAdditionalStreetInfo());
        softly.assertThat(result.getCity()).isEqualTo(billingAddress.getCity());
        softly.assertThat(result.getZip()).isEqualTo(order.getBillingAddress().getPostalCode());
        softly.assertThat(result.getCountry()).isEqualTo(order.getBillingAddress().getCountry().toLocale().getCountry());
        softly.assertThat(result.getEmail()).isEqualTo(order.getBillingAddress().getEmail());
        softly.assertThat(result.getTelephonenumber()).isEqualTo(Optional
                .ofNullable(billingAddress.getPhone())
                .orElse(billingAddress.getMobile()));

        //shipping address data
        Address shippingAddress = order.getShippingAddress();
        softly.assertThat(result.getShipping_firstname()).isEqualTo(shippingAddress.getFirstName());
        softly.assertThat(result.getShipping_lastname()).isEqualTo(shippingAddress.getLastName());
        softly.assertThat(result.getShipping_street()).isEqualTo(shippingAddress.getStreetName() + " " + shippingAddress.getStreetNumber());
        softly.assertThat(result.getShipping_city()).isEqualTo(shippingAddress.getCity());
        softly.assertThat(result.getShipping_zip()).isEqualTo(shippingAddress.getPostalCode());
        softly.assertThat(result.getShipping_country()).isEqualTo(shippingAddress.getCountry().toLocale().getCountry());
        softly.assertThat(result.getShipping_state()).isEqualTo(shippingAddress.getState());
        softly.assertThat(result.getShipping_company()).isEqualTo(shippingAddress.getCompany() + " " + shippingAddress.getDepartment());

        softly.assertAll();
    }

}