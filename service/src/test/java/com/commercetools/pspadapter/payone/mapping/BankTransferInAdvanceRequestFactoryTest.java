package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.BaseTenantPropertyTest;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.commercetools.pspadapter.payone.domain.payone.model.paymentinadvance.BankTransferInAdvancePreautorizationRequest;
import com.commercetools.pspadapter.tenant.TenantPropertyProvider;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.payments.Payment;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import util.PaymentTestHelper;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.javamoney.moneta.function.MonetaryQueries.convertMinorPart;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author mht@dotsource.de
 */
@RunWith(MockitoJUnitRunner.class)
public class BankTransferInAdvanceRequestFactoryTest extends BaseTenantPropertyTest {

    private final PaymentTestHelper payments = new PaymentTestHelper();
    private BanktTransferInAdvanceRequestFactory factory;

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void createFullAuthorizationRequestFromValidPayment() throws Exception {
        when(propertyProvider.getProperty(any())).thenReturn(Optional.of("dummyVal"));
        when(propertyProvider.getMandatoryNonEmptyProperty(any())).thenReturn("dummyVal");

        when(tenantPropertyProvider.getCommonPropertyProvider()).thenReturn(propertyProvider);
        when(tenantPropertyProvider.getTenantProperty(any())).thenReturn(Optional.of("dummyVal"));
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(any())).thenReturn("dummyVal");
        //clear secure key to force unencrypted data
        when(tenantPropertyProvider.getTenantProperty(TenantPropertyProvider.SECURE_KEY)).thenReturn(Optional.of(""));

        factory = new BanktTransferInAdvanceRequestFactory(tenantConfig);

        Payment payment = payments.dummyPaymentOneAuthPending20EuroVOR();
        Order order = payments.dummyOrderMapToPayoneRequest();

        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, order);
        BankTransferInAdvancePreautorizationRequest result = factory.createPreauthorizationRequest(paymentWithCartLike);
        SoftAssertions softly = new SoftAssertions();

        //base values
        softly.assertThat(result.getRequest()).isEqualTo(RequestType.PREAUTHORIZATION.getType());
        softly.assertThat(result.getAid()).isEqualTo(payoneConfig.getSubAccountId());
        softly.assertThat(result.getMid()).isEqualTo(payoneConfig.getMerchantId());
        softly.assertThat(result.getPortalid()).isEqualTo(payoneConfig.getPortalId());
        softly.assertThat(result.getKey()).isEqualTo(payoneConfig.getKeyAsSha384Hash());
        softly.assertThat(result.getMode()).isEqualTo(payoneConfig.getMode());
        softly.assertThat(result.getApiVersion()).isEqualTo(payoneConfig.getApiVersion());
        softly.assertThat(result.getEncoding()).isEqualTo(payoneConfig.getEncoding());
        softly.assertThat(result.getSolutionName()).isEqualTo(payoneConfig.getSolutionName());
        softly.assertThat(result.getSolutionVersion()).isEqualTo(payoneConfig.getSolutionVersion());
        softly.assertThat(result.getIntegratorName()).isEqualTo(payoneConfig.getIntegratorName());
        softly.assertThat(result.getIntegratorVersion()).isEqualTo(payoneConfig.getIntegratorVersion());

        //clearing type
        ClearingType clearingType = ClearingType.getClearingTypeByKey("BANK_TRANSFER-ADVANCE");

        softly.assertThat(result.getClearingtype()).isEqualTo(clearingType.getPayoneCode());


        //references
        softly.assertThat(result.getReference()).isEqualTo(paymentWithCartLike.getReference());
        softly.assertThat(result.getCustomerid()).isEqualTo(payment.getCustomer().getObj().getCustomerNumber());

        //monetary
        softly.assertThat(result.getAmount()).isEqualTo(convertMinorPart().queryFrom(payment.getAmountPlanned()).intValue());
        softly.assertThat(result.getCurrency()).isEqualTo(payment.getAmountPlanned().getCurrency().getCurrencyCode());

        //urls
        //no redirect
        softly.assertThat(result.getSuccessurl()).isNull();
        softly.assertThat(result.getErrorurl()).isNull();
        softly.assertThat(result.getBackurl()).isNull();

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