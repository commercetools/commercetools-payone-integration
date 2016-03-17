package com.commercetools.pspadapter.payone.mapping;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BankTransferAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.payments.Payment;
import org.assertj.core.api.SoftAssertions;
import org.javamoney.moneta.function.MonetaryUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import util.PaymentTestHelper;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * @author fhaertig
 * @since 22.01.16
 */
@RunWith(MockitoJUnitRunner.class)
public class BankTransferRequestFactoryTest {

    private final PaymentTestHelper payments = new PaymentTestHelper();
    private BankTransferRequestFactory factory;

    @Mock
    private PropertyProvider propertyProvider;

    private PayoneConfig config;

    @Before
    public void setUp() {
        when(propertyProvider.getProperty(any())).thenReturn(Optional.of("dummyValue"));
        when(propertyProvider.getMandatoryNonEmptyProperty(any())).thenReturn("dummyValue");

        config = new PayoneConfig(propertyProvider);
        factory = new BankTransferRequestFactory(config);
    }

    @Test
    public void createFullAuthorizationRequestFromValidPayment() throws Exception {

        Payment payment = payments.dummyPaymentOneAuthPending20EuroPNT();
        Order order = payments.dummyOrderMapToPayoneRequest();

        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, order);
        BankTransferAuthorizationRequest result = factory.createAuthorizationRequest(paymentWithCartLike);
        SoftAssertions softly = new SoftAssertions();

        //base values
        softly.assertThat(result.getRequest()).isEqualTo(RequestType.AUTHORIZATION.getType());
        softly.assertThat(result.getAid()).isEqualTo(config.getSubAccountId());
        softly.assertThat(result.getMid()).isEqualTo(config.getMerchantId());
        softly.assertThat(result.getPortalid()).isEqualTo(config.getPortalId());
        softly.assertThat(result.getKey()).isEqualTo(config.getKeyAsMd5Hash());
        softly.assertThat(result.getMode()).isEqualTo(config.getMode());
        softly.assertThat(result.getApiVersion()).isEqualTo(config.getApiVersion());
        softly.assertThat(result.getSolutionName()).isEqualTo(config.getSolutionName());
        softly.assertThat(result.getSolutionVersion()).isEqualTo(config.getSolutionVersion());
        softly.assertThat(result.getIntegratorName()).isEqualTo(config.getIntegratorName());
        softly.assertThat(result.getIntegratorVersion()).isEqualTo(config.getIntegratorVersion());

        //clearing type
        ClearingType clearingType = ClearingType.getClearingTypeByKey("BANK_TRANSFER-SOFORTUEBERWEISUNG");
        softly.assertThat(result.getOnlinebanktransfertype()).isEqualTo(clearingType.getSubType());
        softly.assertThat(result.getClearingtype()).isEqualTo(clearingType.getPayoneCode());

        softly.assertThat(result.getIban()).isEqualTo(payment.getCustom().getFieldAsString(CustomFieldKeys.IBAN_FIELD));
        softly.assertThat(result.getBic()).isEqualTo(payment.getCustom().getFieldAsString(CustomFieldKeys.BIC_FIELD));

        //references
        softly.assertThat(result.getReference()).isEqualTo(paymentWithCartLike.getReference());
        softly.assertThat(result.getCustomerid()).isEqualTo(payment.getCustomer().getObj().getCustomerNumber());

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